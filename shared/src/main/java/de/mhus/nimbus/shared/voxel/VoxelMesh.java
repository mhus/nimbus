package de.mhus.nimbus.shared.voxel;

import java.io.Serializable;

/**
 * Represents mesh data for a voxel
 */
public class VoxelMesh implements Serializable {

    private static final long serialVersionUID = 1L;

    private final Voxel voxel;
    private float[] vertices;
    private int[] indices;
    private float[] normals;
    private float[] textureCoords;

    public VoxelMesh(Voxel voxel) {
        this.voxel = voxel;
        generateCubeMesh();
    }

    private void generateCubeMesh() {
        // Simple cube vertices for a 1x1x1 voxel
        vertices = new float[] {
            // Front face
            0.0f, 0.0f, 1.0f,  1.0f, 0.0f, 1.0f,  1.0f, 1.0f, 1.0f,  0.0f, 1.0f, 1.0f,
            // Back face
            1.0f, 0.0f, 0.0f,  0.0f, 0.0f, 0.0f,  0.0f, 1.0f, 0.0f,  1.0f, 1.0f, 0.0f,
            // Top face
            0.0f, 1.0f, 0.0f,  0.0f, 1.0f, 1.0f,  1.0f, 1.0f, 1.0f,  1.0f, 1.0f, 0.0f,
            // Bottom face
            1.0f, 0.0f, 0.0f,  1.0f, 0.0f, 1.0f,  0.0f, 0.0f, 1.0f,  0.0f, 0.0f, 0.0f,
            // Right face
            1.0f, 0.0f, 0.0f,  1.0f, 1.0f, 0.0f,  1.0f, 1.0f, 1.0f,  1.0f, 0.0f, 1.0f,
            // Left face
            0.0f, 0.0f, 1.0f,  0.0f, 1.0f, 1.0f,  0.0f, 1.0f, 0.0f,  0.0f, 0.0f, 0.0f
        };

        // Triangle indices for cube faces
        indices = new int[] {
            0,  1,  2,    0,  2,  3,    // front
            4,  5,  6,    4,  6,  7,    // back
            8,  9,  10,   8,  10, 11,   // top
            12, 13, 14,   12, 14, 15,   // bottom
            16, 17, 18,   16, 18, 19,   // right
            20, 21, 22,   20, 22, 23    // left
        };
    }

    public float[] getVertices() { return vertices; }
    public int[] getIndices() { return indices; }
    public float[] getNormals() { return normals; }
    public float[] getTextureCoords() { return textureCoords; }
    public Voxel getVoxel() { return voxel; }
}
