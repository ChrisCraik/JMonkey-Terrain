package supergame.control.server;

import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.AbstractControl;
import com.jme3.scene.control.Control;

import supergame.network.Structs.Intent;

public class AiProduceIntentControl extends AbstractControl {

    final private Intent mIntent;
    public AiProduceIntentControl(Intent intent) {
        mIntent = intent;
    }

    @Override
    public Control cloneForSpatial(Spatial arg0) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void controlRender(RenderManager renderManager, ViewPort viewPort) {
        // TODO
    }

    float time = 0;
    @Override
    protected void controlUpdate(float tpf) {
        time += tpf;
        mIntent.x = (float) Math.sin(time) / 8;
        mIntent.z = (float) Math.cos(time) / 8;
    }
}
