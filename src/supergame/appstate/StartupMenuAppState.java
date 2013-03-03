
package supergame.appstate;

import com.jme3.app.Application;
import com.jme3.app.state.AbstractAppState;
import com.jme3.app.state.AppStateManager;

import de.lessvoid.nifty.Nifty;
import de.lessvoid.nifty.NiftyEventSubscriber;
import de.lessvoid.nifty.controls.ButtonClickedEvent;
import de.lessvoid.nifty.controls.CheckBoxStateChangedEvent;
import de.lessvoid.nifty.elements.Element;
import de.lessvoid.nifty.elements.render.TextRenderer;
import de.lessvoid.nifty.screen.Screen;
import de.lessvoid.nifty.screen.ScreenController;
import supergame.terrain.modify.ChunkModifier;

public class StartupMenuAppState extends AbstractAppState implements ScreenController {
    private AppStateManager mStateManager;
    private Application mApp;
    private Nifty mNifty;
    private boolean mIsHosting;

    ///////////////////////////////////////////////////////////////////////////
    // AppState methods
    ///////////////////////////////////////////////////////////////////////////

    @Override public void initialize(AppStateManager stateManager, Application app) {
        mStateManager = stateManager;
        mApp = app;

        mApp.getInputManager().setCursorVisible(true);
    }

    @Override
    public void cleanup() {
        mApp.getInputManager().setCursorVisible(false);
    }

    @Override
    public void update(float tpf) {
        if (mNifty == null) return;

        // TODO: use nifty's "${CALL.method()} to query directly"
        Element element = mNifty.getCurrentScreen().findElementByName("LoadingPercentage");
        if (element != null) {
            float completionPercentage = ChunkModifier.getCompletion() * 100;
            String text = String.format("%.2f%%", completionPercentage);
            element.getRenderer(TextRenderer.class).setText(text);
        }

        NetworkAppState networkAppState = mStateManager.getState(NetworkAppState.class);
        if (ChunkModifier.isEmpty() && networkAppState == null) {
            System.out.println("starting up, hosting = " + mIsHosting);
            mStateManager.attach(new NetworkAppState(mIsHosting ?
                    NetworkAppState.TYPE_SERVER : NetworkAppState.TYPE_CLIENT));
            mNifty.gotoScreen("end");
        }
        // FIXME: remove this appstate when animation completes
    }

    ///////////////////////////////////////////////////////////////////////////
    // Nifty/Screen methods
    ///////////////////////////////////////////////////////////////////////////

    @Override
    public void bind(Nifty nifty, Screen screen) {
        System.out.println("StartupMenu binding nifty");
        mNifty = nifty;
    }

    @Override
    public void onEndScreen() {
    }

    @Override
    public void onStartScreen() {
    }

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
