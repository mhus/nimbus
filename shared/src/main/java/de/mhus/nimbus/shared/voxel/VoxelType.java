package de.mhus.nimbus.shared.voxel;

/**
 * Voxel-Typen ähnlich wie in Minecraft
 * Definiert die verschiedenen Arten von Blöcken/Voxeln mit erweiterten Eigenschaften
 */
public enum VoxelType {
    AIR(0, "Air", false, false, 0, false, 0.0f),
    STONE(1, "Stone", true, true, 5, false, 0.0f),
    GRASS(2, "Grass", true, true, 2, false, 0.0f),
    DIRT(3, "Dirt", true, true, 2, false, 0.0f),
    COBBLESTONE(4, "Cobblestone", true, true, 6, false, 0.0f),
    WOOD(5, "Wood", true, true, 3, false, 0.0f),
    LEAVES(6, "Leaves", true, false, 1, false, 0.0f),
    SAND(7, "Sand", true, true, 2, false, 0.0f),
    WATER(8, "Water", false, false, 0, true, 0.0f),
    LAVA(9, "Lava", false, false, 0, true, 15.0f),
    IRON_ORE(10, "Iron Ore", true, true, 8, false, 0.0f),
    GOLD_ORE(11, "Gold Ore", true, true, 8, false, 0.0f),
    DIAMOND_ORE(12, "Diamond Ore", true, true, 10, false, 0.0f),
    BEDROCK(13, "Bedrock", true, false, -1, false, 0.0f),
    GLOWSTONE(14, "Glowstone", true, true, 1, false, 15.0f),
    TORCH(15, "Torch", false, true, 1, false, 14.0f);

    private final int id;
    private final String displayName;
    private final boolean solid;
    private final boolean breakable;
    private final int hardness;
    private final boolean liquid;
    private final float luminance;

    VoxelType(int id, String displayName, boolean solid, boolean breakable, int hardness,
              boolean liquid, float luminance) {
        this.id = id;
        this.displayName = displayName;
        this.solid = solid;
        this.breakable = breakable;
        this.hardness = hardness;
        this.liquid = liquid;
        this.luminance = luminance;
    }

    public int getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isSolid() {
        return solid;
    }

    public boolean isBreakable() {
        return breakable;
    }

    public int getHardness() {
        return hardness;
    }

    public boolean isLiquid() {
        return liquid;
    }

    public float getLuminance() {
        return luminance;
    }

    /**
     * Findet VoxelType anhand der ID
     */
    public static VoxelType fromId(int id) {
        for (VoxelType type : values()) {
            if (type.id == id) {
                return type;
            }
        }
        return AIR;
    }

    /**
     * Erstellt ein neues Voxel dieses Typs
     */
    public Voxel createVoxel(int x, int y, int z) {
        return new Voxel(x, y, z, this);
    }

    /**
     * Prüft ob dieser Typ Licht emittiert
     */
    public boolean isLightSource() {
        return luminance > 0;
    }

    /**
     * Prüft ob dieser Typ transparent ist
     */
    public boolean isTransparent() {
        return this == AIR || this == WATER || this == LEAVES;
    }
}
