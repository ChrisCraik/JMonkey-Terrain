
package supergame.character;

import com.jme3.math.Vector3f;

import supergame.Config;
import supergame.SuperSimpleApplication;
import supergame.network.Structs.DesiredActionMessage;
import supergame.terrain.modify.BlockChunkModifier;
import supergame.terrain.modify.SphereChunkModifier;

@Deprecated
public class Equipment {
    public static void HPVector(Vector3f vec, float heading, float pitch) {
        float x,y,z;
        x = -(float) (Math.sin(heading * Math.PI / 180.0) * Math.cos(pitch * Math.PI / 180.0));
        y = (float) (Math.sin(pitch * Math.PI / 180.0));
        z = (float) (Math.cos(heading * Math.PI / 180.0) * Math.cos(pitch * Math.PI / 180.0));

        vec.set(x, y, z);
    }

    static final int NOTHING = 0;
    static final int TROWEL = 1;
    static final int SHOVEL = 2;
    static final int CUBEGUN = 3;
    static final float MIN_TARGET = 3;
    static final float MAX_TARGET = 10;
    static final float V = 40;

    private final Vector3f mTargetDir = new Vector3f();
    private final Vector3f mTargetPos = new Vector3f();
    private final Vector3f mTargetVoxelPos = new Vector3f();

    private static float mSecondsSinceShoot = 0;

    public void processDesiredAction(Vector3f position, DesiredActionMessage message, boolean localToolsAllowed) {
        HPVector(mTargetDir, message.heading, message.pitch);

        mTargetPos.set(mTargetDir);
        mTargetPos.multLocal(message.targetDistance);
        mTargetPos.addLocal(position);

        // align the target to a voxel
        mTargetVoxelPos.set(
                (float) Math.floor(mTargetPos.x + 0.5),
                (float) Math.floor(mTargetPos.y + 0.5),
                (float) Math.floor(mTargetPos.z + 0.5));

        /*
        GL11.glPushMatrix();
        GL11.glTranslatef(mTargetPos.x, mTargetPos.y, mTargetPos.z);
        Game.collision.drawCube(0.1f, 0.1f, 0.1f);
        GL11.glPopMatrix();
         */
        if (message.toolSelection == NOTHING) {
            return;
        }

        if (message.toolSelection == TROWEL || message.toolSelection == SHOVEL) {
/*
            GL11.glPushMatrix();
            GL11.glTranslatef(mTargetVoxelPos.x, mTargetVoxelPos.y, mTargetVoxelPos.z);
            Game.collision.drawCube(1, 0.02f, 0.02f);
            Game.collision.drawCube(0.02f, 1, 0.02f);
            Game.collision.drawCube(0.02f, 0.02f, 1);
            GL11.glPopMatrix();
*/
        }

        if (localToolsAllowed) {
            useTool(message);
        }
    }

    private void useTool(DesiredActionMessage message) {
        mSecondsSinceShoot += SuperSimpleApplication.tpf();
        if (mSecondsSinceShoot > (Config.SHOOT_PERIOD_MS / 1000.0) && (message.use0 || message.use1)) {
            mSecondsSinceShoot = 0;

            float increment = 0.5f;
            if (message.use1) {
                increment *= -1;
            }

            switch (message.toolSelection) {
                case (TROWEL):
                    new BlockChunkModifier(mTargetVoxelPos, new Vector3f(0.5f, 0.5f, 0.5f), increment);
                    break;

                case (SHOVEL):
                    new SphereChunkModifier(mTargetVoxelPos, 2, message.use0);
                    break;

                case (CUBEGUN):
                    /*
                    Game.collision.add(Game.collision.getPhysics().createCube(0.25f, 0.5f,
                            mTargetPos.x, mTargetPos.y, mTargetPos.z,
                            V * mTargetDir.x, V * mTargetDir.y, V * mTargetDir.z));
                            */
                    break;
            }
        }
    }
}
