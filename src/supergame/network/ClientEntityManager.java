
package supergame.network;

import com.esotericsoftware.kryonet.Client;

import com.jme3.app.SimpleApplication;
import supergame.Config;
import supergame.appstate.NetworkAppState;
import supergame.character.Character;
import supergame.character.Creature;
import supergame.network.Structs.ChatMessage;
import supergame.network.Structs.ChunkMessage;
import supergame.network.Structs.Entity;
import supergame.network.Structs.EntityData;
import supergame.network.Structs.StartMessage;
import supergame.network.Structs.StateMessage;
import supergame.terrain.modify.ChunkModifier;
import supergame.utils.Log;

import java.io.IOException;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ClientEntityManager extends EntityManager {
    private static Logger sLog = Log.getLogger(ClientEntityManager.class.getName(), Level.ALL);

    /**
     * Normal ClientEntityManager constructor.
     */
    public ClientEntityManager() {
        super(new Client(Config.WRITE_BUFFER_SIZE, Config.OBJECT_BUFFER_SIZE), null, null);
    }

    /**
     * ClientEntityManager constructor that also saves received packets for future playback.
     *
     * @param w The client stores received packets from server in this.
     */
    public ClientEntityManager(WritableByteChannel w) {
        super(new Client(Config.WRITE_BUFFER_SIZE, Config.OBJECT_BUFFER_SIZE), w, null);
        throw new UnsupportedOperationException();
    }

    /**
     * 'Virtual' ClientEntityManager that connects to a BufferedReader instead of a
     * server, for playing back saved network transcripts.
     *
     * @param r Read by the client as though a stream of packets from the server.
     */
    public ClientEntityManager(ReadableByteChannel r) {
        super(null, null, r);
        throw new UnsupportedOperationException();
    }

    /**
     * Apply remote changes client side
     *
     * @param timestamp
     * @param changeMap
     */
    public void applyEntityChanges(double timestamp,
            HashMap<Integer, EntityData> changeMap) {
        for (Map.Entry<Integer, EntityData> entry : changeMap.entrySet()) {
            int id = entry.getKey();
            EntityData data = entry.getValue();

            Entity localEntity = mEntityMap.get(id);
            if (localEntity != null) {
                localEntity.apply(timestamp, data);
            } else {
                // NEW ENTITY
                assert (mPacketToClassMap.containsKey(data.getClass()));
                Class<? extends Entity> entityClass = mPacketToClassMap.get(data.getClass());
                try {
                    Entity newEntity = entityClass.newInstance();
                    registerEntity(newEntity, id);
                    newEntity.apply(timestamp, data);
                } catch (InstantiationException e) {
                    sLog.severe("ERROR: class lacks no-arg constructor");
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    sLog.severe("ERROR: class lacks public constructor");
                    e.printStackTrace();
                }
            }
        }
    }

    public void connect(int timeout, String address, int tcp, int udp) throws IOException {
        sLog.info("connecting to server...");
        if (mEndPoint != null) {
            mEndPoint.start(); // there's probably a reason not to do this here...
            ((Client) mEndPoint).connect(timeout, address, tcp, udp);
        }
    }

    /**
     * Accounts for difference between local and server absolute timestamps
     */
    private static double sClockCorrection = Double.MAX_VALUE;

    /**
     * Returns lerp sample point in server time space
     * <p>
     * It's important to use this and only this for lerping timestamps from the server, as there
     * can be a significant difference between server and local time
     */
    public static double getLerpTime() {
        return NetworkAppState.getLocalNetworkTime() +
                sClockCorrection - (Config.SAMPLE_DELAY_MS / 1000);
    }

    @Override
    public void update(SimpleApplication app) {
        sendControl();
        receiveUpdates(app);
    }

    private void sendControl() {
        // send control info to server
        if (mEntityMap.containsKey(mLocalCharId)) {
            Character localCharacter = (Character)mEntityMap.get(mLocalCharId);
            Structs.Intent intent = localCharacter.getIntent();
            ((Client)mEndPoint).sendUDP(intent);
            // TODO: send chat
            /*
            Creature localCreature = (Creature)mEntityMap.get(mLocalCharId);
            ((Client)mEndPoint).sendUDP(localCreature.getControl());
            ChatMessage chat = localCreature.getChat();
            if (chat != null && chat.s != null) {
                System.err.println("Sending chat to server:" + chat.s);
                ((Client)mEndPoint).sendTCP(chat);
                chat.s = null;
            }
            */
        }
    }

    private void receiveUpdates(SimpleApplication app) {
        double localTime = NetworkAppState.getLocalNetworkTime();
        // receive world state from server
        TransmitPair pair;
        for (;;) {
            pair = pollHard(localTime, 0);
            if (pair == null) break;

            if (pair.object instanceof StateMessage) {
                // ignore entities until connection confirmed, local char created
                if (mEntityMap.isEmpty()) {
                    if (mLocalCharId == INVALID_CHARACTER_ID) continue;
                    mEntityMap.put(mLocalCharId, new Character(Character.CLIENT_LOCAL));
                }

                // Server updates client with state of all entities
                StateMessage state = (StateMessage) pair.object;
                applyEntityChanges(state.timestamp, state.data);

                // update clock correction based on packet timestamp, arrival time
                if (sClockCorrection == Double.MAX_VALUE) {
                    sClockCorrection = state.timestamp - localTime;
                } else {
                    sClockCorrection = Config.CORRECTION_WEIGHT * (state.timestamp - localTime)
                            + (1 - Config.CORRECTION_WEIGHT) * sClockCorrection;
                }
            } else if (pair.object instanceof StartMessage) {
                // Server tells client which character the player controls
                mLocalCharId = ((StartMessage) pair.object).characterEntity;
            } else if (pair.object instanceof ChatMessage) {
                //TODO: chat display
                ChatMessage chat = (ChatMessage) pair.object;
                System.out.println("chat received " + chat.s);
            } else if (pair.object instanceof ChunkMessage) {
                ChunkMessage chunkMessage = (ChunkMessage) pair.object;
                ChunkModifier.client_putModified(chunkMessage);
            }
        }
    }

    @Override
    @Deprecated
    protected void queryDesiredActions(double localTime) {
        localTime = NetworkAppState.getLocalNetworkTime();

        sendControl();
        receiveUpdates(null);

        // move local char
        if (mEntityMap.containsKey(mLocalCharId)) {
            Creature localChar = (Creature)mEntityMap.get(mLocalCharId);
            localChar.queryDesiredAction(localTime);
        }
    }

    @Override
    @Deprecated
    public void processAftermath(double localTime) {
        // for each character, sample interpolation window
        for (Integer key : mEntityMap.keySet()) {
            Entity value = mEntityMap.get(key);
            if (value instanceof Creature) {
                float bias = (key == mLocalCharId) ? 1.0f : 0.5f;
                Creature c = (Creature)value;
                c.sample(localTime + sClockCorrection - (Config.SAMPLE_DELAY_MS / 1000), bias);
            }
        }

        // update controller with local position
        if (mEntityMap.containsKey(mLocalCharId)) {
            Creature localChar = (Creature)mEntityMap.get(mLocalCharId);
            localChar.processAftermath();
        }
    }
}
