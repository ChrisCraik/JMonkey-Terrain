
package supergame.character;

import com.jme3.input.InputManager;
import com.jme3.input.controls.ActionListener;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;

import org.lwjgl.input.Keyboard;

import supergame.network.Structs.ChatMessage;
import supergame.network.Structs.DesiredActionMessage;

public class LocalPlayerCreatureIntelligence extends CreatureIntelligence implements ActionListener {
    private final Vector3f mWalkDir = new Vector3f();
    private final Vector3f mForwardDir = new Vector3f();
    private final Vector3f mStrafeDir = new Vector3f();

    private final Camera mCamera;

    private boolean mLeft, mRight, mForward, mBack, mJump;
    private boolean mToolMain, mToolSecondary;
    private int mToolSelection = 0;

    private static final float MIN_TARGET = 3;
    private static final float MAX_TARGET = 10;
    private float mTargetDistance = 5;

    public LocalPlayerCreatureIntelligence(Camera camera, InputManager inputManager) {
        mCamera = camera;

        inputManager.addListener(this, "left");
        inputManager.addListener(this, "right");
        inputManager.addListener(this, "forward");
        inputManager.addListener(this, "back");
        inputManager.addListener(this, "jump");

        inputManager.addListener(this, "target-forward");
        inputManager.addListener(this, "target-back");

        inputManager.addListener(this, "tool-main");
        inputManager.addListener(this, "tool-secondary");

        inputManager.addListener(this, "select-tool-0");
        inputManager.addListener(this, "select-tool-1");
        inputManager.addListener(this, "select-tool-2");
        inputManager.addListener(this, "select-tool-3");

        mLeft = mRight = mForward = mBack = mJump = false;
        mToolMain = mToolSecondary = false;
    }

    @Override
    public void onAction(String binding, boolean value, float arg2) {
        if (binding.equals("left")) {
            mLeft = value;
        } else if (binding.equals("right")) {
            mRight = value;
        } else if (binding.equals("forward")) {
            mForward = value;
        } else if (binding.equals("back")) {
            mBack = value;
        } else if (binding.equals("jump")) {
            mJump = value;
        } else if (binding.equals("target-forward")) {
            mTargetDistance = Math.max(mTargetDistance + 1, MAX_TARGET);
        } else if (binding.equals("target-back")) {
            mTargetDistance = Math.max(mTargetDistance - 1, MIN_TARGET);
        } else if (binding.equals("tool-main")) {
            mToolMain = value;
        } else if (binding.equals("tool-secondary")) {
            mToolSecondary = value;
        }

        if (!value) return; // only interested in key down below

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

    /**
     * Process input from the mouse and keyboard, adjusting the heading and pitch of the
     * camera, and any character under direct control.
     */
    @Override
    public void queryDesiredAction(double localTime, DesiredActionMessage control, ChatMessage chat) {
        float angles[] = mCamera.getRotation().toAngles(null);
        control.pitch = - (float) (angles[0] * 180 / Math.PI);
        control.heading =  - (float) (angles[1] * 180 / Math.PI);

        // TODO: early return if chatting

        // calculate absolute forward/strafe
        mCamera.getDirection(mForwardDir);
        mForwardDir.y = 0;
        mForwardDir.normalizeLocal();
        mCamera.getLeft(mStrafeDir);

        // set walkDirection for character
        mWalkDir.set(0, 0, 0);
        if (mForward)
            mWalkDir.addLocal(mForwardDir);
        if (mBack)
            mWalkDir.subtractLocal(mForwardDir);
        if (mRight)
            mWalkDir.addLocal(mStrafeDir);
        if (mLeft)
            mWalkDir.subtractLocal(mStrafeDir);

        control.x = mWalkDir.x;
        control.z = mWalkDir.z;
        control.jump = mJump;
        control.duck = Keyboard.isKeyDown(Keyboard.KEY_LSHIFT);
        control.sprint = Keyboard.isKeyDown(Keyboard.KEY_LCONTROL);

        control.use0 = mToolMain;
        control.use1 = mToolSecondary;
        control.toolSelection = mToolSelection;
        control.targetDistance = mTargetDistance;

        // TODO: movement when no char attached
    }

    @Override
    public void processAftermath(Vector3f pos) {
        mCamera.setLocation(pos);
    }
}
