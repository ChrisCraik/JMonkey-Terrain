package supergame.character;

import com.jme3.app.SimpleApplication;
import com.jme3.bullet.collision.shapes.CapsuleCollisionShape;
import com.jme3.bullet.control.CharacterControl;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.shape.Box;

import supergame.application.MaterialManager;
import supergame.appstate.PhysicsAppState;
import supergame.control.ConsumeIntentControl;
import supergame.control.LocalProduceIntentControl;
import supergame.control.server.AiProduceIntentControl;
import supergame.network.Structs.Intent;

public class CharacterFactory {
    // TODO: clean this up
    final private static Box sBox = new Box(Vector3f.ZERO, 1, 2, 1);

    static public Geometry createPlayer(SimpleApplication app) {
        return createCharacter(app, true);
    }

    static public Geometry createAi(SimpleApplication app) {
        return createCharacter(app, false);
    }

    static private Geometry createCharacter(SimpleApplication app, boolean isPlayer) {
        CharacterControl characterControl = createCharacterControl();

        Geometry geometry = new Geometry(isPlayer ? "Player" : "AiCreature", sBox);
        geometry.setMaterial(MaterialManager.getCharacterMaterial());
        Intent intent = new Intent();

        app.getStateManager().getState(PhysicsAppState.class).getPhysicsSpace().add(characterControl);

        geometry.addControl(characterControl);
        if (isPlayer) {
            geometry.addControl(new LocalProduceIntentControl(app.getCamera(), intent, app.getInputManager()));
        } else {
            geometry.addControl(new AiProduceIntentControl(intent));
        }
        geometry.addControl(new ConsumeIntentControl(intent, characterControl));
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

}
