
package supergame;

import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import supergame.character.Controller;
import supergame.network.Structs.ChatMessage;
import supergame.network.Structs.ControlMessage;


public class CameraController extends Controller {
    private final Vector3f mPosition = new Vector3f();
    private final Vector3f mForwardDir = new Vector3f();
    private float mPitchAngle, mHeadingAngle;
    private final Vector3f mStrafeDir = new Vector3f();

    private final Camera mCamera;

    /*package*/ CameraController(Camera camera) {
        mCamera = camera;
    }

    private int mToolSelection = 0;
    private int updateToolSelection() {
        if (Keyboard.isKeyDown(Keyboard.KEY_0)) {
            mToolSelection = 0;
        } else if (Keyboard.isKeyDown(Keyboard.KEY_1)) {
            mToolSelection = 1;
        } else if (Keyboard.isKeyDown(Keyboard.KEY_2)) {
            mToolSelection = 2;
        } else if (Keyboard.isKeyDown(Keyboard.KEY_3)) {
            mToolSelection = 3;
        }
        return mToolSelection;
    }

    static final float MIN_TARGET = 3;
    static final float MAX_TARGET = 10;
    private float mTargetDistance = 5;
    private float updateTargetDistance() {
        while (Keyboard.next()) {
            if (Keyboard.getEventKey() == Keyboard.KEY_UP
                    && Keyboard.getEventKeyState()) {
                mTargetDistance += 1;
            } else if (Keyboard.getEventKey() == Keyboard.KEY_DOWN
                    && Keyboard.getEventKeyState()) {
                mTargetDistance -= 1;
            }
        }
        mTargetDistance += Mouse.getDWheel() / 1000.0;
        mTargetDistance = Math.min(mTargetDistance, MAX_TARGET);
        mTargetDistance = Math.max(mTargetDistance, MIN_TARGET);
        return mTargetDistance;
    }

    /**
     * Process input from the mouse and keyboard, adjusting the heading and pitch of the
     * camera, and any character under direct control.
     */
    @Override
    public void control(double localTime, ControlMessage control, ChatMessage chat) {
        float angles[] = mCamera.getRotation().toAngles(null);
        mPitchAngle = angles[2];
        mHeadingAngle = angles[0];

        // calculate absolute forward/strafe
        mCamera.getDirection(mForwardDir);
        mCamera.getLeft(mStrafeDir);

        control.heading = mHeadingAngle;
        control.pitch = mPitchAngle;

        if (tryChat(chat)) {
            // still chatting, so ignore keys for movement, tool select
            return;
        }

        // set walkDirection for character
        Vector3f walkDirection = new Vector3f(0, 0, 0);
        if (Keyboard.isKeyDown(Keyboard.KEY_W))
            walkDirection.addLocal(mForwardDir);
        if (Keyboard.isKeyDown(Keyboard.KEY_S))
            walkDirection.addLocal(mForwardDir.negate());
        if (Keyboard.isKeyDown(Keyboard.KEY_A))
            walkDirection.addLocal(mStrafeDir);
        if (Keyboard.isKeyDown(Keyboard.KEY_D))
            walkDirection.addLocal(mStrafeDir.negate());

        control.x = walkDirection.x;
        control.z = walkDirection.z;
        control.jump = Keyboard.isKeyDown(Keyboard.KEY_SPACE);
        control.duck = Keyboard.isKeyDown(Keyboard.KEY_LSHIFT);
        control.sprint = Keyboard.isKeyDown(Keyboard.KEY_LCONTROL);

        control.use0 = Mouse.isButtonDown(0);
        control.use1 = Mouse.isButtonDown(1);
        control.toolSelection = updateToolSelection();
        control.targetDistance = updateTargetDistance();
        // TODO: movement when no char attached

        if (/*Game.heartbeatFrame*/ false) {
            System.out.println("dir:" + walkDirection + ", pitch:" + mPitchAngle + ", heading:"
                    + mHeadingAngle);
        }
    }

    private final String mCurrent = null;
    private boolean tryChat(ChatMessage chat) {
        /*
        if (mCurrent == null) {
            while (InputProcessor.PollKeyboard()) {
                if (Keyboard.getEventKey() == Keyboard.KEY_RETURN) {
                    mCurrent = "";
                    break;
                }
            }
        }

        if (mCurrent == null)
            return false;

        // build up the string the player is typing until a 'return'
        while (InputProcessor.PollKeyboard()) {
            if (Keyboard.getEventKey() == Keyboard.KEY_BACK
                    && !mCurrent.isEmpty()) {
                mCurrent = mCurrent.substring(0, mCurrent.length() - 1);
            } else if (Keyboard.getEventKey() == Keyboard.KEY_RETURN) {
                chat.s = mCurrent;
                mCurrent = null;
                ChatDisplay.current = null;
                return true;
            } else {
                mCurrent += Keyboard.getEventCharacter();
            }
        }
        ChatDisplay.current = mCurrent;
        return true;
    */
        return false;
    }

    @Override
    public void response(Vector3f pos) {
        mPosition.set(pos);
    }
}
