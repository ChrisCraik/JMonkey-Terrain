package supergame.character;

import com.jme3.math.Vector3f;
import supergame.Config;
import supergame.application.VerySimpleApplication;
import supergame.terrain.modify.BlockChunkModifier;
import supergame.terrain.modify.SphereChunkModifier;

public class Toolset {

    static final int NOTHING = 0;
    static final int TROWEL = 1;
    static final int SHOVEL = 2;
    static final int CUBEGUN = 3;

    private final Vector3f mTargetPos = new Vector3f();
    private final Vector3f mTargetVoxelPos = new Vector3f();
    private float mSecondsSinceShoot = 0;

    public void operate(Vector3f position, Vector3f targetDir,
            boolean use0, boolean use1, int toolSelection, float targetDistance) {
        mTargetPos.set(targetDir);
        mTargetPos.multLocal(targetDistance);
        mTargetPos.addLocal(position);

        // align the target to a voxel, always in unit coordinates
        mTargetVoxelPos.set(
                (float) Math.floor(mTargetPos.x + 0.5),
                (float) Math.floor(mTargetPos.y + 0.5),
                (float) Math.floor(mTargetPos.z + 0.5));

        mSecondsSinceShoot += VerySimpleApplication.tpf();
        if (toolSelection == NOTHING || (!use0 && !use1)) {
            return;
        }

        if (mSecondsSinceShoot > (Config.SHOOT_PERIOD_MS / 1000.0)) {
            mSecondsSinceShoot = 0;

            float increment = use1 ? -0.5f : 0.5f;

            switch (toolSelection) {
                case (TROWEL):
                    new BlockChunkModifier(mTargetVoxelPos, new Vector3f(0.5f, 0.5f, 0.5f), increment);
                    break;

                case (SHOVEL):
                    new SphereChunkModifier(mTargetVoxelPos, 2, use0);
                    break;

                case (CUBEGUN):
                    // TODO
                    break;
            }
        }
    }
}
