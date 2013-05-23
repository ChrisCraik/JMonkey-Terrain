package supergame.utils;

import com.jme3.math.Vector3f;

public class GeometryUtils {
    /**
     * Returns a unit length vector pointing at the given heading and pitch in vec
     * @param vec Output vector
     * @param heading Heading, in degrees
     * @param pitch Pitch, in degrees
     */
    public static void HPVector(Vector3f vec, float heading, float pitch) {
        float x,y,z;
        x = -(float) (Math.sin(heading * Math.PI / 180.0) * Math.cos(pitch * Math.PI / 180.0));
        y = (float) (Math.sin(pitch * Math.PI / 180.0));
        z = (float) (Math.cos(heading * Math.PI / 180.0) * Math.cos(pitch * Math.PI / 180.0));

        vec.set(x, y, z);
    }
}
