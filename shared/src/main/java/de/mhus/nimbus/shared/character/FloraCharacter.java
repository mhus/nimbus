package de.mhus.nimbus.shared.character;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.time.LocalDateTime;

/**
 * Flora character class for plant life and vegetation.
 * Contains plant-specific properties and growth behaviors.
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class FloraCharacter extends AbstractCharacter {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Plant types for different flora
     */
    public enum PlantType {
        TREE, BUSH, FLOWER, GRASS, VINE, MUSHROOM, CROP, HERB
    }

    /**
     * Growth stages of the plant
     */
    public enum GrowthStage {
        SEED, SPROUT, YOUNG, MATURE, OLD, DEAD
    }

    /**
     * Type of plant
     */
    private PlantType plantType = PlantType.GRASS;

    /**
     * Current growth stage
     */
    private GrowthStage growthStage = GrowthStage.SEED;

    /**
     * Size/scale of the plant (0.1 to 10.0)
     */
    private Double size = 1.0;

    /**
     * Growth rate (how fast it grows)
     */
    private Double growthRate = 1.0;

    /**
     * Whether the plant can be harvested
     */
    private boolean harvestable = false;

    /**
     * Number of times this plant can be harvested
     */
    private Integer maxHarvests = 1;

    /**
     * Current harvest count
     */
    private Integer harvestCount = 0;

    /**
     * Time when plant was last harvested
     */
    private LocalDateTime lastHarvestTime;

    /**
     * Regrowth time in hours after harvest
     */
    private Integer regrowthHours = 24;

    /**
     * Whether the plant spreads/reproduces
     */
    private boolean spreads = false;

    /**
     * Spread radius in blocks
     */
    private Double spreadRadius = 2.0;

    /**
     * Season preference (null = all seasons)
     */
    private String seasonPreference;

    /**
     * Constructor with essential fields
     */
    public FloraCharacter(String worldId, double x, double y, double z, PlantType plantType) {
        super(worldId, CharacterType.FLORA, x, y, z);
        this.plantType = plantType;
        this.name = plantType.name().toLowerCase();
        this.displayName = plantType.name();
        initializePlantDefaults();
    }

    /**
     * Constructor with growth stage
     */
    public FloraCharacter(String worldId, double x, double y, double z, PlantType plantType,
                         GrowthStage growthStage) {
        this(worldId, x, y, z, plantType);
        this.growthStage = growthStage;
        updateSizeForGrowthStage();
    }

    /**
     * Initialize default values based on plant type
     */
    private void initializePlantDefaults() {
        switch (plantType) {
            case TREE:
                this.health = 200;
                this.maxHealth = 200;
                this.size = 5.0;
                this.growthRate = 0.1;
                this.spreads = false;
                break;
            case BUSH:
                this.health = 75;
                this.maxHealth = 75;
                this.size = 2.0;
                this.harvestable = true;
                this.maxHarvests = 3;
                break;
            case FLOWER:
                this.health = 25;
                this.maxHealth = 25;
                this.size = 0.5;
                this.spreads = true;
                this.spreadRadius = 3.0;
                break;
            case GRASS:
                this.health = 10;
                this.maxHealth = 10;
                this.size = 0.3;
                this.spreads = true;
                this.spreadRadius = 1.5;
                break;
            case CROP:
                this.health = 50;
                this.maxHealth = 50;
                this.harvestable = true;
                this.maxHarvests = 1;
                this.regrowthHours = 0; // Crops don't regrow
                break;
            default:
                this.health = 50;
                this.maxHealth = 50;
                break;
        }
    }

    /**
     * Updates size based on growth stage
     */
    private void updateSizeForGrowthStage() {
        double baseSize = size;
        switch (growthStage) {
            case SEED -> size = baseSize * 0.1;
            case SPROUT -> size = baseSize * 0.3;
            case YOUNG -> size = baseSize * 0.6;
            case MATURE -> size = baseSize * 1.0;
            case OLD -> size = baseSize * 1.2;
            case DEAD -> size = baseSize * 0.8;
        }
    }

    /**
     * Grows the plant to next stage
     */
    public boolean grow() {
        if (growthStage == GrowthStage.DEAD) {
            return false;
        }

        GrowthStage[] stages = GrowthStage.values();
        int currentIndex = growthStage.ordinal();
        if (currentIndex < stages.length - 1) {
            growthStage = stages[currentIndex + 1];
            updateSizeForGrowthStage();
            return true;
        }
        return false;
    }

    /**
     * Harvests the plant if possible
     */
    public boolean harvest() {
        if (!harvestable || harvestCount >= maxHarvests) {
            return false;
        }

        harvestCount++;
        lastHarvestTime = LocalDateTime.now();

        // If max harvests reached and no regrowth, plant dies
        if (harvestCount >= maxHarvests && regrowthHours == 0) {
            growthStage = GrowthStage.DEAD;
            active = false;
        }

        return true;
    }

    /**
     * Checks if plant can be harvested again
     */
    public boolean canHarvest() {
        if (!harvestable || harvestCount >= maxHarvests) {
            return false;
        }

        if (lastHarvestTime != null && regrowthHours > 0) {
            return lastHarvestTime.plusHours(regrowthHours).isBefore(LocalDateTime.now());
        }

        return true;
    }

    /**
     * Checks if plant is fully mature
     */
    public boolean isMature() {
        return growthStage == GrowthStage.MATURE || growthStage == GrowthStage.OLD;
    }

    @Override
    public String getTypeSpecificInfo() {
        return String.format("Flora[type=%s, stage=%s, size=%.1f, harvestable=%s, harvestCount=%d/%s]",
                           plantType, growthStage, size, harvestable, harvestCount,
                           maxHarvests != null ? maxHarvests.toString() : "∞");
    }
}
