package de.mhus.nimbus.shared.voxel;

/**
 * Abstract interface for voxel mesh generation
 * Adapted from BlockMeshGenerator in Terasology
 */
public interface VoxelMeshGenerator {

    /**
     * Generates mesh data for the voxel
     */
    VoxelMesh generateMesh(Voxel voxel);

    /**
     * Generates mesh for a specific side of the voxel
     */
    VoxelMeshPart generateSideMesh(Voxel voxel, VoxelSide side);

    /**
     * Checks if this generator can handle the given voxel type
     */
    boolean canGenerateFor(Voxel voxel);
}
