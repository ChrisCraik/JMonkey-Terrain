
package supergame.gui;

import com.jme3.app.Application;
import com.jme3.app.state.AbstractAppState;
import com.jme3.app.state.AppStateManager;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;

import de.lessvoid.nifty.Nifty;
import de.lessvoid.nifty.NiftyEventSubscriber;
import de.lessvoid.nifty.controls.ButtonClickedEvent;
import de.lessvoid.nifty.controls.CheckBoxStateChangedEvent;
import de.lessvoid.nifty.elements.Element;
import de.lessvoid.nifty.elements.render.TextRenderer;
import de.lessvoid.nifty.screen.Screen;
import de.lessvoid.nifty.screen.ScreenController;
import supergame.PhysicsContent;
import supergame.SuperSimpleApplication;
import supergame.network.EntityManager;
import supergame.network.ServerEntityManager;
import supergame.terrain.ChunkManager;
import supergame.terrain.modify.ChunkCastle;
import supergame.terrain.modify.ChunkModifier;

import java.util.logging.Logger;

public class Game extends AbstractAppState implements ScreenController {
    private Nifty mNifty;
    private SuperSimpleApplication mApp;

    private PhysicsContent mPhysicsContent;
    private ChunkManager mChunkManager;
    private EntityManager mEntityManager;
    private double mLocalTime = 0;

    private static Game sInstance;

    public static void registerPhysics(Object obj) {
        sInstance.mPhysicsContent.getRegistrar().registerPhysics(obj);
    }

    // AppState methods

    @Override
    public void initialize(AppStateManager stateManager, Application app) {
        super.initialize(stateManager, app);
        sInstance = this;

        mApp = (SuperSimpleApplication)app;

        Logger.getLogger("").severe("INITIALIZING APP STATE");

        mPhysicsContent = new PhysicsContent(
                mApp.getRootNode(),
                mApp.getAssetManager(),
                mApp.getStateManager());

        Material mat = new Material(mApp.getAssetManager(),
                "Common/MatDefs/Light/Lighting.j3md");
        mat.setBoolean("m_UseMaterialColors", true);
        mat.setColor("m_Ambient", ColorRGBA.Brown);
        mat.setColor("m_Diffuse", ColorRGBA.Brown);
        mChunkManager = new ChunkManager(0, 0, 0, 8, mat,
                mApp.getRootNode(), mPhysicsContent.getRegistrar());
        ChunkCastle.create();
    }

    @Override
    public void update(float tpf) {
        if (mEntityManager != null) {
            mEntityManager.processAftermath(mLocalTime);
        }

        mLocalTime += tpf;

        mChunkManager.updateWithPosition(0, 0, 0);

        Element element = mNifty.getCurrentScreen().findElementByName("LoadingPercentage");
        if (element != null) {
            float completionPercentage = ChunkModifier.getCompletion() * 100;
            String text = String.format("%.2f%%", completionPercentage);
            element.getRenderer(TextRenderer.class).setText(text);
        }

        if (ChunkModifier.isEmpty() && mEntityManager == null) {
            // TODO: choose server vs client
            mEntityManager = new ServerEntityManager();

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
        if (event.isChecked()) {
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
