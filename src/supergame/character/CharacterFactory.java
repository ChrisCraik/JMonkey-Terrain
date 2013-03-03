package supergame.character;

import com.jme3.app.Application;
import com.jme3.bullet.collision.shapes.CapsuleCollisionShape;
import com.jme3.bullet.control.CharacterControl;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.shape.Box;

import supergame.control.ConsumeIntentControl;
import supergame.control.LocalProduceIntentControl;
import supergame.control.server.AiProduceIntentControl;
import supergame.gui.Game;
import supergame.network.Structs.Intent;

public class CharacterFactory {
    // TODO: clean this up
    final private static Box sBox = new Box(Vector3f.ZERO, 1, 2, 1);

    static public Geometry createPlayer(Application app) {
        CharacterControl characterControl = createCharacterControl();
        Geometry geometry = new Geometry("Player", sBox);
        geometry.setMaterial(Game.getCharacterMaterial());
        Intent intent = new Intent();

        geometry.addControl(new LocalProduceIntentControl(app.getCamera(), intent, app.getInputManager()));
        geometry.addControl(new ConsumeIntentControl(intent, characterControl));
        return geometry;
    }

    static public Geometry createAi(Application app) {
        CharacterControl characterControl = createCharacterControl();

        Geometry geometry = new Geometry("AiCreature", sBox);
        geometry.setMaterial(Game.getCharacterMaterial());
        Intent intent = new Intent();

        geometry.addControl(new AiProduceIntentControl(intent));
        geometry.addControl(new ConsumeIntentControl(intent, characterControl));
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
