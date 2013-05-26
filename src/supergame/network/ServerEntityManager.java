
package supergame.network;

import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Server;

import com.jme3.app.SimpleApplication;
import supergame.Config;
import supergame.appstate.NetworkAppState;
import supergame.network.Structs.ChatMessage;
import supergame.network.Structs.ChunkMessage;
import supergame.network.Structs.Entity;
import supergame.network.Structs.EntityData;
import supergame.network.Structs.Intent;
import supergame.network.Structs.StateMessage;
import supergame.terrain.modify.ChunkModifier;
import supergame.utils.Log;

import java.io.IOException;
import java.nio.channels.WritableByteChannel;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ServerEntityManager extends EntityManager {
    private static Logger sLog = Log.getLogger(ServerEntityManager.class.getName(), Level.ALL);

    private int mNextEntityId = 1;

    // TODO: this assumes 0 isn't a valid connection. is that guaranteed?
    public static final int LOCAL_CONN = 0;

    /**
     * Normal game server constructor.
     */
    public ServerEntityManager() {
        super(new Server(Config.WRITE_BUFFER_SIZE, Config.OBJECT_BUFFER_SIZE), null, null);
    }

    /**
     * Game server that also saves sent packets for future playback to a virtual
     * client.
     *
     * @param w The server stores packets it sends in this.
     */
    public ServerEntityManager(WritableByteChannel w) {
        super(new Server(Config.WRITE_BUFFER_SIZE, Config.OBJECT_BUFFER_SIZE), w, null);
        throw new UnsupportedOperationException(); // not currently working
    }

    /**
     * Get local changes server side, to be sent remotely
     */
    public HashMap<Integer, EntityData> getEntityChanges() {
        // TODO: reuse map
        HashMap<Integer, EntityData> changeMap = new HashMap<Integer, EntityData>();

        for (Map.Entry<Integer, Entity> entry : mEntityMap.entrySet()) {
            changeMap.put(entry.getKey(), entry.getValue().getState());
        }
        return changeMap;
    }

    /**
     * Server-side registration of network entities
     *
     * @param entity New entity to register (should usually be 'this' as this is
     *            called from entity constructors)
     */
    int registerEntity(Entity entity) {
        mNextEntityId++;
        registerEntity(entity, mNextEntityId);
        return mNextEntityId;
    }

    public void unregisterEntity(int entityId) {
        mEntityMap.remove(entityId);
    }

    public void sendToAllTCP(Object o) {
        ((Server) mEndPoint).sendToAllTCP(o);
    }

    public void sendToAllUDP(Object o) {
        ((Server) mEndPoint).sendToAllUDP(o);
    }

    public void bind(int tcp, int udp) throws IOException {
        sLog.info("binding to tcp, udp...");
        ((Server) mEndPoint).start(); // there's probably a reason not to do this here...
        ((Server) mEndPoint).bind(tcp, udp);
    }

    // map connection id to client/character structure
    HashMap<Integer, ClientState> mClientStateMap = new HashMap<Integer, ClientState>();

    private boolean connectionIsValid(int connectionId) {
        // FIXME: do lookup instead
        for (Connection c : ((Server) mEndPoint).getConnections()) {
            if (c.getID() == connectionId) {
                return true;
            }
        }
        return false;
    }

    private void processChatMessage(double localTime, ChatMessage chat) {
        if (chat == null || chat.s == null) {
            return;
        }

        String s = chat.s;

        if (s.isEmpty()) {
            chat.s = null;
            return;
        }

        // TODO: chat filtration, command parsing
        // TODO: add name to string

        ((Server)mEndPoint).sendToAllTCP(chat);

        // TODO: chat display
        //mChatDisplay.addChat(localTime, s, Color.white);
        chat.s = null; // clear string to mark as processed for local reuse
    }

    private void updateConnections() {
        // if a connection doesn't remain, delete the char
        for (Integer connectionId : mClientStateMap.keySet()) {
            if (connectionId == LOCAL_CONN || connectionIsValid(connectionId)) {
                continue;
            }

            ClientState oldState = mClientStateMap.remove(connectionId);
            oldState.disconnect(this);
        }

        // create a local ClientState/Creature, if it hasn't been done yet
        if (!mClientStateMap.containsKey(LOCAL_CONN)) {
            sLog.info("creating char for local connection " + 0);
            ClientState localState = new ClientState(this, LOCAL_CONN);
            mClientStateMap.put(LOCAL_CONN, localState);

            /*
            // Create NPC
            Creature npc = new Creature(Creature.SERVER_AI);
            registerEntity(npc);
            */
        }

        // new connection: create ClientState/Creature
        for (Connection c : ((Server) mEndPoint).getConnections()) {
            if (!mClientStateMap.containsKey(c.getID())) {
                sLog.info("New connection " + c.getID() + ", creating char for connection " + c.getID());
                ClientState newClient = new ClientState(this, c.getID());
                mClientStateMap.put(c.getID(), newClient);

                // enqueue all previously modified chunks to the new client
                newClient.enqueueChunks(ChunkModifier.server_getAllModified());
            }
        }
    }

    @Override
    public void update(SimpleApplication app) {
        updateConnections();

        // receive control messages from clients
        TransmitPair pair;
        for (;;) {
            pair = pollHard(NetworkAppState.getLocalNetworkTime(), 0);
            if (pair == null){
                break;
            }

            ClientState remoteClient = mClientStateMap.get(pair.connection.getID());
            if (remoteClient == null) {
                continue;
            }

            if (pair.object instanceof Intent) {
                // Client updates server with state
                Intent state = ((Intent) pair.object);
                remoteClient.mCreature.setIntent(state);
            } else if (pair.object instanceof ChatMessage) {
                // Client updates server with state
                ChatMessage state = ((ChatMessage) pair.object);
                remoteClient.mCreature.setChatMessage(state);
            }
        }

        // send entity updates to clients
        StateMessage serverState = new StateMessage();
        serverState.timestamp = NetworkAppState.getLocalNetworkTime();
        serverState.data = getEntityChanges();
        sendToAllUDP(serverState);

        // send chunk updates to clients
        ChunkMessage[] recentModified = ChunkModifier.server_getRecentModified();
        for (ChunkMessage message : recentModified) {
            sendToAllTCP(message);
        }

        // send chunk initialization to new clients
        for (int connectionId : mClientStateMap.keySet()) {
            if (connectionId == LOCAL_CONN) {
                continue;
            }

            ClientState clientState = mClientStateMap.get(connectionId);
            // clientState.enqueueChunks(recentModified);

            ChunkMessage[] chunksToSend = clientState.dequeueChunks();
            if (chunksToSend != null) {
                for (ChunkMessage message : chunksToSend) {
                    ((Server) mEndPoint).sendToTCP(connectionId, message);
                }
            }
        }
    }
}
