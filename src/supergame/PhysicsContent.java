package supergame;

import com.jme3.app.state.AppStateManager;
import com.jme3.asset.AssetManager;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue.ShadowMode;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Box;

@Deprecated
public class PhysicsContent {
    private final BulletAppState mBulletAppState;
    private final Node mRootNode;
    private final PhysicsRegistrar mRegistrar;


    public abstract class PhysicsRegistrar {
        public abstract void registerPhysics(Object obj);
        public abstract void unregisterPhysics(Object obj);
    }

    public PhysicsRegistrar getRegistrar() { return mRegistrar; }

    public PhysicsContent(Node rootNode, AssetManager assetManager, AppStateManager stateManager) {
        mRootNode = rootNode;
        Material mat = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
        mat.setBoolean("UseMaterialColors", true);
        mat.setColor("Ambient",  ColorRGBA.DarkGray);
        mat.setColor("Diffuse",  ColorRGBA.DarkGray);

        mBulletAppState = new BulletAppState();
        stateManager.attach(mBulletAppState);

        makeBox(mat, 0, 15, new Vector3f(0, 0, 0));
        makeBox(mat, 1, 2, new Vector3f(0, 30, 0));
        makeBox(mat, 1, 2, new Vector3f(0, 34, 0));

        DirectionalLight sun = new DirectionalLight();
        sun.setColor(ColorRGBA.White);
        sun.setDirection(new Vector3f(-2, -1, -2).normalizeLocal());
        rootNode.addLight(sun);

        DirectionalLight sun2 = new DirectionalLight();
        sun2.setColor(ColorRGBA.White);
        sun2.setDirection(new Vector3f(2, -1, -2).normalizeLocal());
        rootNode.addLight(sun2);

        mRegistrar = new PhysicsRegistrar() {
            @Override
            public void registerPhysics(Object obj) {
                mBulletAppState.getPhysicsSpace().add(obj);
            }
            @Override
            public void unregisterPhysics(Object obj) {
                mBulletAppState.getPhysicsSpace().remove(obj);
            }
        };
    }

    private void makeBox(Material mat, float mass, float size, Vector3f translate) {
        Box box = new Box(Vector3f.ZERO, size, size, size);
        Geometry g = new Geometry("box", box);
        g.setMaterial(mat);
        g.setShadowMode(ShadowMode.CastAndReceive);
        mRootNode.attachChild(g);

        g.setLocalTranslation(translate);
        RigidBodyControl control = new RigidBodyControl(mass);
        g.addControl(control);

        mBulletAppState.getPhysicsSpace().add(control);
    }

}
