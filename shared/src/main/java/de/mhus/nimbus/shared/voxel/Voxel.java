package de.mhus.nimbus.shared.voxel;

import java.io.Serializable;
import java.util.Objects;

/**
 * Stores all information for a specific voxel type.
 * Adapted from the Terasology Block model for the Nimbus platform.
 */
public final class Voxel implements Serializable {

    private static final long serialVersionUID = 1L;

    private short id;
    private String displayName = "Untitled voxel";
    private VoxelFamily family;

    /* PROPERTIES */

    // Overall behavioral
    private boolean liquid;
    private boolean attachmentAllowed = true;
    private boolean replacementAllowed = true;
    private int hardness = 3;
    private boolean supportRequired;

    // Rendering related
    private boolean translucent;
    private boolean doubleSided;
    private boolean shadowCasting = true;
    private byte luminance;

    // Collision related
    private boolean penetrable;
    private boolean targetable = true;
    private boolean climbable;

    // Physics
    private float mass = 10.0f;
    private float friction = 0.5f;
    private float restitution = 0.0f;

    // Position in world
    private int x;
    private int y;
    private int z;
    private byte metadata;
    private long lastModified;

    /**
     * Initialize a new voxel with default properties.
     */
    public Voxel() {
        this.lastModified = System.currentTimeMillis();
    }

    /**
     * Constructor with position and basic properties
     */
    public Voxel(int x, int y, int z, VoxelType type) {
        this();
        this.x = x;
        this.y = y;
        this.z = z;
        this.id = (short) type.getId();
        this.displayName = type.getDisplayName();
        this.liquid = type.isLiquid();
        this.hardness = type.getHardness();
        this.luminance = (byte) type.getLuminance();
        this.penetrable = !type.isSolid();
    }

    // Basic getters and setters
    public short getId() {
        return id;
    }

    public void setId(short id) {
        this.id = id;
        this.lastModified = System.currentTimeMillis();
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public int getZ() {
        return z;
    }

    public void setZ(int z) {
        this.z = z;
    }

    public byte getMetadata() {
        return metadata;
    }

    public void setMetadata(byte metadata) {
        this.metadata = metadata;
        this.lastModified = System.currentTimeMillis();
    }

    public long getLastModified() {
        return lastModified;
    }

    public VoxelFamily getFamily() {
        return family;
    }

    public void setFamily(VoxelFamily family) {
        this.family = family;
    }

    // Behavioral properties
    public boolean isLiquid() {
        return liquid;
    }

    public void setLiquid(boolean liquid) {
        this.liquid = liquid;
    }

    public int getHardness() {
        return hardness;
    }

    public void setHardness(int hardness) {
        this.hardness = hardness;
    }

    public byte getLuminance() {
        return luminance;
    }

    public void setLuminance(byte luminance) {
        this.luminance = luminance;
    }

    public boolean isPenetrable() {
        return penetrable;
    }

    public void setPenetrable(boolean penetrable) {
        this.penetrable = penetrable;
    }

    public boolean isTranslucent() {
        return translucent;
    }

    public void setTranslucent(boolean translucent) {
        this.translucent = translucent;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Voxel voxel = (Voxel) o;
        return x == voxel.x &&
               y == voxel.y &&
               z == voxel.z &&
               id == voxel.id &&
               metadata == voxel.metadata;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, x, y, z, metadata);
    }

    @Override
    public String toString() {
        return String.format("Voxel{id=%d, name='%s', pos=(%d,%d,%d), metadata=%d}",
                           id, displayName, x, y, z, metadata);
    }

    /**
     * Creates a copy of this voxel at a new position
     */
    public Voxel copyAt(int newX, int newY, int newZ) {
        Voxel copy = new Voxel(newX, newY, newZ, VoxelType.fromId(this.id));
        copy.metadata = this.metadata;
        copy.displayName = this.displayName;
        copy.liquid = this.liquid;
        copy.hardness = this.hardness;
        copy.translucent = this.translucent;
        copy.luminance = this.luminance;
        return copy;
    }
}
