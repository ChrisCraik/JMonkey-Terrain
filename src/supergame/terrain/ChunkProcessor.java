
package supergame.terrain;

import java.util.ArrayList;

public interface ChunkProcessor {
    public void processChunks(ArrayList<Chunk> chunksForProcessing);
    public Chunk getChunk(ChunkIndex i);
    public void swapChunks(ArrayList<Chunk> chunksForSwapping);
}
