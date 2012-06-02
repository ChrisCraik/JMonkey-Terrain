
package supergame.character;

import com.jme3.math.Vector3f;

import supergame.network.Structs.ChatMessage;
import supergame.network.Structs.DesiredActionMessage;

/**
 * Simple AI for NPCs. Currently just spins the character in circles.
 */
public class ArtificialCreatureIntelligence extends CreatureIntelligence {
    @Override
    public void queryDesiredAction(double localTime, DesiredActionMessage control, ChatMessage chat) {
        control.x = (float) Math.sin(localTime / 400.0) / 4;
        control.z = (float) Math.cos(localTime / 400.0) / 4;
        System.err.println(this + "moving AI x " + control.x + ", z " + control.z);
    }

    @Override
    public void processAftermath(Vector3f pos) {
        System.err.println(this + " at " + pos);
    }
}
