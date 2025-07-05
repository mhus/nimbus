package de.mhus.nimbus.shared.voxel;

import java.util.*;

/**
 * Basic implementation of VoxelFamily
 * Groups voxels with different rotations but same base type
 */
public class BasicVoxelFamily implements VoxelFamily {

    private final VoxelUri uri;
    private final String displayName;
    private final String category;
    private final Map<VoxelRotation, Voxel> voxels;
    private final Voxel baseVoxel;

    public BasicVoxelFamily(VoxelUri uri, String displayName, String category, Voxel baseVoxel) {
        this.uri = uri;
        this.displayName = displayName;
        this.category = category;
        this.baseVoxel = baseVoxel;
        this.voxels = new EnumMap<>(VoxelRotation.class);
        this.voxels.put(VoxelRotation.NONE, baseVoxel);
    }

    @Override
    public VoxelUri getUri() {
        return uri;
    }

    @Override
    public String getDisplayName() {
        return displayName;
    }

    @Override
    public Voxel getBaseVoxel() {
        return baseVoxel;
    }

    @Override
    public Voxel getVoxelFor(VoxelRotation rotation) {
        return voxels.getOrDefault(rotation, baseVoxel);
    }

    @Override
    public Iterable<Voxel> getVoxels() {
        return voxels.values();
    }

    @Override
    public boolean contains(Voxel voxel) {
        return voxels.containsValue(voxel);
    }

    @Override
    public String getCategory() {
        return category;
    }

    /**
     * Adds a rotated variant to this family
     */
    public void addVoxel(VoxelRotation rotation, Voxel voxel) {
        voxels.put(rotation, voxel);
        voxel.setFamily(this);
    }

    /**
     * Gets the number of voxels in this family
     */
    public int size() {
        return voxels.size();
    }
}
