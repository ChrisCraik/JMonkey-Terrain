
package supergame.control.client;

import com.jme3.bullet.control.CharacterControl;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.AbstractControl;
import com.jme3.scene.control.Control;

import supergame.Config;
import supergame.character.Creature;
import supergame.control.LocalProduceIntentControl;
import supergame.network.ClientEntityManager;
import supergame.network.PiecewiseLerp;
import supergame.network.Structs.EntityData;
import supergame.utils.GeometryUtils;

/**
 * Represents a server-controlled position/pitch/heading. Receives server
 * updates via {@link #applyUpdatePacket(double, EntityData) applyUpdatePacket}
 * method and updates the spatial and CharacterControl accordingly each frame,
 * linearly interpolating between packets in the past
 */
public class RemoteControl extends AbstractControl {
    private final static int LERP_FIELDS = 5;

    PiecewiseLerp mLerp = new PiecewiseLerp(Config.CHAR_STATE_SAMPLES);
    private final CharacterControl mCharacterControl;

    public RemoteControl(CharacterControl characterControl) {
        if (characterControl == null) {
            throw new IllegalArgumentException();
        }
        mCharacterControl = characterControl;
    }

    public void applyUpdatePacket(double serverTime, EntityData packet) {
        Creature.CreatureData data = (Creature.CreatureData) packet;
        mLerp.addSample(serverTime, data.array);
    }

    @Override
    public Control cloneForSpatial(Spatial spatial) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void controlRender(RenderManager arg0, ViewPort arg1) {
    }

    private final Vector3f mPosition = new Vector3f();
    private final Vector3f mTargetDir = new Vector3f();
    private final float mLerpFloats[] = new float[LERP_FIELDS];
    private final Quaternion mQuaternion = new Quaternion();

    @Override
    protected void controlUpdate(float tpf) {
        if (spatial.getControl(LocalProduceIntentControl.class) != null) {
            // TODO: should use bias if local produce exists, and snap if necessary
            return;
        }

        mLerp.sample(ClientEntityManager.getLerpTime(), mLerpFloats);

        mPosition.set(mLerpFloats[0], mLerpFloats[1], mLerpFloats[2]);
        float heading = mLerpFloats[3];
        float pitch = mLerpFloats[4];

        // sample the linearInter
        mCharacterControl.setPhysicsLocation(mPosition);
        spatial.setLocalTranslation(mPosition);

        // TODO: simpler / more elegant quaternion setup
        GeometryUtils.HPVector(mTargetDir, heading, pitch);
        mQuaternion.lookAt(mTargetDir, Vector3f.UNIT_Y);
        spatial.setLocalRotation(mQuaternion);
    }

}
