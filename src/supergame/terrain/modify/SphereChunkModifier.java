
package supergame.terrain.modify;

import com.jme3.math.Vector3f;
import java.util.ArrayList;

import supergame.terrain.ChunkIndex;

public class SphereChunkModifier extends ChunkModifier {

    private final Vector3f mPosition;
    private final float mRadius;
    private final boolean mPositive;

    public SphereChunkModifier(Vector3f position, float radius, boolean positive) {
        mPosition = position;
        mRadius = radius;
        mPositive = positive;
    }

    @Override
    public ArrayList<ChunkIndex> getIndexList() {
        return getBoundingIndexList(
                mPosition.x, mPosition.y, mPosition.z,
                mRadius, mRadius, mRadius);
    }

    @Override
    public float getModification(Vector3f p, float current) {
        Vector3f origin = new Vector3f(mPosition);

        origin.subtractLocal(p);
        if (origin.length() <= mRadius) {
            // returning some positive constant here will give a vaguely round
            // shape, but giving a variant density over the surface smoothes it
            float newVal = 2 * (mRadius - origin.length());
            if (!mPositive)
                newVal *= -1;
            return Math.max(-1, Math.min(1, current + newVal));
        }

        return current;
    }
}
