
package supergame.appstate;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.AbstractAppState;
import com.jme3.app.state.AppStateManager;
import com.jme3.bullet.BulletAppState;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;

import supergame.Config;
import supergame.terrain.Chunk;
import supergame.terrain.ChunkBakerThread;
import supergame.terrain.ChunkIndex;
import supergame.terrain.ChunkProcessor;
import supergame.terrain.ChunkProvider;
import supergame.terrain.modify.ChunkModifier;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 *
 */
public class ChunkAppState extends AbstractAppState {
    private AppStateManager mStateManager;
    private final HashMap<ChunkIndex, Chunk> mChunks = new HashMap<ChunkIndex, Chunk>();
    private final LinkedBlockingQueue<Chunk> mDirtyChunks = new LinkedBlockingQueue<Chunk>();
    private final LinkedHashSet<ChunkIndex> mChunkCache = new LinkedHashSet<ChunkIndex>();
    private long mLastX = 0;
    private long mLastY = 0;
    private long mLastZ = 0;

    private final Node mChunkRoot = new Node("chunk_root");
    private Material mChunkMaterial = null;

    private final ChunkProvider mChunkProvider = new ChunkProvider() {
        @Override
        public Chunk getChunkToProcess() throws InterruptedException {
            /*if (mDirtyChunks.size() == 1 && startTime != 0) {
                double timeTaken = ((Sys.getTime() - startTime) * 1.0) / Sys.getTimerResolution();
                System.out.println("\t\t\tCompleted in " + timeTaken);
                startTime = 0;
            }*/
            return mDirtyChunks.poll(1, TimeUnit.SECONDS);
        }
    };

    private final ChunkProcessor mChunkProcessor = new ChunkProcessor() {
        @Override
        public void processChunks(ArrayList<Chunk> chunksForProcessing) {
            mDirtyChunks.addAll(chunksForProcessing);
        }

        @Override
        public void swapChunks(ArrayList<Chunk> chunksForSwapping) {
            for (Chunk newChunk : chunksForSwapping) {
                ChunkIndex i = newChunk.getChunkIndex();
                Chunk oldChunk = mChunks.remove(i);

                if (oldChunk == null) {
                    System.err.println("swapped out nonexistant chunk...");
                    System.exit(1);
                }

                if (!oldChunk.processingIsComplete()) {
                    oldChunk.cancelParallelProcessing();
                }
                detachChunkGeometry(oldChunk.serial_clean());
                mChunks.put(i, newChunk);
            }
        }

        @Override
        public Chunk getChunk(ChunkIndex i) {
            return mChunks.get(i);
        }
    };

    @Override
    public void initialize(AppStateManager stateManager, Application app) {
        mStateManager = stateManager;
        ((SimpleApplication)app).getRootNode().attachChild(mChunkRoot);

        mChunkMaterial = new Material(app.getAssetManager(),
                "Common/MatDefs/Light/Lighting.j3md");
        mChunkMaterial.setBoolean("UseMaterialColors", true);
        mChunkMaterial.setColor("Ambient", ColorRGBA.Brown);
        mChunkMaterial.setColor("Diffuse", ColorRGBA.Brown);

        for (int i = 0; i < Config.WORKER_THREADS; i++) {
            new ChunkBakerThread(i, mChunkProvider).start();
        }

        sweepNearby(0, 0, 0, 2, false);
        sweepNearby(0, 0, 0, Config.CHUNK_LOAD_DISTANCE, false);
        System.out.println("chunks to make are " + mChunks.size() + ", " + mDirtyChunks.size());
    }

    @Override
    public void update(float tpf) {
        System.out.println("chunks to make are " + mChunks.size() + ", " + mDirtyChunks.size());
        // TODO: use current character position
        processPosition(0, 0, 0);

        // process modified chunks, swap them into place as needed
        ChunkModifier.step(mChunkProcessor);

        // create geometry as needed from chunks finished processing
        for (Chunk c : mChunks.values()) {
            attachChunkGeometry(c.serial_getNewGeometry());
        }
    }

    private void attachChunkGeometry(Geometry g) {
        if (g == null) return;

        System.out.println("attaching chunk geom");
        g.setMaterial(mChunkMaterial);
        mChunkRoot.attachChild(g);
        mStateManager.getState(BulletAppState.class).getPhysicsSpace().add(g);
    }

