package supergame.appstate;

import com.jme3.app.Application;
import com.jme3.app.state.AbstractAppState;
import com.jme3.app.state.AppStateManager;

import supergame.character.CharacterFactory;

public class NetworkAppState extends AbstractAppState {
    public static final int TYPE_SERVER = 0;
    public static final int TYPE_CLIENT = 1;
    private final boolean mIsServer;

    public NetworkAppState(int type) {
        mIsServer = (type == TYPE_SERVER);
    }

    @Override
    public void initialize(AppStateManager stateManager, Application app) {
        //in initialize, create entityManager and attach
        //TODO: try manager.bind(1432, 1432);
        //TODO: try manager.connect(Config.CONNECT_TIMEOUT_MS, "localhost", 1432, 1432);
        if (mIsServer) {
            System.out.println("creating player and ai");

            CharacterFactory.createPlayer(app);
            CharacterFactory.createAi(app);
        }
    }

    @Override
    public void cleanup() {
        //TODO: closeConnections
    }

    @Override
    public void update(float tpf) {
        // if a server, query all creatures' states, send

        // if a client, copy out the Intents from all creatures

        // either way, recieve relevant data
    }
}
