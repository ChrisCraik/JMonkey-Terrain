package supergame.appstate;

import com.jme3.app.Application;
import com.jme3.app.state.AbstractAppState;
import com.jme3.app.state.AppStateManager;

import supergame.Config;
import supergame.application.VerySimpleApplication;
import supergame.network.ClientEntityManager;
import supergame.network.EntityManager;
import supergame.network.ServerEntityManager;

import java.io.IOException;

public class NetworkAppState extends AbstractAppState {
    public static final int TYPE_SERVER = 0;
    public static final int TYPE_CLIENT = 1;
    public static double sLocalNetworkTime = 0;
    public static double getLocalNetworkTime() { return sLocalNetworkTime; }

    private final boolean mIsServer;
    private EntityManager mEntityManager;

    public NetworkAppState(int type) {
        mIsServer = (type == TYPE_SERVER);
    }

    @Override
    public void initialize(AppStateManager stateManager, Application app) {
        stateManager.getState(ChunkAppState.class).setServerMode(mIsServer);
        if (mIsServer) {
            ServerEntityManager manager = new ServerEntityManager();
            try {
                manager.bind(1432, 1432);
            } catch (IOException e) {
                e.printStackTrace();
            }
            mEntityManager = manager;
            //new Character(Character.SERVER_LOCAL);
            //new Character(Character.SERVER_AI);
        } else {
            ClientEntityManager manager = new ClientEntityManager();
            try {
                // TODO: query port and host address correctly
                manager.connect(Config.CONNECT_TIMEOUT_MS, "localhost", 1432, 1432);
            } catch (IOException e) {
                e.printStackTrace();
            }
            mEntityManager = manager;
        }
    }

    @Override
    public void cleanup() {
        if (mEntityManager != null) {
            mEntityManager.close();
        }
    }

    @Override
    public void update(float tpf) {
        if (mEntityManager != null) {
            mEntityManager.update(VerySimpleApplication.getInstance());
        }

        sLocalNetworkTime += tpf;
        // if a server, query all creatures' states, send

        // if a client, copy out the Intents from all creatures

        // either way, recieve relevant data
    }
}
