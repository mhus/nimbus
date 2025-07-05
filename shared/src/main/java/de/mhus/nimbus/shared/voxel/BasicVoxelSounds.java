package de.mhus.nimbus.shared.voxel;

/**
 * Basic implementation of VoxelSounds
 * Provides default sound effects for voxel interactions
 */
public class BasicVoxelSounds implements VoxelSounds {

    private final String placeSound;
    private final String breakSound;
    private final String stepSound;
    private final String digSound;
    private final float volumeModifier;
    private final float pitchModifier;

    public BasicVoxelSounds(String placeSound, String breakSound, String stepSound, String digSound) {
        this(placeSound, breakSound, stepSound, digSound, 1.0f, 1.0f);
    }

    public BasicVoxelSounds(String placeSound, String breakSound, String stepSound, String digSound,
                           float volumeModifier, float pitchModifier) {
        this.placeSound = placeSound;
        this.breakSound = breakSound;
        this.stepSound = stepSound;
        this.digSound = digSound;
        this.volumeModifier = Math.max(0.0f, Math.min(1.0f, volumeModifier));
        this.pitchModifier = Math.max(0.5f, Math.min(2.0f, pitchModifier));
    }

    @Override
    public String getPlaceSound() {
        return placeSound;
    }

    @Override
    public String getBreakSound() {
        return breakSound;
    }

    @Override
    public String getStepSound() {
        return stepSound;
    }

    @Override
    public String getDigSound() {
        return digSound;
    }

    @Override
    public float getVolumeModifier() {
        return volumeModifier;
    }

    @Override
    public float getPitchModifier() {
        return pitchModifier;
    }

    // Predefined sound sets for common voxel types
    public static final VoxelSounds STONE = new BasicVoxelSounds(
        "voxel.stone.place", "voxel.stone.break", "voxel.stone.step", "voxel.stone.dig"
    );

    public static final VoxelSounds WOOD = new BasicVoxelSounds(
        "voxel.wood.place", "voxel.wood.break", "voxel.wood.step", "voxel.wood.dig"
    );

    public static final VoxelSounds GRASS = new BasicVoxelSounds(
        "voxel.grass.place", "voxel.grass.break", "voxel.grass.step", "voxel.grass.dig"
    );

    public static final VoxelSounds SAND = new BasicVoxelSounds(
        "voxel.sand.place", "voxel.sand.break", "voxel.sand.step", "voxel.sand.dig"
    );

    public static final VoxelSounds WATER = new BasicVoxelSounds(
        "voxel.water.place", "voxel.water.break", "voxel.water.step", "voxel.water.dig", 0.5f, 1.2f
    );
}
