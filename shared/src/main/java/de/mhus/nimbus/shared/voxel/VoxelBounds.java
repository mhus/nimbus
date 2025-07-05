package de.mhus.nimbus.shared.voxel;

import java.io.Serializable;

/**
 * Represents the 3D bounds of a voxel for collision detection
 */
public class VoxelBounds implements Serializable {

    private static final long serialVersionUID = 1L;

    private float minX, minY, minZ;
    private float maxX, maxY, maxZ;

    public VoxelBounds() {
        this(0.0f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f);
    }

    public VoxelBounds(float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
        this.minX = minX;
        this.minY = minY;
        this.minZ = minZ;
        this.maxX = maxX;
        this.maxY = maxY;
        this.maxZ = maxZ;
    }

    public float getMinX() { return minX; }
    public float getMinY() { return minY; }
    public float getMinZ() { return minZ; }
    public float getMaxX() { return maxX; }
    public float getMaxY() { return maxY; }
    public float getMaxZ() { return maxZ; }

    public void setMinX(float minX) { this.minX = minX; }
    public void setMinY(float minY) { this.minY = minY; }
    public void setMinZ(float minZ) { this.minZ = minZ; }
    public void setMaxX(float maxX) { this.maxX = maxX; }
    public void setMaxY(float maxY) { this.maxY = maxY; }
    public void setMaxZ(float maxZ) { this.maxZ = maxZ; }

    public float getWidth() { return maxX - minX; }
    public float getHeight() { return maxY - minY; }
    public float getDepth() { return maxZ - minZ; }

    public boolean intersects(VoxelBounds other) {
        return !(maxX < other.minX || minX > other.maxX ||
                 maxY < other.minY || minY > other.maxY ||
                 maxZ < other.minZ || minZ > other.maxZ);
    }
}