    private void detachChunkGeometry(Geometry g) {
        if (g == null) return;

        g.removeFromParent();
        mStateManager.getState(BulletAppState.class).getPhysicsSpace().remove(g);
    }
    private void sweepNearby(long x, long y, long z, int limit, boolean stall) {
        for (int i = -limit; i <= limit; i++) {
            for (int j = -limit; j <= limit; j++) {
                for (int k = -limit; k <= limit; k++) {
                    if (stall) {
                        // Stall until chunk processed
                        Chunk localChunk = mChunks.get(new ChunkIndex(x + i, y + j, z + k));
                        while (!localChunk.processingIsComplete()) {}
                        attachChunkGeometry(localChunk.serial_getNewGeometry());
                    } else {
                        // prioritize the chunk
                        prioritizeChunk(x + i, y + j, z + k);
                    }
                }
            }
        }
    }

    /**
     * Create a chunk at the parameter coordinates if it doesn't exist.
     */
    private void prioritizeChunk(long x, long y, long z) {
        ChunkIndex key = new ChunkIndex(x, y, z);
        // System.out.println("Prioritizing chunk " + key.getVec3());
        if (mChunks.containsKey(key)) {
            // remove from LRU
            mChunkCache.remove(key);
        } else {
            Chunk c = new Chunk(key);
            mDirtyChunks.add(c);
            mChunks.put(key, c);
        }
    }

    /**
     * Delete the chunk at the parameter coordinates, as it's no longer needed.
     * Eventually, this will just cache the chunk and only delete a chunk if the
     * cache is full.
     */
    private void deprioritizeChunk(long x, long y, long z) {
        ChunkIndex key = new ChunkIndex(x, y, z);
        // System.out.println("DEPrioritizing chunk "+key.getVec3()+"chunkCache size is "+chunkCache.size());
        // insert to LRU (shouldn't be present)

        Chunk c = mChunks.get(key);
        if (c != null) {
            if (!c.processingIsComplete()) {
                c.cancelParallelProcessing();
            }
            Geometry g = c.serial_clean();
            if (g != null) {
                g.removeFromParent();
                mStateManager.getState(BulletAppState.class).getPhysicsSpace().remove(g);
            }
            mChunks.remove(key);
        }

        // if (chunkCache.contains(key))
        // System.err.println("WARNING: non-local chunk removed from local pool");
        // chunkCache.add(key);
    }

    public void updateWithPosition(long x, long y, long z) {
    }

    private void processPosition(long x, long y, long z) {
        // NOTE: assumes constant mLoadDistance
        long dx = x - mLastX, dy = y - mLastY, dz = z - mLastZ;

        if (dx == 0 && dy == 0 && dz == 0)
            return;

        // prioritize chunks now within range, deprioritize those out of range
        // moving chunks in one dimension means a 2d slice of chunks no longer
        // in range.
        int distance = Config.CHUNK_LOAD_DISTANCE;
        for (int i = -distance; i <= distance; i++)
            for (int j = -distance; j <= distance; j++) {
                if (dx == 1) {
                    prioritizeChunk(x + distance, y + i, z + j);
                    deprioritizeChunk(x - distance - 1, y + i, z + j);
                } else if (dx == -1) {
                    prioritizeChunk(x - distance, y + i, z + j);
                    deprioritizeChunk(x + distance + 1, y + i, z + j);
                }

                if (dy == 1) {
                    prioritizeChunk(x + i, y + distance, z + j);
                    deprioritizeChunk(x + i, y - distance - 1, z + j);
                } else if (dy == -1) {
                    prioritizeChunk(x + i, y - distance, z + j);
                    deprioritizeChunk(x + i, y + distance + 1, z + j);
                }

                if (dz == 1) {
                    prioritizeChunk(x + i, y + j, z + distance);
                    deprioritizeChunk(x + i, y + j, z - distance - 1);
                } else if (dz == -1) {
                    prioritizeChunk(x + i, y + j, z - distance);
                    deprioritizeChunk(x + i, y + j, z + distance + 1);
                }
            }

        mLastX = x;
        mLastY = y;
        mLastZ = z;

        sweepNearby(x, y, z, 1, true);

        System.out.println("NOW " + mChunks.size() + " CHUNKS EXIST.");
    }
}
