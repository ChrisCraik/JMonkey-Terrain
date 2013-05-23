/*
 * Thread that aids in rendering volume data to polygons
 */

package supergame.terrain;

import supergame.SuperSimpleApplication;

public class ChunkBakerThread extends Thread {
    @SuppressWarnings("unused")
    private final int mId;
    private final ChunkProvider mChunkProvider;

    private static boolean sRunning = true;
    public static void stopAllThreads() { sRunning = false; }

    public ChunkBakerThread(int id, ChunkProvider chunkProvider) {
        mId = id;
        mChunkProvider = chunkProvider;
        System.out.println("ChunkBakerThread " + id);
    }

    /*
     * Take one dirty chunk and (re-)initialize it so that it can be rendered,
     * and collided
     */
    @Override
    public void run() {
        Chunk current;
        Thread.currentThread().setPriority(Thread.MIN_PRIORITY);
        Object buffers = Chunk.parallel_workerBuffersInit();
        while (sRunning) {
            try {
                current = mChunkProvider.getChunkToProcess();
                if (current != null) {
                    current.parallel_process(buffers);
                }
            } catch (InterruptedException e) {
                System.out.println("InterruptedException ignored");
            } catch (Exception e) {
                System.out.println("ERROR: Worker thread experienced exception");
                e.printStackTrace();
                System.exit(1);
            }
        }
    }
}
