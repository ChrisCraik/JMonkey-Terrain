package supergame.control;

import com.jme3.bullet.control.CharacterControl;
import com.jme3.math.Vector3f;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.AbstractControl;
import com.jme3.scene.control.Control;

import supergame.character.Character;
import supergame.character.Toolset;
import supergame.network.Structs;
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
    private final Vector3f mPosition = new Vector3f();
    private final Toolset mToolset = new Toolset();

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
        if (mMoveDirection.lengthSquared() > 1f || mIntent.sprint) {
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

        mCharacterControl.getPhysicsLocation(mPosition);
        mToolset.operate(mPosition, mIntent.heading, mIntent.pitch,
                mIntent.use0, mIntent.use1, mIntent.toolSelection, mIntent.targetDistance);
    }

    public Structs.EntityData generatePacket() {
        Character.CharacterData data = new Character.CharacterData();
        data.array[0] = mPosition.x;
        data.array[1] = mPosition.y;
        data.array[2] = mPosition.z;
        data.array[3] = mIntent.heading;
        data.array[4] = mIntent.heading;
        return data;
    }
}
