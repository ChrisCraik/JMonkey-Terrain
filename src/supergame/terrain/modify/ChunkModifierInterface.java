
package supergame.terrain.modify;

import com.jme3.math.Vector3f;

import supergame.terrain.Chunk;

public interface ChunkModifierInterface {
    public float getModification(Vector3f p, float current);
    
    public void chunkCompletion(Chunk c);
}
