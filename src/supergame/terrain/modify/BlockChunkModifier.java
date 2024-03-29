
package supergame.terrain.modify;

import com.jme3.math.Vector3f;
import java.util.ArrayList;

import supergame.terrain.ChunkIndex;

public class BlockChunkModifier extends ChunkModifier {
    private final Vector3f mPosition;
    private final Vector3f mSize;
    private final float mIncrement;

    public BlockChunkModifier(Vector3f position, Vector3f size, float increment) {
        mPosition = position;
        mSize = size;
        mIncrement = increment;
    }

    @Override
    public float getModification(Vector3f p, float current) {
        if (p.x < mPosition.x - mSize.x || p.x > mPosition.x + mSize.x)
            return current;
        if (p.y < mPosition.y - mSize.y || p.y > mPosition.y + mSize.y)
            return current;
        if (p.z < mPosition.z - mSize.z || p.z > mPosition.z + mSize.z)
            return current;

        return Math.max(-1, Math.min(1, mIncrement + current));
    }

    @Override
    public ArrayList<ChunkIndex> getIndexList() {
        return getBoundingIndexList(
                mPosition.x, mPosition.y, mPosition.z,
                mSize.x, mSize.y, mSize.z);
    }
}
