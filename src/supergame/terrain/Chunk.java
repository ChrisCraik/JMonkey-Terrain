
package supergame.terrain;

import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.util.BufferUtils;

import supergame.Config;
import supergame.network.Structs.ChunkMessage;
import supergame.terrain.modify.ChunkModifierInterface;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A chunk is an NxN volumetric surface that is collide-able. It's 'processed'
 * by one of N worker threads in parallel, which generates the necessary
 * geometry for display and physics. parallel_ methods are those called
 * (indirectly) by those processing threads. Serial methods (such as drawing
 * the chunks) are for the UI thread.
 * <p>
 * In order to be modified (for building, and destruction) chunks are replaced
 * with copies that are modified. See the second constructor.
 * <p>
 * First time processing:
 *     1) Weights are calculated
 *     2) Create geometry
 *     3) Create index and vertex lists are created (the format used by OpenGL
 *        & Bullet)
 *     4) Generate point normals (for OpenGL) from TerrainGenerator
 * <p>
 * Then the chunk is ready for rendering and collisions. But there's more:
 * <p>
 * First modification:
 *     1) Weights are calculated from the TerrainGenerator, combined with the
 *        modification
 *     2) (same as above)
 *     3) (same as above)
 *     4) Generate point normals by brute force, by averaging triangle normals
 *     5) finally, the chunk's weights are saved so that later
 *        modifications can use it as a starting point
 * <p>
 * Further modifications:
 *     1) Weights are taken from the previous chunk, and combined with the new
 *        modification
 *     2...5) same as with first modification
 */
public class Chunk {
    //public static Vec3[] rayDistribution;

    private static final int SERIAL_INITIAL = 0; // Chunk allocated, not yet processed
    private static final int PARALLEL_PROCESSING = 1; // being processed by worker thread
    private static final int PARALLEL_COMPLETE = 2; // Processing complete, render-able
    private static final int PARALLEL_GARBAGE = 3; // Processing interrupted by chunk garbage collection

    private boolean mIsEmpty = true;

    private final ChunkIndex mIndex;
    private Vector3f mPosition; // cube's origin (not center)

    private float mModifiedWeights[][][] = null;
    private Chunk mModifiedParent = null;
    private final ChunkModifierInterface mModifyComplete;

    private final AtomicInteger mState = new AtomicInteger(SERIAL_INITIAL);

    // physics engine needs ByteBuffers, so we don't use others for simplicity
    private ByteBuffer mChunkShortIndices, mChunkIntIndices, mChunkVertices, mChunkNormals;

    ///////////////////////////////////////////////////////////////////////////////////////////////
    // Serial Methods - called by main loop
    ///////////////////////////////////////////////////////////////////////////////////////////////

    public Chunk(ChunkIndex index) {
        mIndex = index;
        mModifyComplete = null;
    }

    public Chunk(ChunkIndex index, Chunk other, ChunkModifierInterface cm) {
        mIndex = index;
        mModifyComplete = cm;
        mModifiedParent = other;
    }

    private static ByteBuffer getByteBuffer(byte[] array) {
        ByteBuffer buf = BufferUtils.createByteBuffer(array.length);
        buf.put(array);
        buf.flip();
        return buf;
    }

    private static byte[] getByteArray(ByteBuffer buf) {
        byte[] array = new byte[buf.limit()];
        buf.get(array);
        buf.flip();
        return array;
    }

    public Chunk(ChunkMessage remoteData) {
        mIndex = remoteData.index;
        mModifyComplete = null;

        mPosition = mIndex.getVector3f();
        mPosition = mPosition.mult(Config.CHUNK_DIVISION);

        if (remoteData.vertices != null) {
            mChunkShortIndices = getByteBuffer(remoteData.shortIndices);
            mChunkIntIndices = getByteBuffer(remoteData.intIndices);
            mChunkVertices = getByteBuffer(remoteData.vertices);
            mChunkNormals = getByteBuffer(remoteData.normals);
            mIsEmpty = false;

            // TODO: do this on network thread if expensive -
            // currently will be done on worker thread for server
            String chunkName = "Chunk" + mIndex.toString();
            mGeometry = ChunkUtils.createGeometry(chunkName, mChunkIntIndices, mChunkVertices, mChunkNormals);
        }

        mState.set(PARALLEL_COMPLETE);
    }

    private Geometry mGeometry = null;

    public Geometry serial_getNewGeometry() {
        // if the chunk doesn't have content, it's empty or not done
        // if geometry has a material, it's already been attached
        if (mIsEmpty || mGeometry.getMaterial() != null) return null;

        return mGeometry;
    }

