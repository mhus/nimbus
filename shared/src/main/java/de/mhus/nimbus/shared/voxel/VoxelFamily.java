package de.mhus.nimbus.shared.voxel;

/**
 * Abstract interface for voxel families
 * Groups related voxel types together (like different orientations of the same block)
 * Adapted from BlockFamily in Terasology
 */
public interface VoxelFamily {

    /**
     * Gets the URI that identifies this voxel family
     */
    VoxelUri getUri();

    /**
     * Gets the display name for this family
     */
    String getDisplayName();

    /**
     * Gets the base voxel for this family (usually the default orientation)
     */
    Voxel getBaseVoxel();

    /**
     * Gets a voxel from this family with specific rotation
     */
    Voxel getVoxelFor(VoxelRotation rotation);

    /**
     * Gets all voxels in this family
     */
    Iterable<Voxel> getVoxels();

    /**
     * Checks if a voxel belongs to this family
     */
    boolean contains(Voxel voxel);

    /**
     * Gets the category this family belongs to
     */
    String getCategory();
}
