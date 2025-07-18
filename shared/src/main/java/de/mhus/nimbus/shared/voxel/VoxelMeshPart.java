package de.mhus.nimbus.shared.voxel;

import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * Represents mesh data for a specific part/side of a voxel
 */
@Data
public class VoxelMeshPart implements Serializable {

    @Serial
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
}
