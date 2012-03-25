package supergame;

import com.jme3.app.SimpleApplication;
import com.jme3.bullet.BulletAppState;
import com.jme3.font.BitmapText;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;

import supergame.character.Controller;
import supergame.modify.ChunkCastle;
import supergame.modify.ChunkModifier;
import supergame.network.GameEndPoint;
import supergame.network.GameServer;

public class Game extends SimpleApplication {
    ChunkManager mChunkManager;
    BulletAppState mBulletAppState;

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
    private PhysicsContent mPhysicsContent;

    @Override
    public void simpleInitApp() {
        mCamera = new CameraController(cam);
        flyCam.setMoveSpeed(100);

        mPhysicsContent = new PhysicsContent(rootNode, assetManager, stateManager);

        Material matWireframe = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        matWireframe.getAdditionalRenderState().setWireframe(true);

        Material mat = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
        mat.setBoolean("m_UseMaterialColors", true);
        mat.setColor("m_Ambient",  ColorRGBA.Brown);
        mat.setColor("m_Diffuse",  ColorRGBA.Brown);

        viewPort.setBackgroundColor(new ColorRGBA(0.5f, 0.5f, 1, 1));

        DirectionalLight sun = new DirectionalLight();
        sun.setColor(ColorRGBA.White);
        sun.setDirection(new Vector3f(-2, -1, -2).normalizeLocal());
        rootNode.addLight(sun);

        DirectionalLight sun2 = new DirectionalLight();
        sun2.setColor(ColorRGBA.White);
        sun2.setDirection(new Vector3f(2, -1, -2).normalizeLocal());
        rootNode.addLight(sun2);

        Node terrainNode = new Node("terrain");
        rootNode.attachChild(terrainNode);

        mChunkManager = new ChunkManager(0, 0, 0, 6, mat, terrainNode, mPhysicsContent.getRegistrar());
        ChunkCastle.create();
    }

    private BitmapText mStatusText = null;
    private GameEndPoint mGameEndPoint = null;

    private static float sTpf;
    public static float tpf() {
        return sTpf;
    }

    @Override
    public void simpleUpdate(float tpf) {
        sTpf = tpf;
        mChunkManager.updateWithPosition(0, 0, 0);
        mChunkManager.renderChunks();


        if (!ChunkModifier.isEmpty()) {
            if (mStatusText == null) {
                mStatusText = new BitmapText(guiFont, false);
                mStatusText.setSize(guiFont.getCharSet().getRenderedSize());
                mStatusText.setText("Completing chunk generation...");
                mStatusText.setLocalTranslation(300, mStatusText.getLineHeight(), 0);
            }
            if (mStatusText.getParent() == null) {
                guiNode.attachChild(mStatusText);
            }
        } else {
            if (mStatusText != null && mStatusText.getParent() != null) {
                mStatusText.removeFromParent();
            }
            if (mGameEndPoint == null) {
                mGameEndPoint = new GameServer();
            }
        }
    }

    private static boolean mRunning = true;
    public static boolean isRunning() { return mRunning; }

    @Override
    public void stop() {
        mRunning = false;
        super.stop();
    }

    public static void main(String[] args){
        Game app = new Game();
        app.start();
    }
}