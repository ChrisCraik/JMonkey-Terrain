package supergame.network;

import com.esotericsoftware.kryonet.Server;

import supergame.ChunkIndex;
import supergame.character.Creature;
import supergame.character.CreatureIntelligence;
import supergame.network.Structs.ChunkMessage;
import supergame.network.Structs.StartMessage;

import java.util.HashMap;

public class ClientState {
    public final Creature mCreature;

    private final int mCreatureId;
    private String mName = null;
    private final HashMap<ChunkIndex, ChunkMessage> mChunksToSync = new HashMap<ChunkIndex, ChunkMessage>();

    public ClientState(ServerEntityManager server, int connectionId, CreatureIntelligence intelligence) {
        mCreature = new Creature(0f, 40f, 0f, intelligence);
        mCreatureId = server.registerEntity(mCreature);

        if (connectionId < 1) {
            return;
        }

        // tell client their character ID
        StartMessage m = new StartMessage();
        m.characterEntity = mCreatureId;
        ((Server)server.mEndPoint).sendToTCP(connectionId, m);
    }

    public void enqueueChunks(ChunkMessage[] chunks) {
        for (ChunkMessage c : chunks) {
            mChunksToSync.put(c.index, c);
        }
    }

    public ChunkMessage[] dequeueChunks() {
        if (mChunksToSync.isEmpty()) {
            return null;
        }

        // TODO: send only a few per frame

        System.err.println("sending " + mChunksToSync.size() + " chunks to new client");
        ChunkMessage messages[] = new ChunkMessage[mChunksToSync.size()];
        messages = mChunksToSync.values().toArray(messages);
        mChunksToSync.clear();

        return messages;
    }

    public void disconnect(ServerEntityManager server) {
        // Client has disconnected.
        server.unregisterEntity(mCreatureId);
    }

    public void setName(String name) {
        // TODO: set char name as well
        mName = name;
    }

    public String getName() {
        return mName;
    }
}
