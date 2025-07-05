package de.mhus.nimbus.shared.voxel;

/**
 * Simple single-shape mesh generator for voxels
 * Default implementation of VoxelMeshGenerator
 */
public class VoxelMeshGeneratorSingleShape implements VoxelMeshGenerator {

    private final Voxel voxel;

    public VoxelMeshGeneratorSingleShape(Voxel voxel) {
        this.voxel = voxel;
    }

    @Override
    public VoxelMesh generateMesh(Voxel voxel) {
        // Simple cube mesh generation
        return new VoxelMesh(voxel);
    }

    @Override
    public VoxelMeshPart generateSideMesh(Voxel voxel, VoxelSide side) {
        // Generate mesh for specific side
        return new VoxelMeshPart(voxel, side);
    }

    @Override
    public boolean canGenerateFor(Voxel voxel) {
        return voxel != null && !voxel.isAir();
    }
}
