package de.mhus.nimbus.shared.voxel;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.joml.Vector3f;
import org.joml.Vector3i;
import org.joml.Vector3ic;
import org.joml.RoundingMode;

import java.io.Serial;
import java.io.Serializable;

/**
 * Stores all information for a specific voxel type.
 * Adapted from the Terasology Block model for the Nimbus platform.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString
public final class Voxel implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @EqualsAndHashCode.Include
    private short id;

    private VoxelUri uri;

    @Builder.Default
    private String displayName = "Untitled voxel";

    private VoxelFamily family;

    @Builder.Default
    private VoxelRotation rotation = VoxelRotation.NONE;

    /* PROPERTIES */

    // Overall behavioral
    @Builder.Default
    private boolean liquid = false;

    @Builder.Default
    private boolean attachmentAllowed = true;

    @Builder.Default
    private boolean replacementAllowed = true;

    @Builder.Default
    private int hardness = 3;

    @Builder.Default
    private boolean supportRequired = false;

    // Full side support for each side (6 sides: FRONT, BACK, LEFT, RIGHT, TOP, BOTTOM)
    private final boolean[] fullSide = new boolean[6];

    private VoxelSounds sounds;

    // Special rendering flags
    @Builder.Default
    private boolean water = false;

    @Builder.Default
    private boolean grass = false;

    @Builder.Default
    private boolean ice = false;

    // Rendering related
    private VoxelMeshGenerator meshGenerator;

    @Builder.Default
    private boolean translucent = false;

    @Builder.Default
    private boolean doubleSided = false;

    @Builder.Default
    private boolean shadowCasting = true;

    @Builder.Default
    private boolean waving = false;

    @Builder.Default
    private byte luminance = 0;

    @Builder.Default
    private VoxelColor tint = new VoxelColor(1.0f, 1.0f, 1.0f, 1.0f);

    // Collision related
    @Builder.Default
    private boolean penetrable = false;

    @Builder.Default
    private boolean targetable = true;

    @Builder.Default
    private boolean climbable = false;

    // Physics
    @Builder.Default
    private float mass = 10.0f;

    @Builder.Default
    private boolean debrisOnDestroy = true;

    @Builder.Default
    private float friction = 0.5f;

    @Builder.Default
    private float restitution = 0.0f;

    // Entity integration
    @Builder.Default
    private boolean keepActive = false;

    @Builder.Default
    private boolean lifecycleEventsRequired = false;

    // Inventory settings
    @Builder.Default
    private boolean directPickup = false;

    @Builder.Default
    private boolean stackable = true;

    // Collision
    private VoxelBounds bounds;

    private Vector3f collisionOffset;

    // Position in world
    @EqualsAndHashCode.Include
    private int x;

    @EqualsAndHashCode.Include
    private int y;

    @EqualsAndHashCode.Include
    private int z;

    @EqualsAndHashCode.Include
    private byte metadata;

    @Builder.Default
    private long lastModified = System.currentTimeMillis();

    /**
     * Constructor with position and basic properties
     */
    public Voxel(int x, int y, int z, VoxelType type) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.id = (short) type.getId();
        this.displayName = type.getDisplayName();
        this.liquid = type.isLiquid();
        this.hardness = type.getHardness();
        this.luminance = (byte) type.getLuminance();
        this.penetrable = !type.isSolid();
        this.lastModified = System.currentTimeMillis();

        // Set default values for other fields
        this.attachmentAllowed = true;
        this.replacementAllowed = true;
        this.supportRequired = false;
        this.translucent = false;
        this.doubleSided = false;
        this.shadowCasting = true;
        this.targetable = true;
        this.climbable = false;
        this.mass = 10.0f;
        this.friction = 0.5f;
        this.restitution = 0.0f;
    }

    /**
     * Convenience methods
     */
    public boolean isSolid() {
        return !penetrable && !liquid;
    }

    public boolean isBreakable() {
        return hardness > 0;
    }

    public boolean isAir() {
        return id == VoxelType.AIR.getId();
    }

    /**
     * Creates a copy of this voxel at a new position
     */
    public Voxel copyAt(int newX, int newY, int newZ) {
        return Voxel.builder()
                .x(newX)
                .y(newY)
                .z(newZ)
                .id(this.id)
                .uri(this.uri)
                .displayName(this.displayName)
                .family(this.family)
                .rotation(this.rotation)
                .liquid(this.liquid)
                .attachmentAllowed(this.attachmentAllowed)
                .replacementAllowed(this.replacementAllowed)
                .hardness(this.hardness)
                .supportRequired(this.supportRequired)
                .sounds(this.sounds)
                .water(this.water)
                .grass(this.grass)
                .ice(this.ice)
                .meshGenerator(this.meshGenerator)
                .translucent(this.translucent)
                .doubleSided(this.doubleSided)
                .shadowCasting(this.shadowCasting)
                .waving(this.waving)
                .luminance(this.luminance)
                .tint(this.tint)
                .penetrable(this.penetrable)
                .targetable(this.targetable)
                .climbable(this.climbable)
                .mass(this.mass)
                .debrisOnDestroy(this.debrisOnDestroy)
                .friction(this.friction)
                .restitution(this.restitution)
                .keepActive(this.keepActive)
                .lifecycleEventsRequired(this.lifecycleEventsRequired)
                .directPickup(this.directPickup)
                .stackable(this.stackable)
                .bounds(this.bounds)
                .collisionOffset(this.collisionOffset)
                .metadata(this.metadata)
                .lastModified(System.currentTimeMillis())
                .build();
    }

    /**
     * Updates the lastModified timestamp
     */
    public void touch() {
        this.lastModified = System.currentTimeMillis();
    }

    /**
     * Gets the direction this voxel is facing based on its rotation
     */
    public VoxelSide getDirection() {
        return rotation.rotate(VoxelSide.FRONT);
    }

    /**
     * Checks if a voxel can attach to the specified side of this voxel
     *
     * @param side The side to check for attachment
     * @return true if attachment is allowed and the side is full
     */
    public boolean canAttachTo(VoxelSide side) {
        return attachmentAllowed && isFullSide(side);
    }

    /**
     * Checks if the specified side of this voxel is full
     *
     * @param side The side to check
     * @return true if the side is full
     */
    public boolean isFullSide(VoxelSide side) {
        return fullSide[side.ordinal()];
    }

    /**
     * Sets whether the specified side of this voxel is full
     *
     * @param side The side to set
     * @param full Whether the side is full
     */
    public void setFullSide(VoxelSide side, boolean full) {
        fullSide[side.ordinal()] = full;
    }

    /**
     * Checks if this voxel is destructible (can be broken)
     *
     * @return true if hardness > 0
     */
    public boolean isDestructible() {
        return getHardness() > 0;
    }

    /**
     * Override isShadowCasting to consider luminance like in Block class
     *
     * @return true if shadow casting is enabled and luminance is 0
     */
    public boolean isShadowCasting() {
        return shadowCasting && luminance == 0;
    }

    /**
     * Gets the bounding box for this voxel at the specified position
     *
     * @param pos The position as Vector3ic
     * @return A new VoxelBounds translated to the position
     */
    public VoxelBounds getBounds(Vector3ic pos) {
        if (bounds == null) {
            return null;
        }
        return bounds.translate(pos.x(), pos.y(), pos.z());
    }

    /**
     * Gets the bounding box for this voxel at the specified position
     *
     * @param floatPos The position as Vector3f
     * @return A new VoxelBounds translated to the position
     */
    public VoxelBounds getBounds(Vector3f floatPos) {
        return getBounds(new Vector3i(floatPos, RoundingMode.HALF_UP));
    }
}
