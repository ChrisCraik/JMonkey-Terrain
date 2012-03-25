package supergame;

import com.jme3.app.SimpleApplication;
import com.jme3.asset.plugins.FileLocator;
import com.jme3.math.ColorRGBA;
import com.jme3.niftygui.NiftyJmeDisplay;

import de.lessvoid.nifty.Nifty;
import supergame.character.Controller;
import supergame.gui.Game;

import java.util.logging.Logger;

public class SuperSimpleApplication extends SimpleApplication {
    private static long getTimeMillis() {
        //return (Sys.getTime() * 1000) / Sys.getTimerResolution();
        return 0;
    }

    private static long mLastProfMillis = 0;
    public static void PROFILE(String label) {
        long tempTime = getTimeMillis();
        if (tempTime - mLastProfMillis > 20)
            System.out.printf("%20s: %d\n", label + " took", (tempTime - mLastProfMillis));
        mLastProfMillis = tempTime;
    }

    //TODO: rename to cameracontroller
    public static Controller mCamera = null;

    @Override
    public void simpleInitApp() {
        Logger.getLogger("").setLevel(Config.LOG_LEVEL);
        assetManager.registerLocator( "./assets/", FileLocator.class.getName() );
        mCamera = new CameraController(cam);

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

        // disable the fly cam
        flyCam.setEnabled(false);
        inputManager.setCursorVisible(true);
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
        app.start();
    }
}