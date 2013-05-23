package supergame.appstate;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.AppStateManager;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.material.Material;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Box;

import supergame.application.MaterialManager;

public class PhysicsAppState extends BulletAppState {
    @Override
    public void initialize(AppStateManager stateManager, Application app) {
        Material mat = MaterialManager.getBoxMaterial();
        System.out.println("app is " + app);

        Node root = ((SimpleApplication)app).getRootNode();

        makeBox(root, mat, 0, 15, new Vector3f(0, 0, 0));
        makeBox(root, mat, 1, 2, new Vector3f(0, 30, 0));
        makeBox(root, mat, 1, 2, new Vector3f(0, 34, 0));
    }

    private void makeBox(Node root, Material mat, float mass, float size, Vector3f translate) {
        Box box = new Box(Vector3f.ZERO, size, size, size);
        Geometry g = new Geometry("box", box);
        g.setMaterial(mat);
        g.setShadowMode(RenderQueue.ShadowMode.CastAndReceive);
        root.attachChild(g);

        g.setLocalTranslation(translate);
        RigidBodyControl control = new RigidBodyControl(mass);
        g.addControl(control);

        getPhysicsSpace().add(control);
    }
}
