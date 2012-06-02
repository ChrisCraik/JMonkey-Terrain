
package supergame.character;

import com.jme3.math.Vector3f;

import supergame.network.Structs.ChatMessage;
import supergame.network.Structs.DesiredActionMessage;

/**
 * A CreatureIntelligence controls a specific creature.
 */
public abstract class CreatureIntelligence {
    /**
     * Called each frame to query the Creature's desired actions.
     *
     * @param localTime Current time
     * @param message Message in which to store desired action.
     * @param chat Message in which to store desired chat.
     */
    // TODO: pass info about where it's char, and other chars are
    public abstract void queryDesiredAction(double localTime, DesiredActionMessage message, ChatMessage chat);

    /**
     * Called after physics evaluation, to update the Intelligence as to the new
     * location of the creature.
     *
     * @param pos The new position of the Creature.
     */
    public abstract void processAftermath(Vector3f pos);
}
