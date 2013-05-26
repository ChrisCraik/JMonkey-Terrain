package supergame.network;

import com.esotericsoftware.kryonet.Server;

import supergame.character.Character;
import supergame.network.Structs.ChunkMessage;
import supergame.network.Structs.StartMessage;
import supergame.terrain.ChunkIndex;

import java.util.HashMap;

public class ClientState {
    public final Character mCharacter;

    private final int mCreatureId;
    private String mName = null;
    private final HashMap<ChunkIndex, ChunkMessage> mChunksToSync = new HashMap<ChunkIndex, ChunkMessage>();

    public ClientState(ServerEntityManager server, int connectionId) {
        int charType = (connectionId == ServerEntityManager.LOCAL_CONN) ?
                Character.SERVER_LOCAL : Character.SERVER_REMOTE;
        mCharacter = new Character(charType);
        mCreatureId = server.registerEntity(mCharacter);

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
