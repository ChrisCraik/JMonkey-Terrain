
package supergame.terrain;

import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.bullet.util.CollisionShapeFactory;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer.Type;

import supergame.Config;
import supergame.terrain.Chunk.WorkerBuffers;
import supergame.terrain.modify.ChunkModifierInterface;
import supergame.utils.MarchingCubes;

import java.nio.ByteBuffer;

/**
 * Collection of static utility functions used in creating chunk geometry.
 */
public class ChunkUtils {
    /** Purely static class */
    private ChunkUtils() {};

    /**
     * Updates weights used in calculating chunk geometry. Positive values
     * indicate presence of terrain, negative values indicate its absence.
     *
     * @param weights The input/output weight data
     * @param skipGeneration If true, the weights have already been calculated
     *            by a previous generation, (but haven't been modified).
     * @param position Origin of the chunk
     * @param modifier Modifier object that alters
     * @return False if empty.
     */
    public static boolean calculateWeights(float[][][] weights, boolean skipGeneration,
            Vector3f position, ChunkModifierInterface modifier) {
        int posCount = 0, negCount = 0;
        Vector3f localPos = new Vector3f();
        for (int x = 0; x < Config.CHUNK_DIVISION + 1; x++) {
            for (int y = 0; y < Config.CHUNK_DIVISION + 1; y++) {
                for (int z = 0; z < Config.CHUNK_DIVISION + 1; z++) {
                    localPos.set(position.getX() + x, position.getY() + y, position.getZ() + z);

                    if (!skipGeneration) {
                        // haven't already initialized the weights from parent,
                        // so need to call TerrainGenerator
                        weights[x][y][z] = TerrainGenerator.getDensity(localPos);
                    }

                    if (modifier != null) {
                        weights[x][y][z] = modifier.getModification(localPos, weights[x][y][z]);
                    }

                    if (weights[x][y][z] < 0) {
                        negCount++;
                    } else {
                        posCount++;
                    }
                }
            }
        }
        return (negCount != 0) && (posCount != 0);
    }

    /**
     * Generate vertices and indices from weights via MarchingCubes
     * @param buffers Struct containing input weights, and output vertex/index lists.
     * @param position Origin of the chunk.
     * @return False if empty.
     */
    public static boolean calculateTriangles(WorkerBuffers buffers, Vector3f position) {
        int x, y, z;
        buffers.indicesIntCount = 0;
        buffers.verticesFloatCount = 0;
        Vector3f blockPos = new Vector3f(0, 0, 0);
        for (x = 0; x < Config.CHUNK_DIVISION + 1; x++) {
            for (y = 0; y < Config.CHUNK_DIVISION + 1; y++) {
                for (z = 0; z < Config.CHUNK_DIVISION + 1; z++) {
                    if (MarchingCubes.cubeOccupied(x, y, z, buffers.weights)) {
                        // calculate vertices, populate vertex buffer,
                        // vertIndexVolume buffer (NOTE some of these vertices
                        // wasted: reside in neighbor chunks)
                        blockPos.set(position.getX() + x, position.getY() + y, position.getZ() + z);
                        buffers.verticesFloatCount = MarchingCubes.writeLocalVertices(blockPos, x,
                                y, z, buffers.weights, buffers.vertices,
                                buffers.verticesFloatCount, buffers.vertIndexVolume);
                    }
                }
            }
        }
        for (x = 0; x < Config.CHUNK_DIVISION; x++) {
            for (y = 0; y < Config.CHUNK_DIVISION; y++) {
                for (z = 0; z < Config.CHUNK_DIVISION; z++) {
                    if (MarchingCubes.cubeOccupied(x, y, z, buffers.weights)) {
                        // calculate indices
                        buffers.indicesIntCount = MarchingCubes.writeLocalIndices(x, y, z,
                                buffers.weights, buffers.indices, buffers.indicesIntCount,
                                buffers.vertIndexVolume);
                    }
                }
            }
        }

        return (buffers.verticesFloatCount != 0) && (buffers.indicesIntCount != 0);
    }

