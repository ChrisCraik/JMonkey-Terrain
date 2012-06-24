package supergame;

import com.jme3.app.SimpleApplication;
import com.jme3.asset.plugins.FileLocator;
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.input.controls.MouseAxisTrigger;
import com.jme3.input.controls.MouseButtonTrigger;
import com.jme3.math.ColorRGBA;
import com.jme3.niftygui.NiftyJmeDisplay;
import com.jme3.system.AppSettings;

import de.lessvoid.nifty.Nifty;
import supergame.character.CreatureIntelligence;
import supergame.character.LocalPlayerCreatureIntelligence;
import supergame.gui.Game;

import java.util.logging.Logger;

public class SuperSimpleApplication extends SimpleApplication {

    //TODO: rename to cameracontroller
    public static CreatureIntelligence sCamera = null;

    public void setMouseMenuMode(boolean menuMode) {
        flyCam.setEnabled(!menuMode);
        inputManager.setCursorVisible(menuMode);
    }

    private void initInputMappings() {
        inputManager.addMapping("left", new KeyTrigger(KeyInput.KEY_D));
        inputManager.addMapping("right", new KeyTrigger(KeyInput.KEY_A));
        inputManager.addMapping("forward", new KeyTrigger(KeyInput.KEY_W));
        inputManager.addMapping("back", new KeyTrigger(KeyInput.KEY_S));
        inputManager.addMapping("jump", new KeyTrigger(KeyInput.KEY_SPACE));

        inputManager.addMapping("target-forward", new KeyTrigger(KeyInput.KEY_UP));
        inputManager.addMapping("target-back", new KeyTrigger(KeyInput.KEY_DOWN));
        inputManager.addMapping("target-forward", new MouseAxisTrigger(MouseInput.AXIS_WHEEL, false));
        inputManager.addMapping("target-back", new MouseAxisTrigger(MouseInput.AXIS_WHEEL, true));

        inputManager.addMapping("tool-main", new MouseButtonTrigger(MouseInput.BUTTON_LEFT));
        inputManager.addMapping("tool-secondary", new MouseButtonTrigger(MouseInput.BUTTON_RIGHT));

        inputManager.addMapping("select-tool-0", new KeyTrigger(KeyInput.KEY_0));
        inputManager.addMapping("select-tool-1", new KeyTrigger(KeyInput.KEY_1));
        inputManager.addMapping("select-tool-2", new KeyTrigger(KeyInput.KEY_2));
        inputManager.addMapping("select-tool-3", new KeyTrigger(KeyInput.KEY_3));
    }

    @Override
    public void simpleInitApp() {
        Logger.getLogger("").setLevel(Config.LOG_LEVEL);
        assetManager.registerLocator("./assets/", FileLocator.class);
        initInputMappings();
        sCamera = new LocalPlayerCreatureIntelligence(cam, inputManager);

        viewPort.setBackgroundColor(new ColorRGBA(0.5f, 0.5f, 1, 1));

        NiftyJmeDisplay niftyDisplay = new NiftyJmeDisplay(assetManager,
                inputManager,
                audioRenderer,
                guiViewPort);
        Nifty nifty = niftyDisplay.getNifty();
        Game game = new Game();
        nifty.fromXml("Interface/Nifty/startup.xml", "start", game);
        guiViewPort.addProcessor(niftyDisplay);

        stateManager.attach(game);

        setMouseMenuMode(true);
    }

    private static float sTpf;
    public static float tpf() {
        return sTpf;
    }

    @Override
    public void simpleUpdate(float tpf) {
        sTpf = tpf;
    }

    private static boolean mRunning = true;
    public static boolean isRunning() { return mRunning; }

    @Override
    public void stop() {
        mRunning = false;
        super.stop();
    }

    public static void main(String[] args){
        SuperSimpleApplication app = new SuperSimpleApplication();
        AppSettings settings = new AppSettings(true);
        settings.setFrameRate(Config.FRAME_RATE_CAP);
        app.setSettings(settings);
        app.start();
    }
}