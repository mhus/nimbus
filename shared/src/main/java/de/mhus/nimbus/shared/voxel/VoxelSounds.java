package de.mhus.nimbus.shared.voxel;

/**
 * Interface for voxel sound management
 * Adapted from BlockSounds in Terasology
 */
public interface VoxelSounds {

    /**
     * Sound when voxel is placed
     */
    String getPlaceSound();

    /**
     * Sound when voxel is broken/destroyed
     */
    String getBreakSound();

    /**
     * Sound when walking on the voxel
     */
    String getStepSound();

    /**
     * Sound when digging the voxel
     */
    String getDigSound();

    /**
     * Volume modifier for all sounds (0.0 - 1.0)
     */
    float getVolumeModifier();

    /**
     * Pitch modifier for all sounds (0.5 - 2.0)
     */
    float getPitchModifier();
}
