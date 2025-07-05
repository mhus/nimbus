package de.mhus.nimbus.shared.voxel;

/**
 * Voxel rotation states
 * Similar to block rotations in Minecraft/Terasology
 */
public enum VoxelRotation {
    NONE(0),
    ROTATE_90(90),
    ROTATE_180(180),
    ROTATE_270(270);

    private final int degrees;

    VoxelRotation(int degrees) {
        this.degrees = degrees;
    }

    public int getDegrees() {
        return degrees;
    }

    public VoxelRotation next() {
        return switch (this) {
            case NONE -> ROTATE_90;
            case ROTATE_90 -> ROTATE_180;
            case ROTATE_180 -> ROTATE_270;
            case ROTATE_270 -> NONE;
        };
    }

    public VoxelRotation previous() {
        return switch (this) {
            case NONE -> ROTATE_270;
            case ROTATE_90 -> NONE;
            case ROTATE_180 -> ROTATE_90;
            case ROTATE_270 -> ROTATE_180;
        };
    }

    public static VoxelRotation fromDegrees(int degrees) {
        int normalized = ((degrees % 360) + 360) % 360;
        return switch (normalized) {
            case 0 -> NONE;
            case 90 -> ROTATE_90;
            case 180 -> ROTATE_180;
            case 270 -> ROTATE_270;
            default -> NONE;
        };
    }
}
