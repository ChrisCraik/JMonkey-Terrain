
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
        control.x = (float) Math.sin(localTime) / 4;
        control.z = (float) Math.cos(localTime) / 4;
    }

    @Override
    public void processAftermath(Vector3f pos) {
    }
}
