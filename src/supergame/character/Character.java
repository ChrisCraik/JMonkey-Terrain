package supergame.character;

import com.jme3.app.SimpleApplication;
import com.jme3.bullet.collision.shapes.CapsuleCollisionShape;
import com.jme3.bullet.control.CharacterControl;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Box;
import supergame.application.MaterialManager;
import supergame.application.VerySimpleApplication;
import supergame.appstate.PhysicsAppState;
import supergame.control.ConsumeIntentControl;
import supergame.control.LocalProduceIntentControl;
import supergame.control.client.RemoteControl;
import supergame.control.server.AiProduceIntentControl;
import supergame.control.server.RemoteProduceIntentControl;
import supergame.network.Structs;
import supergame.network.Structs.ChatMessage;
import supergame.network.Structs.Intent;

public class Character extends Structs.Entity {

    public static class CharacterData extends Structs.BasicSpatialData {}

    /*
    SERVER:
        Local player:
            LocalProduceIntentControl
            ConsumeIntentControl
            NetworkBroadcastControl
        AI:
            AiProduceIntentControl
            ConsumeIntentControl
            NetworkBroadcastControl
        Client:
            RemoteProduceIntentControl
            ConsumeIntentControl
            NetworkBroadcastControl
    CLIENT:
        Local player:
            LocalProduceIntentControl
            ConsumeIntentControl
            RemoteControl (special logic)
        Other:
            RemoteControl

    Server:
        maps client messages to spatial to RemoteProduceIntentControl
    Client:
        maps server messages to spatial to RemoteControls (local has special logic)

    Construct scenarios:

    Server creates character (for AI/other client)
        Server knows type, can just call create...()
        Client doesn't know type, but there's only one type currently - can just call create...(CLIENT_LOCAL)

        For client creating own local player, things are simpler
     */

    public static final int SERVER_LOCAL = 0;
    public static final int SERVER_AI = 1;
    public static final int SERVER_REMOTE = 2;
    public static final int CLIENT_LOCAL = 3;
    public static final int CLIENT_REMOTE = 4;

    private static final String[] TYPE_NAMES = {
            "Player",
            "Ai",
            "RemoteClient",
            "Player",
            "Remote"};

    final private static Box sBox = new Box(Vector3f.ZERO, 1, 2, 1);

    static private Spatial createCharacterSpatial(int type) {
        SimpleApplication app = VerySimpleApplication.getInstance(); // TODO: make this cleaner
        CharacterControl characterControl = createCharacterControl();

        Geometry geometry = new Geometry(TYPE_NAMES[type], sBox);
        geometry.setMaterial(MaterialManager.getCharacterMaterial());
        app.getStateManager().getState(PhysicsAppState.class).getPhysicsSpace().add(characterControl);

        if (type != CLIENT_REMOTE) {
            Intent intent = new Intent();
            geometry.addControl(characterControl);
            switch(type) {
                case SERVER_LOCAL:
                case CLIENT_LOCAL:
                    geometry.addControl(new LocalProduceIntentControl(app.getCamera(),
                            intent, app.getInputManager()));
                    break;
                case SERVER_AI:
                    geometry.addControl(new AiProduceIntentControl(intent));
                    break;
                case SERVER_REMOTE:
                    geometry.addControl(new RemoteProduceIntentControl(intent));
                    break;
                default:
                    throw new IllegalArgumentException("Unhandled character type");
            }
            geometry.addControl(new ConsumeIntentControl(intent, characterControl));
        }

        if (type == CLIENT_REMOTE || type == CLIENT_LOCAL) {
            geometry.addControl(new RemoteControl(characterControl));
        }

        app.getRootNode().attachChild(geometry);
        characterControl.setPhysicsLocation(new Vector3f(0, 80, 0));
        return geometry;
    }

    static private CharacterControl createCharacterControl() {
        CapsuleCollisionShape capsuleShape = new CapsuleCollisionShape(1, 2);
        CharacterControl characterControl = new CharacterControl(capsuleShape, 0.05f);
        characterControl.setJumpSpeed(20);
        characterControl.setFallSpeed(30);
        characterControl.setGravity(30);
        return characterControl;
    }

    public Character(int type) {
        super(createCharacterSpatial(type));
    }

    public Character() {
        // client only constructor
        this(CLIENT_REMOTE);
    }

    public Intent getIntent() {
        return mSpatial.getControl(LocalProduceIntentControl.class).getIntent();
    }

    public void setIntent(Intent intent) {
        mSpatial.getControl(RemoteProduceIntentControl.class).setIntent(intent);
    }

    public void setChatMessage(ChatMessage message) {
        System.out.println("char " + this + " set chat message" + message.s);
    }
}
