
package supergame.network;

import com.jme3.scene.Spatial;
import supergame.control.ConsumeIntentControl;
import supergame.control.client.RemoteControl;
import supergame.terrain.ChunkIndex;

import java.util.HashMap;

public class Structs {
    public static class StateMessage {
        public double timestamp;
        public HashMap<Integer, EntityData> data;
    }

    public static class StartMessage {
        public int characterEntity;
    }

    public static class ChatMessage {
        public String s = null;
    }

    public static class Intent {
        public double timestamp;
        public float x;
        public float z;
        public float heading;
        public float pitch;
        public boolean jump;
        public boolean sprint;
        public boolean duck;
        public boolean use0;
        public boolean use1;
        public int toolSelection;
        public float targetDistance;

        public void set(Intent other) {
            timestamp = other.timestamp;
            x = other.x;
            z = other.z;
            heading = other.heading;
            pitch = other.pitch;
            jump = other.jump;
            sprint = other.sprint;
            duck = other.duck;
            use0 = other.use0;
            use1 = other.use1;
            toolSelection = other.toolSelection;
            targetDistance = other.targetDistance;
        }

        @Override
        public String toString() {
            return String.format("Moving %1.2f %1.2f, h %3.0f, p %3.0f (j%b s%b d%b u0%b u1%b) t %d, d %f",
                    x, z, heading, pitch, jump, sprint, duck, use0, use1, toolSelection, targetDistance);
        }
    }

    public static class DesiredActionMessage {
        public double timestamp;
        public float x;
        public float z;
        public float heading;
        public float pitch;
        public boolean jump;
        public boolean sprint;
        public boolean duck;
        public boolean use0;
        public boolean use1;
        public int toolSelection;
        public float targetDistance;
    }

    public static class ChunkMessage {
        public ChunkIndex index;
        public byte[] shortIndices;
        public byte[] intIndices;
        public byte[] vertices;
        public byte[] normals;
    }

    public static abstract class EntityData {
    }

    public static class BasicSpatialData extends EntityData {
        private final static int LERP_FIELDS = 5;
        // state for any given character that the server sends to the client
        public float array[] = new float[LERP_FIELDS]; // x,y,z, heading, pitch
    }

    public static abstract class Entity {
        protected final Spatial mSpatial;
        public Entity(Spatial spatial) {
            mSpatial = spatial;
        }

        public void apply(double serverTimestamp, EntityData packet) {
            mSpatial.getControl(RemoteControl.class).applyUpdatePacket(serverTimestamp, packet);
        }

        public EntityData getState() {
            return mSpatial.getControl(ConsumeIntentControl.class).generatePacket();
        }
    }
}
