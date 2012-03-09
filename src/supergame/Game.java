package supergame;

import com.jme3.app.SimpleApplication;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Box;

import supergame.modify.ChunkCastle;

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

    private void makeBox(Material mat, float mass, float size, Vector3f translate) {
        Box box = new Box(Vector3f.ZERO, size, size, size);
        Geometry g = new Geometry("box", box);
        g.setMaterial(mat);
        rootNode.attachChild(g);

        g.setLocalTranslation(translate);
        RigidBodyControl control = new RigidBodyControl(mass);
        g.addControl(control);

        mBulletAppState.getPhysicsSpace().add(control);
    }

    public abstract class PhysicsRegistrar {
        public abstract void registerPhysics(Geometry geom);
    };

    private void setupPhysics() {
        Material mat = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
        mat.setBoolean("m_UseMaterialColors", true);
        mat.setColor("m_Ambient",  ColorRGBA.DarkGray);
        mat.setColor("m_Diffuse",  ColorRGBA.DarkGray);

        mBulletAppState = new BulletAppState();
        stateManager.attach(mBulletAppState);

        makeBox(mat, 0, 15, new Vector3f(0, 0, 0));
        makeBox(mat, 1, 2, new Vector3f(0, 30, 0));
        makeBox(mat, 1, 2, new Vector3f(0, 34, 0));
    }

    @Override
    public void simpleInitApp() {
        flyCam.setMoveSpeed(100);


        setupPhysics();

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

        PhysicsRegistrar registrar = new PhysicsRegistrar() {
            @Override
            public void registerPhysics(Geometry geom) {
                mBulletAppState.getPhysicsSpace().add(geom);
            }
        };

        mChunkManager = new ChunkManager(0, 0, 0, 6, mat, terrainNode, registrar);
        ChunkCastle.create();
        //ChunkModifier.setServerMode(true, mChunkManager);
    }

    @Override
    public void simpleUpdate(float tpf) {
        mChunkManager.updateWithPosition(0, 0, 0);
        mChunkManager.renderChunks();
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