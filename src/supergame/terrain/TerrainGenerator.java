
package supergame.terrain;

import com.jme3.math.Vector3f;

import supergame.utils.Perlin;

public class TerrainGenerator {

    public static void getNormal(float x, float y, float z, Vector3f v) {
        // first attempt: brute force
        float delta = 0.01f, origin, deltaX, deltaY, deltaZ;
        origin = getDensity(x, y, z);
        deltaX = origin - getDensity(x + delta, y, z);
        deltaY = origin - getDensity(x, y + delta, z);
        deltaZ = origin - getDensity(x, y, z + delta);

        v.set(deltaX, deltaY, deltaZ);
        v.normalize();
    }

    public static float getDensity(Vector3f p) {
        return getDensity(p.x, p.y, p.z);
    }

    public static float getDensity(float x, float y, float z) {
        //return (float) Perlin.noise(x / 20, y / 20, z / 20);
        //return (float) Perlin.noise(x / 10, y / 10, z / 10) + 0.0f - y * 0.05f;
        return (float) Perlin.noise(x / 20, 0, z / 20) + 0.0f - y * 0.05f;
        /*
        float caves, center_falloff, plateau_falloff, density;
        if (y <= 0.8)
            plateau_falloff = 1.0f;
        else if (0.8 < y && y < 0.9)
            plateau_falloff = 1.0f - (y - 0.8f) * 10;
        else
            plateau_falloff = 0.0f;

        center_falloff = (float) (0.1 / (Math.pow((x - 0.5) * 1.5, 2) + Math.pow((y - 1.0) * 0.8, 2) + Math.pow(
                (z - 0.5) * 1.5, 2)));
        caves = (float) Math.pow(Perlin.octave_noise(1, x * 5, y * 5, z * 5), 3);
        density = (float) (Perlin.octave_noise(5, x, y * 0.5, z) * center_falloff * plateau_falloff);
        density *= Math.pow(Perlin.noise((x + 1) * 3.0, (y + 1) * 3.0, (z + 1) * 3.0) + 0.4, 1.8);
        if (caves < 0.5)density = 0;
        return density;
        */
    }
}
