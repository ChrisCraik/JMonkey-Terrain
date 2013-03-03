
package supergame.terrain;

public interface ChunkProvider {
    public Chunk getChunkToProcess() throws InterruptedException;
}
