
package supergame.control.server;

import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.AbstractControl;
import com.jme3.scene.control.Control;

import supergame.network.Structs.Intent;

/**
 * Used by the network to pass Intents from over the network to a character on the server.
 *
 * Does nothing more than copy the most recent intent it has received to the receiving control.
 */
public class RemoteProduceIntentControl extends AbstractControl {
    private final Intent mIntent;
    private final Intent mNewIntent = new Intent();

    public RemoteProduceIntentControl(Intent intent) {
        mIntent = intent;
    }

    /**
     * Used by the ServerEntityManager to pass intents to characters
     *
     * @param newIntent Most recently received intent (not necessarily newest)
     */
    public void setIntent(Intent newIntent) {
        if (newIntent.timestamp > mNewIntent.timestamp) {
            mNewIntent.set(newIntent);
        }
    }

    @Override
    public Control cloneForSpatial(Spatial arg0) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void controlRender(RenderManager arg0, ViewPort arg1) {
    }

    @Override
    protected void controlUpdate(float tpf) {
        if (mNewIntent.timestamp > mIntent.timestamp) {
            mIntent.set(mNewIntent);
        }
    }
}
