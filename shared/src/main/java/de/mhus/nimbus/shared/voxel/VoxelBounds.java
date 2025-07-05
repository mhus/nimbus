package de.mhus.nimbus.shared.voxel;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * Represents the 3D bounds of a voxel for collision detection
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VoxelBounds implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private float minX = 0.0f;
    private float minY = 0.0f;
    private float minZ = 0.0f;
    private float maxX = 1.0f;
    private float maxY = 1.0f;
    private float maxZ = 1.0f;

    public float getWidth() {
        return maxX - minX;
    }

    public float getHeight() {
        return maxY - minY;
    }

    public float getDepth() {
        return maxZ - minZ;
    }

    /**
     * Creates a new VoxelBounds translated by the specified offsets
     *
     * @param offsetX X offset
     * @param offsetY Y offset
     * @param offsetZ Z offset
     * @return A new translated VoxelBounds
     */
    public VoxelBounds translate(float offsetX, float offsetY, float offsetZ) {
        return new VoxelBounds(
            minX + offsetX,
            minY + offsetY,
            minZ + offsetZ,
            maxX + offsetX,
            maxY + offsetY,
            maxZ + offsetZ
        );
    }

    public boolean intersects(VoxelBounds other) {
        return !(maxX < other.minX || minX > other.maxX ||
                 maxY < other.minY || minY > other.maxY ||
                 maxZ < other.minZ || minZ > other.maxZ);
    }
}
