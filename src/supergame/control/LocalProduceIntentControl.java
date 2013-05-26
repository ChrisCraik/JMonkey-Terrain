
package supergame.control;

import com.jme3.input.InputManager;
import com.jme3.input.controls.ActionListener;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.AbstractControl;
import com.jme3.scene.control.Control;

import supergame.appstate.NetworkAppState;
import supergame.network.Structs.Intent;

/**
 * Drives Intent from camera and local player input.
 */
public class LocalProduceIntentControl extends AbstractControl {

    private class LocalListener implements ActionListener {
        static final float MIN_TARGET = 3;
        static final float MAX_TARGET = 10;
        float mTargetDistance = 5;

        boolean mLeft, mRight, mForward, mBack, mJump, mDuck, mSprint;
        boolean mToolMain, mToolSecondary;
        int mToolSelection = 0;

        LocalListener(InputManager inputManager) {
            inputManager.addListener(this, "left");
            inputManager.addListener(this, "right");
            inputManager.addListener(this, "forward");
            inputManager.addListener(this, "back");

            inputManager.addListener(this, "jump");
            inputManager.addListener(this, "duck");
            inputManager.addListener(this, "sprint");

            inputManager.addListener(this, "target-forward");
            inputManager.addListener(this, "target-back");

            inputManager.addListener(this, "tool-main");
            inputManager.addListener(this, "tool-secondary");

            inputManager.addListener(this, "select-tool-0");
            inputManager.addListener(this, "select-tool-1");
            inputManager.addListener(this, "select-tool-2");
            inputManager.addListener(this, "select-tool-3");
        }

        @Override
        public void onAction(String binding, boolean keyDown, float arg2) {
            if (binding.equals("left")) {
                mLeft = keyDown;
            } else if (binding.equals("right")) {
                mRight = keyDown;
            } else if (binding.equals("forward")) {
                mForward = keyDown;
            } else if (binding.equals("back")) {
                mBack = keyDown;
            } else if (binding.equals("jump")) {
                mJump = keyDown;
            } else if (binding.equals("duck")) {
                mDuck = keyDown;
            } else if (binding.equals("sprint")) {
                mSprint = keyDown;
            } else if (binding.equals("target-forward")) {
                mTargetDistance = Math.max(mTargetDistance + 1, MAX_TARGET);
            } else if (binding.equals("target-back")) {
                mTargetDistance = Math.max(mTargetDistance - 1, MIN_TARGET);
            } else if (binding.equals("tool-main")) {
                mToolMain = keyDown;
            } else if (binding.equals("tool-secondary")) {
                mToolSecondary = keyDown;
            }

            if (!keyDown) return; // only interested in key down below

            if (binding.equals("select-tool-0")) {
                mToolSelection = 0;
            } else if (binding.equals("select-tool-1")) {
                mToolSelection = 1;
            } else if (binding.equals("select-tool-2")) {
                mToolSelection = 2;
            } else if (binding.equals("select-tool-3")) {
                mToolSelection = 3;
            }
        }
    }

    private final Camera mCamera;
    private final Intent mIntent;
    private final LocalListener mLocalListener;

    public Intent getIntent() { return mIntent; }

    public LocalProduceIntentControl(Camera camera, Intent intent, InputManager inputManager) {
        mCamera = camera;
        mIntent = intent;
        mLocalListener = new LocalListener(inputManager);
    }

    @Override
    public Control cloneForSpatial(Spatial arg0) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void controlRender(RenderManager renderManager, ViewPort viewPort) {
    }

    private final Vector3f mWalkDir = new Vector3f();
    private final Vector3f mForwardDir = new Vector3f();
    private final Vector3f mStrafeDir = new Vector3f();

    @Override
    protected void controlUpdate(float tpf) {
        mCamera.setLocation(spatial.getLocalTranslation());
        mCamera.getDirection(mForwardDir);
        mForwardDir.y = 0;
        mForwardDir.normalizeLocal();
        mCamera.getLeft(mStrafeDir);

        // set walkDirection for character
        mWalkDir.set(0, 0, 0);
        if (mLocalListener.mForward) {
            mWalkDir.addLocal(mForwardDir);
        }
        if (mLocalListener.mBack) {
            mWalkDir.subtractLocal(mForwardDir);
        }
        if (mLocalListener.mRight) {
            mWalkDir.addLocal(mStrafeDir);
        }
        if (mLocalListener.mLeft) {
            mWalkDir.subtractLocal(mStrafeDir);
        }

        mIntent.timestamp = NetworkAppState.getLocalNetworkTime();
        mIntent.x = mWalkDir.x;
        mIntent.z = mWalkDir.z;
        float angles[] = mCamera.getRotation().toAngles(null);
        mIntent.pitch = - (float) (angles[0] * 180 / Math.PI);
        mIntent.heading =  - (float) (angles[1] * 180 / Math.PI);
        mIntent.jump = mLocalListener.mJump;
        mIntent.duck = mLocalListener.mDuck;
        mIntent.sprint = mLocalListener.mSprint;

        mIntent.use0 = mLocalListener.mToolMain;
        mIntent.use1 = mLocalListener.mToolSecondary;
        mIntent.toolSelection = mLocalListener.mToolSelection;
        mIntent.targetDistance = mLocalListener.mTargetDistance;
    }

}