    public Geometry serial_clean() {
        if (!mIsEmpty) {
            mChunkVertices = null;
            mChunkNormals = null;
            mChunkIntIndices = null;
            mChunkShortIndices = null;
        }

        mPosition = null;
        return mGeometry; // null if empty
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////
    // Parallel Methods - called by worker threads
    ///////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Per worker/thread temporary buffer data for chunk processing
     *
     * Instead of using adaptive structures to store geometry, we generate the data in static, maximum size arrays
     * When the worker thread is done it copies the used data into minimally sized, newly allocated byte buffers.
     */
    /*package*/ static class WorkerBuffers {
        public float[][][] weights = new float[Config.CHUNK_DIVISION + 2][Config.CHUNK_DIVISION + 2][Config.CHUNK_DIVISION + 2];
        public float[] vertices = new float[9 * Config.CHUNK_DIVISION * Config.CHUNK_DIVISION * Config.CHUNK_DIVISION];
        public int verticesFloatCount;
        public int[] indices = new int[15 * Config.CHUNK_DIVISION * Config.CHUNK_DIVISION * Config.CHUNK_DIVISION];
        public int indicesIntCount;
        public int[][][][] vertIndexVolume = new int[Config.CHUNK_DIVISION + 2][Config.CHUNK_DIVISION + 2][Config.CHUNK_DIVISION + 2][3];
        public HashMap<Integer, Vector3f> normals = new HashMap<Integer, Vector3f>();
    }

    public static Object parallel_workerBuffersInit() {
        return new WorkerBuffers();
    }

    private void parallel_processSetEmpty(WorkerBuffers buffers) {
        // initialize weights from modifiedParent, if parent was modified
        boolean skipWeightGeneration = false;
        if (mModifiedParent != null) {
            float oldWeights[][][] = mModifiedParent.getModifiedWeights();
            if (oldWeights != null) {
                buffers.weights = oldWeights;
                skipWeightGeneration = true;
            }
        }

        // calculate weights from previous chunk, position, and modification
        if (!ChunkUtils.calculateWeights(buffers.weights, skipWeightGeneration, mPosition,
                mModifyComplete)) {
            return;
        }

        // calculate triangles
        if (!ChunkUtils.calculateTriangles(buffers, mPosition))
            return;

        // if chunk is modified, have to brute force calculate normals
        if (mModifyComplete != null) {
            ChunkUtils.calculateNormals(buffers);
        }

        // save polys in bytebuffers for rendering/physics
        mChunkVertices = BufferUtils.createByteBuffer(buffers.verticesFloatCount * 4);
        mChunkNormals = BufferUtils.createByteBuffer(buffers.verticesFloatCount * 4);
        mChunkShortIndices = BufferUtils.createByteBuffer(buffers.indicesIntCount * 2);
        mChunkIntIndices = BufferUtils.createByteBuffer(buffers.indicesIntCount * 4);
        ChunkUtils.storeTrianglesAndNormals(buffers, mChunkVertices, mChunkNormals,
                mChunkShortIndices, mChunkIntIndices);

        // create Geometry and physics objects
        String chunkName = "Chunk" + mIndex.toString();
        mGeometry = ChunkUtils.createGeometry(chunkName, mChunkIntIndices, mChunkVertices, mChunkNormals);

        mIsEmpty = false; // flag tells main loop that chunk can be used
    }

    public void parallel_process(Object workerBuffers) {
        if (!mState.compareAndSet(SERIAL_INITIAL, PARALLEL_PROCESSING))
            return;

        WorkerBuffers buffers = (WorkerBuffers) workerBuffers;

        mPosition = mIndex.getVector3f();
        mPosition.multLocal(Config.CHUNK_DIVISION);

        buffers.verticesFloatCount = 0;
        buffers.indicesIntCount = 0;
        buffers.normals.clear();

        parallel_processSetEmpty(buffers);

        if (!mState.compareAndSet(PARALLEL_PROCESSING, PARALLEL_COMPLETE)) {
            System.err.println("Error: Chunk parallel processing interrupted");
            System.exit(1);
        }

        if (mModifyComplete == null) {
            // Weights aren't modified, so don't save them since they can be
            // regenerated
            mModifiedWeights = null;
        } else {
            // Because the weights have been modified, they can't be regenerated
            // from the TerrainGenerator, so we save them.
            mModifiedWeights = buffers.weights;
            buffers.weights = new float[Config.CHUNK_DIVISION + 2][Config.CHUNK_DIVISION + 2][Config.CHUNK_DIVISION + 2];

            mModifyComplete.chunkCompletion(this);
        }
    }

    /*
    normals = new ArrayList<Vec3>(triangles.size());
    if (Config.USE_AMBIENT_OCCLUSION)
        occlusion = new ArrayList<Float>(triangles.size());

    for (int i = 0; i < triangles.size(); i++) {
        if (Config.USE_SMOOTH_SHADE || (i % 3 == 0)) {
            Vec3 p = triangles.get(i);
            if (Config.USE_AMBIENT_OCCLUSION) {
                float visibility = 0;
                for (Vec3 ray : rayDistribution) {
                    boolean isOccluded = false;
                    for (int step = 1; step < Config.AMB_OCC_BIGRAY_STEPS && !isOccluded; step++) {

                        Vec3 rp = p.add(ray.multiply(Config.AMB_OCC_BIGRAY_STEP_SIZE * step));
                        if (TerrainGenerator.getDensity(rp.getX(), rp.getY(), rp.getZ()) > 0)
                            isOccluded = true;
                    }
                    if (!isOccluded)
                        visibility += 1.0f / Config.AMB_OCC_RAY_COUNT;
                }
                occlusion.add(Math.min(0.1f, 0.4f * visibility - 0.1f));
            }
        } else {
            if (Config.USE_AMBIENT_OCCLUSION)
                occlusion.add(null);
        }
    }
    */

    // parallel OR serial
    public ChunkIndex getChunkIndex() {
        return mIndex;
    }

    public boolean processingIsComplete() {
        return mState.get() == PARALLEL_COMPLETE;
    }

    public void cancelParallelProcessing() {
        if (mState.compareAndSet(SERIAL_INITIAL, PARALLEL_GARBAGE))
            return;
        while (mState.get() != PARALLEL_COMPLETE) {}
    }

    public float[][][] getModifiedWeights() {
        float ret[][][] = mModifiedWeights;
        mModifiedWeights = null;
        return ret;
    }

    public ChunkMessage getChunkPacket() {
        ChunkMessage m = new ChunkMessage();
        m.index = mIndex;
        if (!mIsEmpty) {
            m.shortIndices = getByteArray(mChunkShortIndices);
            m.intIndices = getByteArray(mChunkIntIndices);
            m.vertices = getByteArray(mChunkVertices);
            m.normals = getByteArray(mChunkNormals);
        }
        return m;
    }
}
