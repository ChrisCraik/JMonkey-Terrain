
package supergame.character;

import com.jme3.bullet.collision.shapes.CapsuleCollisionShape;
import com.jme3.bullet.control.CharacterControl;
import com.jme3.math.Vector3f;

import supergame.Config;
import supergame.gui.Game;
import supergame.network.PiecewiseLerp;
import supergame.network.Structs.ChatMessage;
import supergame.network.Structs.DesiredActionMessage;
import supergame.network.Structs.Entity;
import supergame.network.Structs.EntityData;

/**
 * A creature in the game world. It is controlled by a
 * {@link CreatureIntelligence} which provides it's desire. The
 * {@link CreatureIntelligence} defines which item the creature tries to use and
 * when, desired movement direction, and even spoken text.
 */
public class Creature extends Entity {

    private final static int LERP_FIELDS = 5;

    public static class CreatureData extends EntityData {
        // state for any given character that the server sends to the client
        float array[] = new float[LERP_FIELDS]; // x,y,z, heading, pitch
    }

    private float mHeading = 0;
    private float mPitch = 0;

    private final Equipment mEquipment = new Equipment();

    private final CharacterControl mCharacterControl;
    private final CreatureIntelligence mIntelligence;
    private DesiredActionMessage mDesiredActionMessage = new DesiredActionMessage();
    private final ChatMessage mChatMessage = new ChatMessage();
    private final PiecewiseLerp mStateLerp = new PiecewiseLerp(Config.CHAR_STATE_SAMPLES);

    // temporary vectors used for intermediate calculations. should not be
    // queried outside of the functions that set them.
    private final Vector3f mPosition = new Vector3f();

    public Creature(float x, float y, float z, CreatureIntelligence intelligence) {
        CapsuleCollisionShape capsuleShape = new CapsuleCollisionShape(1f, 2f, 1);
        mCharacterControl = new CharacterControl(capsuleShape, 0.05f);
        mCharacterControl.setJumpSpeed(20);
        mCharacterControl.setFallSpeed(30);
        mCharacterControl.setGravity(30);
        mCharacterControl.setPhysicsLocation(new Vector3f(x, y, z));
        Game.registerPhysics(mCharacterControl);

        mIntelligence = intelligence;

        // TODO: make node, add mCharacterControl as its control
    }

    public void setControlMessage(DesiredActionMessage message) {
        // FIXME: handle out of order messages

        // FIXME: only allow on server side, for remote players
        mDesiredActionMessage = message;
    }

    public void setChatMessage(ChatMessage chat) {
        mChatMessage.s = chat.s;
    }

    public void queryDesiredAction(double localTime) {
        if (mIntelligence != null) {
            // local controller
            mIntelligence.queryDesiredAction(localTime, mDesiredActionMessage, mChatMessage);
        }
        mHeading = mDesiredActionMessage.heading;
        mPitch = mDesiredActionMessage.pitch;

        Vector3f walkDirection = new Vector3f(mDesiredActionMessage.x, 0, mDesiredActionMessage.z);
        if (walkDirection.length() > 1f)
            walkDirection.normalize();

        if (mDesiredActionMessage.jump) {
            mCharacterControl.jump();
        }

        walkDirection.multLocal(0.25f);
        mCharacterControl.setWalkDirection(walkDirection);
    }

    public void processDesiredAction(boolean localToolsAllowed) {
        // TODO: move something renderable in place of the character
        mEquipment.processDesiredAction(mPosition, mDesiredActionMessage, localToolsAllowed);
    }

    public void processAftermath() {
        mCharacterControl.getPhysicsLocation(mPosition);

        if (mIntelligence != null) {
            mIntelligence.processAftermath(mPosition);
        }
    }

    @Override
    public void apply(double serverTime, EntityData packet) {
        // store position / look angles into interpolation window
        assert (packet instanceof CreatureData);
        CreatureData data = (CreatureData) packet;
        mStateLerp.addSample(serverTime, data.array);
    }

    @Override
    public EntityData getState() {
        CreatureData d = new CreatureData();
        // FIXME: use final ints to index into array
        d.array[0] = mPosition.x;
        d.array[1] = mPosition.y;
        d.array[2] = mPosition.z;
        d.array[3] = mHeading;
        d.array[4] = mPitch;
        return d;
    }

    private final float lerpFloats[] = new float[LERP_FIELDS];
    public void sample(double serverTime, float bias) {
        // sample interpolation window
        mStateLerp.sample(serverTime, lerpFloats);

        Vector3f pos = new Vector3f(lerpFloats[0], lerpFloats[1], lerpFloats[2]);
        mHeading = lerpFloats[3];
        mPitch = lerpFloats[4];

        //TODO: bias, so position = (old pos * (1-bias)) + (new pos * bias)
        mCharacterControl.setPhysicsLocation(pos);
    }

    public DesiredActionMessage getControl() {
        return mDesiredActionMessage;
    }

    public ChatMessage getChat() {
        return mChatMessage;
    }
}
