
package supergame.network;

import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Server;

import supergame.Config;
import supergame.SuperSimpleApplication;
import supergame.character.ArtificialCreatureIntelligence;
import supergame.character.Creature;
import supergame.network.Structs.ChatMessage;
import supergame.network.Structs.ChunkMessage;
import supergame.network.Structs.DesiredActionMessage;
import supergame.network.Structs.Entity;
import supergame.network.Structs.EntityData;
import supergame.network.Structs.StateMessage;
import supergame.terrain.modify.ChunkModifier;

import java.io.IOException;
import java.nio.channels.WritableByteChannel;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class ServerEntityManager extends EntityManager {
    private int mNextEntityId = 1;

    // FIXME: this assumes 0 isn't a valid connection. is that guaranteed?
    private static final int LOCAL_CONN = 0;

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
    }

    /**
     * Get local changes server side, to be sent remotely
     */
    public HashMap<Integer, EntityData> getEntityChanges() {
        // TODO: reuse map
        HashMap<Integer, EntityData> changeMap = new HashMap<Integer, EntityData>();

        Iterator<Map.Entry<Integer, Entity>> it = mEntityMap.entrySet()
                .iterator();
        while (it.hasNext()) {
            Map.Entry<Integer, Entity> entry = it.next();
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
            System.err.println("creating char for local connection " + 0);
            ClientState localState = new ClientState(this, LOCAL_CONN, SuperSimpleApplication.sCamera);
            mClientStateMap.put(LOCAL_CONN, localState);

            // Create NPC
            Creature npc = new Creature(0, 40, 10, new ArtificialCreatureIntelligence());
            registerEntity(npc);
        }

        // new connection: create ClientState/Creature
        for (Connection c : ((Server) mEndPoint).getConnections()) {
            if (!mClientStateMap.containsKey(c.getID())) {
                System.err.println("creating char for connection " + c.getID());
                ClientState newClient = new ClientState(this, c.getID(), null);
                mClientStateMap.put(c.getID(), newClient);

                // enqueue all previously modified chunks to the new client
                newClient.enqueueChunks(ChunkModifier.server_getAllModified());
            }
        }
    }

    @Override
    protected void queryDesiredActions(double localTime) {
        updateConnections();

        // receive control messages from clients
        TransmitPair pair;
        for (;;) {
            pair = pollHard(localTime, 0);
            if (pair == null){
                break;
            }

            ClientState remoteClient = mClientStateMap.get(pair.connection.getID());
            if (remoteClient == null) {
                continue;
            }

            if (pair.object instanceof DesiredActionMessage) {
                // Client updates server with state
                DesiredActionMessage state = ((DesiredActionMessage) pair.object);
                remoteClient.mCreature.setControlMessage(state);
            } else if (pair.object instanceof ChatMessage) {
                // Client updates server with state
                ChatMessage state = ((ChatMessage) pair.object);
                remoteClient.mCreature.setChatMessage(state);
            }
        }

        // process character move intent, and chats
        for (Entity e : mEntityMap.values()) {
            if (e instanceof Creature) {
                Creature c = (Creature)e;
                c.queryDesiredAction(localTime);

                processChatMessage(localTime, c.getChat());
            }
        }
    }

    @Override
    public void processAftermath(double localTime) {
        for (Entity e : mEntityMap.values()) {
            if (e instanceof Creature) {
                ((Creature)e).processAftermath();
            }
        }

        // send entity updates to clients
        StateMessage serverState = new StateMessage();
        serverState.timestamp = localTime;
        serverState.data = getEntityChanges();
        sendToAllUDP(serverState);

        // send chunk updates to clients
        ChunkMessage[] recentModified = ChunkModifier.server_getRecentModified();
        for (ChunkMessage message : recentModified) {
            System.err.println("sending chunk " + message.index);
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
