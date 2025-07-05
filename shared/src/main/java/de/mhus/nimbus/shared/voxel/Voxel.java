package de.mhus.nimbus.shared.voxel;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

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

    @Builder.Default
    private String displayName = "Untitled voxel";

    private VoxelFamily family;

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

    // Rendering related
    @Builder.Default
    private boolean translucent = false;

    @Builder.Default
    private boolean doubleSided = false;

    @Builder.Default
    private boolean shadowCasting = true;

    @Builder.Default
    private byte luminance = 0;

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
    private float friction = 0.5f;

    @Builder.Default
    private float restitution = 0.0f;

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
                .displayName(this.displayName)
                .family(this.family)
                .liquid(this.liquid)
                .attachmentAllowed(this.attachmentAllowed)
                .replacementAllowed(this.replacementAllowed)
                .hardness(this.hardness)
                .supportRequired(this.supportRequired)
                .translucent(this.translucent)
                .doubleSided(this.doubleSided)
                .shadowCasting(this.shadowCasting)
                .luminance(this.luminance)
                .penetrable(this.penetrable)
                .targetable(this.targetable)
                .climbable(this.climbable)
                .mass(this.mass)
                .friction(this.friction)
                .restitution(this.restitution)
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
}
