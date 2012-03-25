
package supergame.character;

import com.jme3.math.Vector3f;

import supergame.network.Structs.ChatMessage;
import supergame.network.Structs.ControlMessage;

public abstract class Controller {
    // TODO: pass info about where it's char, and other chars are
    public abstract void control(double localTime, ControlMessage control, ChatMessage chat);

    // response - callback after physics step and game logic
    public abstract void response(Vector3f pos);
}
