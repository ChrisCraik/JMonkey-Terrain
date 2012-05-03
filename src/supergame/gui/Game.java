
package supergame.gui;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
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
import supergame.CameraController;
import supergame.ChunkManager;
import supergame.PhysicsContent;
import supergame.character.Controller;
import supergame.modify.ChunkCastle;
import supergame.modify.ChunkModifier;
import supergame.network.GameEndPoint;
import supergame.network.GameServer;

import java.util.logging.Logger;

public class Game extends AbstractAppState implements ScreenController {
    private Nifty mNifty;
    private SimpleApplication mApp;

    private PhysicsContent mPhysicsContent;
    private ChunkManager mChunkManager;
    private GameEndPoint mGameEndPoint;

    public static Controller sCamera = null;

    // AppState methods

    @Override
    public void initialize(AppStateManager stateManager, Application app) {
        super.initialize(stateManager, app);
        mApp=(SimpleApplication)app;

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

        sCamera = new CameraController(mApp.getCamera());
    }

    @Override
    public void update(float tpf) {
        mChunkManager.updateWithPosition(0, 0, 0);
        mChunkManager.renderChunks();

        float completionPercentage = ChunkModifier.getCompletion() * 100;
        Element element = mNifty.getCurrentScreen().findElementByName("LoadingPercentage");
        if (element != null) {
            String text = String.format("%.2f%%", completionPercentage);
            element.getRenderer(TextRenderer.class).setText(text);
        }

        if (ChunkModifier.isEmpty() && mGameEndPoint == null) {
            mGameEndPoint = new GameServer();
            mNifty.gotoScreen("end");
            // TODO: spawn character
        }
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
        if (event.isChecked())
            element.hide();
        else
            element.show();
    }

    @NiftyEventSubscriber(id = "StartButton")
    public void onStartButtonClicked(final String id, final ButtonClickedEvent event) {
        System.out.println("loading now!");
        mNifty.gotoScreen("load");
    }
}
