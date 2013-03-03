package supergame.control;

import com.jme3.bullet.control.CharacterControl;
import com.jme3.math.Vector3f;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.AbstractControl;
import com.jme3.scene.control.Control;

import supergame.network.Structs.Intent;

/**
 * Translate incoming Intents (from the local player, server, or AI) into
 * movement of the character's physics object.
 *
 * On the server, determines position of all characters.
 *
 * On the client, determines a prediction of where the local player (only!) is.
 */
public class ConsumeIntentControl extends AbstractControl {
    private final Intent mIntent;
    private final CharacterControl mCharacterControl;

    private final Vector3f mMoveDirection = new Vector3f();

    // time from liftoff to peak of a jump, used to workaround onGround bug
    private final double JUMP_TIME_TO_PEAK = 0.667;
    private double mTimeSinceLastJump = 0;

    public ConsumeIntentControl(Intent intent, CharacterControl control) {
        if (intent == null || control == null) throw new IllegalArgumentException();

        mIntent = intent;
        mCharacterControl = control;
    }

    @Override
    public Control cloneForSpatial(Spatial spatial) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void controlRender(RenderManager renderManager, ViewPort viewPort) {
        // TODO
    }

    @Override
    protected void controlUpdate(float tpf) {
        mMoveDirection.set(mIntent.x, 0, mIntent.z);
        if (mMoveDirection.lengthSquared() > 1f) {
            mMoveDirection.normalizeLocal();
        }
        mMoveDirection.multLocal(mIntent.sprint ? 0.5f : 0.25f);
        mCharacterControl.setWalkDirection(mMoveDirection);

        mTimeSinceLastJump += tpf;
        if (mIntent.jump && mCharacterControl.onGround()) {
            if (Math.abs(mTimeSinceLastJump - JUMP_TIME_TO_PEAK) > 0.05) {
                mCharacterControl.jump();
                mTimeSinceLastJump = 0;
            }
        }
    }

}