    /**
     * Perform brute-force normal calculation, without the simpler and more
     * efficient terrain algorithm weight sampling. This approach is required if
     * the terrain is modified beyond its original form.
     *
     * @param buffers Struct containing input vertex and index lists, as well as
     *            output normals hashmap.
     */
    public static void calculateNormals(WorkerBuffers buffers) {
        // manually calculate normals, because we have been modified since generation
        for (int i = 0; i < buffers.indicesIntCount; i += 3) {
            Vector3f vectors[] = new Vector3f[3];
            for (int j = 0; j < 3; j++) {
                vectors[j] = new Vector3f(
                        buffers.vertices[buffers.indices[i + j] + 0],
                        buffers.vertices[buffers.indices[i + j] + 1],
                        buffers.vertices[buffers.indices[i + j] + 2]);
            }

            Vector3f a = vectors[0].subtract(vectors[1]);
            Vector3f b = vectors[0].subtract(vectors[2]);

            // Note: normal vector isn't normalized so that bigger triangles have more weight
            Vector3f normal = a.cross(b);

            for (int j = 0; j < 3; j++) {
                int index = buffers.indices[i + j];
                Vector3f normalSumForIndex = buffers.normals.get(index);
                if (normalSumForIndex == null) {
                    // COPY first normal into hash (since the value in hash
                    // will be modified)
                    buffers.normals.put(index, new Vector3f(normal));
                } else {
                    // add into normal (since they're all normalized, we can
                    // normalize the sum later to get the average)
                    normalSumForIndex.addLocal(normal);
                }
            }
        }
    }

    /**
     * Copy index, vertex, and normal lists from WorkerBuffers struct to
     * ByteBuffers. Currently generates redundant index lists - both shorts and
     * ints.
     *
     * @param buffers Struct containing input vertex, index, and normal buffers.
     * @param verticesBuffer Output list of vertices, three floats each.
     * @param normalsBuffer Output list of normals each associated with a
     *            vertex.
     * @param shortIndicesBuffer Output list of indices, three shorts per
     *            triangle.
     * @param intIndicesBuffer Output list of indices, three ints per triangle.
     */
    public static void storeTrianglesAndNormals(WorkerBuffers buffers, ByteBuffer verticesBuffer,
            ByteBuffer normalsBuffer, ByteBuffer shortIndicesBuffer, ByteBuffer intIndicesBuffer) {
        Vector3f normal = new Vector3f(0, 0, 0);
        for (int i = 0; i < buffers.verticesFloatCount; i += 3) {
            float vx = buffers.vertices[i + 0];
            float vy = buffers.vertices[i + 1];
            float vz = buffers.vertices[i + 2];

            verticesBuffer.putFloat(vx);
            verticesBuffer.putFloat(vy);
            verticesBuffer.putFloat(vz);

            if (buffers.normals.containsKey(i)) {
                normal = buffers.normals.get(i).normalize();
            } else {
                TerrainGenerator.getNormal(vx, vy, vz, normal);
            }

            normalsBuffer.putFloat(normal.getX());
            normalsBuffer.putFloat(normal.getY());
            normalsBuffer.putFloat(normal.getZ());
        }
        for (int i = 0; i < buffers.indicesIntCount; i++) {
            shortIndicesBuffer.putShort((short) (buffers.indices[i] / 3));
            intIndicesBuffer.putInt(buffers.indices[i] / 3);
        }

        verticesBuffer.flip();
        normalsBuffer.flip();
        shortIndicesBuffer.flip();
        intIndicesBuffer.flip();
    }

    /**
     * Create a Geometry from triangle information in ByteBuffers.
     *
     * @param chunkName Name given to Geometry object.
     * @param intIndicesBuffer Indices for the Geometry.
     * @param verticesBuffer Vertices for the Geometry.
     * @param normalsBuffer Normals for the Geometry
     * @return The created Geometry.
     */
    public static Geometry createGeometry(String chunkName, ByteBuffer intIndicesBuffer,
            ByteBuffer verticesBuffer, ByteBuffer normalsBuffer) {
        Mesh m = new Mesh();
        m.setBuffer(Type.Index, 1, intIndicesBuffer.asIntBuffer());
        m.setBuffer(Type.Position, 3, verticesBuffer.asFloatBuffer());
        m.setBuffer(Type.Normal, 3, normalsBuffer.asFloatBuffer());
        m.updateBound();

        Geometry g = new Geometry(chunkName, m);

        CollisionShape shape = CollisionShapeFactory.createMeshShape(g);
        RigidBodyControl control = new RigidBodyControl(shape, 0);
        g.addControl(control);
        return g;
    }
}
