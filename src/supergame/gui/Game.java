
package supergame.gui;

import com.jme3.app.Application;
import com.jme3.app.state.AbstractAppState;
import com.jme3.app.state.AppStateManager;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;

import de.lessvoid.nifty.Nifty;
import de.lessvoid.nifty.NiftyEventSubscriber;
import de.lessvoid.nifty.controls.ButtonClickedEvent;
import de.lessvoid.nifty.controls.CheckBoxStateChangedEvent;
import de.lessvoid.nifty.elements.Element;
import de.lessvoid.nifty.elements.render.TextRenderer;
import de.lessvoid.nifty.screen.Screen;
import de.lessvoid.nifty.screen.ScreenController;
import supergame.Config;
import supergame.PhysicsContent;
import supergame.SuperSimpleApplication;
import supergame.network.ClientEntityManager;
import supergame.network.EntityManager;
import supergame.network.ServerEntityManager;
import supergame.terrain.ChunkManager;
import supergame.terrain.modify.ChunkCastle;
import supergame.terrain.modify.ChunkModifier;
import supergame.utils.Log;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Game extends AbstractAppState implements ScreenController {
    private static Logger sLog = Log.getLogger(Game.class.getName(), Level.ALL);

    private Nifty mNifty;
    private SuperSimpleApplication mApp;

    private PhysicsContent mPhysicsContent;
    private ChunkManager mChunkManager;
    private EntityManager mEntityManager;
    private double mLocalTime = 0;
    private boolean mIsHosting = true; // default must match startup.xml definition

    private static Game sInstance;

    // TODO: clean this up, redesign class hierarchy so event signals trickle
    // down as needed, or use event callbacks in the Game instance
    public static void closeConnections() {
        if (sInstance != null && sInstance.mEntityManager != null) {
            sInstance.mEntityManager.close();
        }
    }

    public static void registerPhysics(Object obj) {
        sInstance.mPhysicsContent.getRegistrar().registerPhysics(obj);
    }

    private static Material sCreatureMaterial = null;
    public static Material getCharacterMaterial() {
        if (sCreatureMaterial == null) {
            sCreatureMaterial = new Material(sInstance.mApp.getAssetManager(),
                    "Common/MatDefs/Light/Lighting.j3md");
            sCreatureMaterial.setBoolean("UseMaterialColors", true);
            sCreatureMaterial.setColor("Ambient", ColorRGBA.Green);
            sCreatureMaterial.setColor("Diffuse", ColorRGBA.Green);
        }
        return sCreatureMaterial;
    }

    private static Node sCreatureRoot = null;
    public static void addCreatureGeometry(Geometry creatureGeomNode) {
        if (sCreatureRoot == null) {
            sCreatureRoot = new Node("creature root");
            sInstance.mApp.getRootNode().attachChild(sCreatureRoot);
        }

        sCreatureRoot.attachChild(creatureGeomNode);
    }

    // AppState methods

    @Override
    public void initialize(AppStateManager stateManager, Application app) {
        super.initialize(stateManager, app);
        sInstance = this;

        mApp = (SuperSimpleApplication)app;

        sLog.severe("INITIALIZING APP STATE");

        mPhysicsContent = new PhysicsContent(
                mApp.getRootNode(),
                mApp.getAssetManager(),
                mApp.getStateManager());

        Material mat = new Material(mApp.getAssetManager(),
                "Common/MatDefs/Light/Lighting.j3md");
        mat.setBoolean("UseMaterialColors", true);
        mat.setColor("Ambient", ColorRGBA.Brown);
        mat.setColor("Diffuse", ColorRGBA.Brown);
        mChunkManager = new ChunkManager(0, 0, 0, Config.CHUNK_LOAD_DISTANCE,
                mat, mApp.getRootNode(), mPhysicsContent.getRegistrar());
        ChunkCastle.create();
    }

    @Override
    public void update(float tpf) {
        if (mEntityManager != null) {
            mEntityManager.processAftermath(mLocalTime);
        }

        mLocalTime += tpf;

        mChunkManager.updateWithPosition(0, 0, 0);

        // TODO: use nifty's "${CALL.method()} to query directly"
        Element element = mNifty.getCurrentScreen().findElementByName("LoadingPercentage");
        if (element != null) {
            float completionPercentage = ChunkModifier.getCompletion() * 100;
            String text = String.format("%.2f%%", completionPercentage);
            element.getRenderer(TextRenderer.class).setText(text);
        }

        if (ChunkModifier.isEmpty() && mEntityManager == null) {
            sLog.severe("starting up, hosting = " + mIsHosting);

            if (mIsHosting) {
                ServerEntityManager manager = new ServerEntityManager();
                try {
                    manager.bind(1432, 1432);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                mEntityManager = manager;
            } else {
                ClientEntityManager manager = new ClientEntityManager();
                try {
                    // TODO: query port and host addr correctly
                    manager.connect(Config.CONNECT_TIMEOUT_MS, "localhost", 1432, 1432);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                mEntityManager = manager;
            }

            mNifty.gotoScreen("end");
            mApp.setMouseMenuMode(false);

            boolean isServer = mEntityManager instanceof ServerEntityManager;
            ChunkModifier.setServerMode(isServer, mChunkManager);
        }

        if (mEntityManager != null) {
            mEntityManager.queryAndProcessIntents(mLocalTime);
        }
    }

    @Override
    public void render(com.jme3.renderer.RenderManager rm) {
    }

    // ScreenController methods

    @Override
    public void bind(Nifty nifty, Screen screen) {
        mNifty = nifty;
    }

    @Override
    public void onEndScreen() {
    }

    @Override
    public void onStartScreen() {
    }

    // ScreenController event handlers

    @NiftyEventSubscriber(id = "HostingCheckBox")
    public void onAllCheckBoxChanged(final String id, final CheckBoxStateChangedEvent event) {
        Element element = mNifty.getCurrentScreen().findElementByName("ServerPickerPanel");
        mIsHosting = event.isChecked();
        if (mIsHosting) {
            element.hide();
        } else {
            element.show();
        }
    }

    @NiftyEventSubscriber(id = "StartButton")
    public void onStartButtonClicked(final String id, final ButtonClickedEvent event) {
        System.out.println("loading now!");
        mNifty.gotoScreen("load");
    }
}
