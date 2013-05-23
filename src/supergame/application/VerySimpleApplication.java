package supergame.application;

import com.jme3.app.SimpleApplication;
import com.jme3.asset.plugins.FileLocator;
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.input.controls.MouseAxisTrigger;
import com.jme3.input.controls.MouseButtonTrigger;
import com.jme3.light.DirectionalLight;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.niftygui.NiftyJmeDisplay;
import com.jme3.system.AppSettings;

import de.lessvoid.nifty.Nifty;
import supergame.Config;
import supergame.appstate.ChunkAppState;
import supergame.appstate.PhysicsAppState;
import supergame.appstate.StartupMenuAppState;
import supergame.terrain.modify.ChunkCastle;

import java.util.logging.Level;
import java.util.logging.Logger;

public class VerySimpleApplication extends SimpleApplication {

    private void initInputMappings() {
        inputManager.addMapping("left", new KeyTrigger(KeyInput.KEY_D));
        inputManager.addMapping("right", new KeyTrigger(KeyInput.KEY_A));
        inputManager.addMapping("forward", new KeyTrigger(KeyInput.KEY_W));
        inputManager.addMapping("back", new KeyTrigger(KeyInput.KEY_S));

        inputManager.addMapping("jump", new KeyTrigger(KeyInput.KEY_SPACE));
        inputManager.addMapping("duck", new KeyTrigger(KeyInput.KEY_LSHIFT));
        inputManager.addMapping("sprint", new KeyTrigger(KeyInput.KEY_LCONTROL));

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
        flyCam.setEnabled(false);
        Logger.getLogger("").setLevel(Level.WARNING);
        assetManager.registerLocator("./assets/", FileLocator.class);
        MaterialManager.init(assetManager);
        initInputMappings();

        DirectionalLight sun = new DirectionalLight();
        sun.setColor(ColorRGBA.White);
        sun.setDirection(new Vector3f(-2, -1, -2).normalizeLocal());
        rootNode.addLight(sun);

        DirectionalLight sun2 = new DirectionalLight();
        sun2.setColor(ColorRGBA.White);
        sun2.setDirection(new Vector3f(2, -1, -2).normalizeLocal());
        rootNode.addLight(sun2);

        // initialize basic drawing (color, shadows)
        viewPort.setBackgroundColor(new ColorRGBA(0.5f, 0.5f, 1, 1));
        /* Doesn't yet work - needs tuning
        FilterPostProcessor fpp = new FilterPostProcessor(assetManager);
        PssmShadowFilter pssmShadowFilter = new PssmShadowFilter(assetManager, 1024*2, 3);
        pssmShadowFilter.setDirection(new Vector3f(-.5f, -.6f, -.7f).normalizeLocal());
        fpp.addFilter(pssmShadowFilter);
        viewPort.addProcessor(fpp);
        */

        NiftyJmeDisplay niftyDisplay = new NiftyJmeDisplay(assetManager,
                inputManager, audioRenderer, guiViewPort);
        Nifty nifty = niftyDisplay.getNifty();
        StartupMenuAppState startup = new StartupMenuAppState();
        nifty.fromXml("Interface/Nifty/startup.xml", "start", startup);
        guiViewPort.addProcessor(niftyDisplay);

        stateManager.attach(startup);
        stateManager.attach(new PhysicsAppState());
        stateManager.attach(new ChunkAppState());

        ChunkCastle.create();
    }

    public void setMenuMode(boolean enable) {
        flyCam.setEnabled(!enable);
        inputManager.setCursorVisible(enable);
    }

    // TODO: better method for frame time tracking
    private static float sTpf;
    public static float tpf() {
        return sTpf;
    }

    @Override
    public void simpleUpdate(float tpf) {
        super.simpleUpdate(tpf);
        sTpf = tpf;
    }

    public static void main(String[] args){
        VerySimpleApplication app = new VerySimpleApplication();
        AppSettings settings = new AppSettings(true);
        settings.setFrameRate(Config.FRAME_RATE_CAP);
        settings.setResolution(1280, 720);
        settings.setSamples(0);
        app.setSettings(settings);
        app.setPauseOnLostFocus(false);
        app.start();
    }
}
