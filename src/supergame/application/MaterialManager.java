
package supergame.application;

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;

public class MaterialManager {
    private static Material sCharacterMaterial = null;
    private static Material sBoxMaterial = null;
    private static Material sTerrainMaterial = null;

    public static Material getCharacterMaterial() {
        return sCharacterMaterial;
    }

    public static Material getBoxMaterial() {
        return sBoxMaterial;
    }

    public static Material getTerrainMaterial() {
        return sTerrainMaterial;
    }

    public static void init(AssetManager assetManager) {
        sCharacterMaterial = new Material(assetManager,
                "Common/MatDefs/Light/Lighting.j3md");
        sCharacterMaterial.setBoolean("UseMaterialColors", true);
        sCharacterMaterial.setColor("Ambient", ColorRGBA.Green);
        sCharacterMaterial.setColor("Diffuse", ColorRGBA.Green);

        sBoxMaterial = new Material(assetManager,
                "Common/MatDefs/Light/Lighting.j3md");
        sBoxMaterial.setBoolean("UseMaterialColors", true);
        sBoxMaterial.setColor("Ambient", ColorRGBA.DarkGray);
        sBoxMaterial.setColor("Diffuse", ColorRGBA.DarkGray);

        sTerrainMaterial = new Material(assetManager,
                "Common/MatDefs/Light/Lighting.j3md");
        sTerrainMaterial.setBoolean("UseMaterialColors", true);
        sTerrainMaterial.setColor("Ambient", ColorRGBA.Brown);
        sTerrainMaterial.setColor("Diffuse", ColorRGBA.Brown);
    }
}
