
package supergame.terrain;

import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.bullet.util.CollisionShapeFactory;
import com.jme3.material.Material;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.VertexBuffer.Type;
import com.jme3.util.BufferUtils;

import supergame.Config;
import supergame.PhysicsContent.PhysicsRegistrar;
import supergame.network.Structs.ChunkMessage;
import supergame.terrain.modify.ChunkModifierInterface;
import supergame.utils.MarchingCubes;

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

    public static final float colors[][][] = { { { 0, 1, 0, 1 }, { 1, 0, 0, 1 } },
           { { 1, 0.5f, 0, 1 }, { 0.5f, 0, 1, 1 } }, { { 0.9f, 0.9f, 0.9f, 1 }, { 0.4f, 0.4f, 0.4f, 1 } } };

    private static final int SERIAL_INITIAL = 0; // Chunk allocated, not yet processed
    private static final int PARALLEL_PROCESSING = 1; // being processed by worker thread
    private static final int PARALLEL_COMPLETE = 2; // Processing complete, render-able
    private static final int PARALLEL_GARBAGE = 3; // Processing interrupted by chunk garbage collection

    private boolean mIsEmpty = true;

    private final ChunkIndex mIndex;
    private Vector3f mPosition; // cube's origin (not center)

    private float mModifiedWeights[][][] = null;
    HashMap<Integer, Vector3f> mModifyNormals = null; // TODO: turn this into a workers buffer array
    private Chunk mModifiedParent = null;
    private final ChunkModifierInterface mModifyComplete;

    private final AtomicInteger mState = new AtomicInteger(SERIAL_INITIAL);

    // physics engine needs ByteBuffers, so we don't use others for simplicity
    private ByteBuffer mChunkShortIndices, mChunkIntIndices, mChunkVertices, mChunkNormals;

    // Serial Methods - called by main loop

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
        }

        mState.set(PARALLEL_COMPLETE);
    }

    private Geometry mGeometry = null;

    public void serial_attachGeometry(Material mat, Node parent, PhysicsRegistrar registrar) {
        // if the chunk doesn't have content, it's empty or not done
        // if geometry has a material, it's already been attached
        if (mIsEmpty || mGeometry.getMaterial() != null)
            return;

        mGeometry.setMaterial(mat);
        parent.attachChild(mGeometry);

        registrar.registerPhysics(mGeometry);
        return;
    }

    public void serial_clean() {
        if (!mIsEmpty) {
            mChunkVertices = null;
            mChunkNormals = null;
            mChunkIntIndices = null;
            mChunkShortIndices = null;
            if (mGeometry != null) {
                mGeometry.removeFromParent();
            }
        }

        mPosition = null;
    }

    // Parallel Methods - called by worker threads

