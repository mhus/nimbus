package de.mhus.nimbus.shared.voxel;

import java.io.Serializable;

/**
 * Represents mesh data for a specific part/side of a voxel
 */
public class VoxelMeshPart implements Serializable {

    private static final long serialVersionUID = 1L;

    private final Voxel voxel;
    private final VoxelSide side;
    private float[] vertices;
    private int[] indices;
    private float[] normals;
    private float[] textureCoords;

    public VoxelMeshPart(Voxel voxel, VoxelSide side) {
        this.voxel = voxel;
        this.side = side;
        generateSideMesh();
    }

    private void generateSideMesh() {
        // Generate vertices for specific side
        switch (side) {
            case FRONT -> generateFrontFace();
            case BACK -> generateBackFace();
            case TOP -> generateTopFace();
            case BOTTOM -> generateBottomFace();
            case LEFT -> generateLeftFace();
            case RIGHT -> generateRightFace();
        }
    }

    private void generateFrontFace() {
        vertices = new float[] {
            0.0f, 0.0f, 1.0f,  1.0f, 0.0f, 1.0f,  1.0f, 1.0f, 1.0f,  0.0f, 1.0f, 1.0f
        };
        indices = new int[] { 0, 1, 2, 0, 2, 3 };
    }

    private void generateBackFace() {
        vertices = new float[] {
            1.0f, 0.0f, 0.0f,  0.0f, 0.0f, 0.0f,  0.0f, 1.0f, 0.0f,  1.0f, 1.0f, 0.0f
        };
        indices = new int[] { 0, 1, 2, 0, 2, 3 };
    }

    private void generateTopFace() {
        vertices = new float[] {
            0.0f, 1.0f, 0.0f,  0.0f, 1.0f, 1.0f,  1.0f, 1.0f, 1.0f,  1.0f, 1.0f, 0.0f
        };
        indices = new int[] { 0, 1, 2, 0, 2, 3 };
    }

    private void generateBottomFace() {
        vertices = new float[] {
            1.0f, 0.0f, 0.0f,  1.0f, 0.0f, 1.0f,  0.0f, 0.0f, 1.0f,  0.0f, 0.0f, 0.0f
        };
        indices = new int[] { 0, 1, 2, 0, 2, 3 };
    }

    private void generateLeftFace() {
        vertices = new float[] {
            0.0f, 0.0f, 1.0f,  0.0f, 1.0f, 1.0f,  0.0f, 1.0f, 0.0f,  0.0f, 0.0f, 0.0f
        };
        indices = new int[] { 0, 1, 2, 0, 2, 3 };
    }

    private void generateRightFace() {
        vertices = new float[] {
            1.0f, 0.0f, 0.0f,  1.0f, 1.0f, 0.0f,  1.0f, 1.0f, 1.0f,  1.0f, 0.0f, 1.0f
        };
        indices = new int[] { 0, 1, 2, 0, 2, 3 };
    }

    public float[] getVertices() { return vertices; }
    public int[] getIndices() { return indices; }
    public float[] getNormals() { return normals; }
    public float[] getTextureCoords() { return textureCoords; }
    public Voxel getVoxel() { return voxel; }
    public VoxelSide getSide() { return side; }
}
