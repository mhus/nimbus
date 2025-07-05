package de.mhus.nimbus.shared.voxel;

/**
 * Represents the six sides of a voxel
 * Similar to the Side enum in Terasology
 */
public enum VoxelSide {
    TOP(0, 0, 1, 0),
    BOTTOM(1, 0, -1, 0),
    FRONT(2, 0, 0, -1),
    BACK(3, 0, 0, 1),
    LEFT(4, -1, 0, 0),
    RIGHT(5, 1, 0, 0);

    private final int index;
    private final int xOffset;
    private final int yOffset;
    private final int zOffset;

    VoxelSide(int index, int xOffset, int yOffset, int zOffset) {
        this.index = index;
        this.xOffset = xOffset;
        this.yOffset = yOffset;
        this.zOffset = zOffset;
    }

    public int getIndex() {
        return index;
    }

    public int getXOffset() {
        return xOffset;
    }

    public int getYOffset() {
        return yOffset;
    }

    public int getZOffset() {
        return zOffset;
    }

    /**
     * Gets the opposite side
     */
    public VoxelSide getOpposite() {
        return switch (this) {
            case TOP -> BOTTOM;
            case BOTTOM -> TOP;
            case FRONT -> BACK;
            case BACK -> FRONT;
            case LEFT -> RIGHT;
            case RIGHT -> LEFT;
        };
    }

    /**
     * Checks if this side is horizontal
     */
    public boolean isHorizontal() {
        return this == FRONT || this == BACK || this == LEFT || this == RIGHT;
    }

    /**
     * Checks if this side is vertical
     */
    public boolean isVertical() {
        return this == TOP || this == BOTTOM;
    }

    public static VoxelSide fromIndex(int index) {
        for (VoxelSide side : values()) {
            if (side.index == index) {
                return side;
            }
        }
        return TOP;
    }
}