//per worker temporary buffer data for chunk processing
    private static class WorkerBuffers {
        public float[][][] weights = new float[Config.CHUNK_DIVISION + 2][Config.CHUNK_DIVISION + 2][Config.CHUNK_DIVISION + 2];
        public float[] vertices = new float[9 * Config.CHUNK_DIVISION * Config.CHUNK_DIVISION * Config.CHUNK_DIVISION];
        public int verticesFloatCount;
        public int[] indices = new int[15 * Config.CHUNK_DIVISION * Config.CHUNK_DIVISION * Config.CHUNK_DIVISION];
        public int indicesIntCount;
        public int[][][][] vertIndexVolume = new int[Config.CHUNK_DIVISION + 2][Config.CHUNK_DIVISION + 2][Config.CHUNK_DIVISION + 2][3];
    }

    public static Object parallel_workerBuffersInit() {
        return new WorkerBuffers();
    }

    private boolean parallel_processCalcWeights(WorkerBuffers buffers) {
        // initialize weights from modifiedParent, if parent was modified
        boolean skipGeneration = false;
        if (mModifiedParent != null) {
            float oldWeights[][][] = mModifiedParent.getModifiedWeights();
            if (oldWeights != null) {
                buffers.weights = oldWeights;
                skipGeneration = true;
            }
        }

        int posCount = 0, negCount = 0;
        for (int x = 0; x < Config.CHUNK_DIVISION + 1; x++)
            for (int y = 0; y < Config.CHUNK_DIVISION + 1; y++)
                for (int z = 0; z < Config.CHUNK_DIVISION + 1; z++) {
                    Vector3f localPos = new Vector3f(mPosition.getX() + x, mPosition.getY() + y, mPosition.getZ() + z);

                    if (!skipGeneration) {
                        // haven't already initialized the weights from parent,
                        // so need to call TerrainGenerator
                        buffers.weights[x][y][z] = TerrainGenerator.getDensity(localPos);
                    }

                    if (mModifyComplete != null)
                        buffers.weights[x][y][z] = mModifyComplete.getModification(
                                localPos,
                                buffers.weights[x][y][z]);

                    if (buffers.weights[x][y][z] < 0)
                        negCount++;
                    else
                        posCount++;
                }
        return (negCount != 0) && (posCount != 0); // return false if empty
    }

    private boolean parallel_processCalcGeometry(WorkerBuffers buffers) {
        int x, y, z;
        buffers.indicesIntCount = 0;
        buffers.verticesFloatCount = 0;
        if (true) {
            Vector3f blockPos = new Vector3f(0, 0, 0);
            for (x = 0; x < Config.CHUNK_DIVISION + 1; x++)
                for (y = 0; y < Config.CHUNK_DIVISION + 1; y++)
                    for (z = 0; z < Config.CHUNK_DIVISION + 1; z++)
                        if (MarchingCubes.cubeOccupied(x, y, z, buffers.weights)) {
                            //calculate vertices, populate vert buffer, vertIndexVolume buffer (NOTE some of these vertices wasted: reside in neighbor chunks)
                            blockPos.set(mPosition.getX() + x, mPosition.getY() + y, mPosition.getZ() + z);
                            buffers.verticesFloatCount = MarchingCubes.writeLocalVertices(blockPos, x, y, z,
                                    buffers.weights, buffers.vertices, buffers.verticesFloatCount,
                                    buffers.vertIndexVolume);
                        }
            for (x = 0; x < Config.CHUNK_DIVISION; x++)
                for (y = 0; y < Config.CHUNK_DIVISION; y++)
                    for (z = 0; z < Config.CHUNK_DIVISION; z++)
                        if (MarchingCubes.cubeOccupied(x, y, z, buffers.weights)) {
                            //calculate indices
                            buffers.indicesIntCount = MarchingCubes.writeLocalIndices(x, y, z, buffers.weights,
                                    buffers.indices, buffers.indicesIntCount, buffers.vertIndexVolume);
                        }
        }

        if (buffers.verticesFloatCount == 0 || buffers.indicesIntCount == 0)
            return false;

        if (mModifyComplete != null) {
            mModifyNormals = new HashMap<Integer, Vector3f>();
            // manually calculate normals, because we have been modified since
            // generation
            for (int i = 0; i < buffers.indicesIntCount; i += 3) {
                Vector3f vectors[] = new Vector3f[3];
                for (int j = 0; j < 3; j++) {
                    vectors[j] = new Vector3f(
                            buffers.vertices[buffers.indices[i + j] + 0],
                            buffers.vertices[buffers.indices[i + j] + 1],
                            buffers.vertices[buffers.indices[i + j] + 2]);
                }

                Vector3f a = vectors[0].subtract(vectors[1]);
                Vector3f b = vectors[0].subtract(vectors[2]);

                // Note: don't normalize so that bigger polys have more weight
                Vector3f normal = a.cross(b); // .normalize();

                for (int j = 0; j < 3; j++) {
                    int index = buffers.indices[i + j];
                    Vector3f normalSumForIndex = mModifyNormals.get(index);
                    if (normalSumForIndex == null) {
                        // COPY first normal into hash (since the value in hash will
                        // be modified)
                        mModifyNormals.put(index, new Vector3f(normal));
                    } else {
                        // add into normal (since they're all normalized, we
                        // can normalize the sum later to get the average)
                        normalSumForIndex.addLocal(normal);
                    }
                }
            }
        }

        return true;
    }

    private boolean parallel_processSaveGeometry(WorkerBuffers buffers) {
        mChunkVertices = BufferUtils.createByteBuffer(buffers.verticesFloatCount * 4);
        mChunkNormals = BufferUtils.createByteBuffer(buffers.verticesFloatCount * 4);
        mChunkShortIndices = BufferUtils.createByteBuffer(buffers.indicesIntCount * 2);
        mChunkIntIndices = BufferUtils.createByteBuffer(buffers.indicesIntCount * 4);

        if (true) {
            Vector3f normal = new Vector3f(0, 0, 0);
            for (int i = 0; i < buffers.verticesFloatCount; i += 3) {
                float vx = buffers.vertices[i + 0];
                float vy = buffers.vertices[i + 1];
                float vz = buffers.vertices[i + 2];

                mChunkVertices.putFloat(vx);
                mChunkVertices.putFloat(vy);
                mChunkVertices.putFloat(vz);

                if (mModifyComplete != null && mModifyNormals.containsKey(i)) {
                    normal = mModifyNormals.get(i).normalize();
                } else {
                    TerrainGenerator.getNormal(vx, vy, vz, normal);
                }

                mChunkNormals.putFloat(normal.getX());
                mChunkNormals.putFloat(normal.getY());
                mChunkNormals.putFloat(normal.getZ());
            }
            for (int i = 0; i < buffers.indicesIntCount; i++) {
                mChunkShortIndices.putShort((short) (buffers.indices[i] / 3));
                mChunkIntIndices.putInt(buffers.indices[i] / 3);
            }
        }

        mChunkVertices.flip();
        mChunkNormals.flip();
        mChunkShortIndices.flip();
        mChunkIntIndices.flip();
        return false;
    }

    private void parallel_createGeometryAndPhysics() {
        String chunkName = "Chunk" + mIndex.toString();
        Mesh m = new Mesh();
        m.setBuffer(Type.Index, 1, mChunkIntIndices.asIntBuffer());
        m.setBuffer(Type.Position, 3, mChunkVertices.asFloatBuffer());
        m.setBuffer(Type.Normal, 3, mChunkNormals.asFloatBuffer());
        m.updateBound();

        mGeometry = new Geometry(chunkName, m);

        CollisionShape shape = CollisionShapeFactory.createMeshShape(mGeometry);
        RigidBodyControl control = new RigidBodyControl(shape, 0);
        mGeometry.addControl(control);
    }

    private void parallel_processSetEmpty(WorkerBuffers buffers) {
        if (!parallel_processCalcWeights(buffers)) {
            // if weights all negative or positive, no geometry
            return;
        }

        if (!parallel_processCalcGeometry(buffers)) {
            // no geometry generated (probably due to rounding issues)
            return;
        }

        // save polys in bytebuffers for rendering/physics
        parallel_processSaveGeometry(buffers);

        // create Geometry and physics objects
        parallel_createGeometryAndPhysics();
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
    if (triangles.size() > 0) {
        System.out.println("-- START Small Chunk Splat---");
        for (int i = 0; i < triangles.size(); i++) {
            System.out.printf("ORIG %f,%f,%f    NEW %f,%f,%f\n", triangles.get(i).getX(), triangles.get(i).getY(),
                    triangles.get(i).getZ(), buffers.vertices[buffers.indices[i] + 0],
                    buffers.vertices[buffers.indices[i] + 1], buffers.vertices[buffers.indices[i] + 2]);
        }
        System.out.println("-- END   Small Chunk Splat---");
    }
    */
    ////

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
        float ret[][][] = this.mModifiedWeights;
        this.mModifiedWeights = null;
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
