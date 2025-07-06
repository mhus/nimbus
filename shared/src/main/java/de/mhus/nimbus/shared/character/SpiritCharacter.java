package de.mhus.nimbus.shared.character;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.HashMap;

/**
 * Spirit character class for ghostly, spiritual, and otherworldly beings.
 * Contains spirit-specific properties and supernatural behaviors.
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class SpiritCharacter extends AbstractCharacter {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Types of spiritual entities
     */
    public enum SpiritType {
        GHOST, PHANTOM, WRAITH, SPECTER, ELEMENTAL, GUARDIAN, ANCESTOR, DEMON, ANGEL, WISP
    }

    /**
     * Spiritual manifestation states
     */
    public enum ManifestationState {
        INVISIBLE, TRANSLUCENT, SEMI_SOLID, MANIFESTED, BANISHED, DORMANT
    }

    /**
     * Spiritual alignment
     */
    public enum SpiritAlignment {
        BENEVOLENT, NEUTRAL, MALEVOLENT, CHAOTIC, PROTECTIVE, VENGEFUL
    }

    /**
     * Type of spirit
     */
    private SpiritType spiritType = SpiritType.GHOST;

    /**
     * Current manifestation state
     */
    private ManifestationState manifestationState = ManifestationState.TRANSLUCENT;

    /**
     * Spiritual alignment
     */
    private SpiritAlignment alignment = SpiritAlignment.NEUTRAL;

    /**
     * Spiritual energy level (0-100)
     */
    private Integer spiritualEnergy = 100;

    /**
     * Maximum spiritual energy
     */
    private Integer maxSpiritualEnergy = 100;

    /**
     * Energy regeneration rate per hour
     */
    private Integer energyRegenRate = 10;

    /**
     * Whether the spirit can pass through solid objects
     */
    private boolean incorporeal = true;

    /**
     * Whether the spirit can be seen by living beings
     */
    private boolean visible = true;

    /**
     * Whether the spirit can interact with physical objects
     */
    private boolean canInteractPhysically = false;

    /**
     * Haunting range - area of influence
     */
    private Double hauntingRange = 15.0;

    /**
     * Connection to the physical world (0-100)
     */
    private Integer physicalConnection = 30;

    /**
     * Age of the spirit (in years since death/creation)
     */
    private Integer spiritAge = 0;

    /**
     * Reason for spiritual existence
     */
    private String purpose;

    /**
     * Anchoring object or location ID
     */
    private String anchorId;

    /**
     * Time when spirit was created/died
     */
    private LocalDateTime deathTime;

    /**
     * Whether spirit fades over time
     */
    private boolean fades = true;

    /**
     * Fade rate per day (spiritual energy lost)
     */
    private Integer fadeRate = 1;

    /**
     * Spiritual abilities
     */
    private Map<String, Integer> abilities = new HashMap<>();

    /**
     * Memories or knowledge possessed
     */
    private Map<String, Object> memories = new HashMap<>();

    /**
     * Constructor with essential fields
     */
    public SpiritCharacter(String worldId, double x, double y, double z, SpiritType spiritType) {
        super(worldId, CharacterType.SPIRIT, x, y, z);
        this.spiritType = spiritType;
        this.name = spiritType.name().toLowerCase();
        this.displayName = spiritType.name();
        this.deathTime = LocalDateTime.now();
        initializeSpiritDefaults();
    }

    /**
     * Constructor with alignment
     */
    public SpiritCharacter(String worldId, double x, double y, double z, SpiritType spiritType,
                          SpiritAlignment alignment) {
        this(worldId, x, y, z, spiritType);
        this.alignment = alignment;
        adjustForAlignment();
    }

    /**
     * Constructor with purpose
     */
    public SpiritCharacter(String worldId, double x, double y, double z, SpiritType spiritType,
                          SpiritAlignment alignment, String purpose) {
        this(worldId, x, y, z, spiritType, alignment);
        this.purpose = purpose;
    }

    /**
     * Initialize default values based on spirit type
     */
    private void initializeSpiritDefaults() {
        switch (spiritType) {
            case GHOST:
                this.health = 60;
                this.maxHealth = 60;
                this.spiritualEnergy = 80;
                this.maxSpiritualEnergy = 80;
                this.incorporeal = true;
                this.visible = true;
                this.physicalConnection = 20;
                this.abilities.put("haunt", 70);
                this.abilities.put("possession", 30);
                break;
            case PHANTOM:
                this.health = 40;
                this.maxHealth = 40;
                this.spiritualEnergy = 100;
                this.maxSpiritualEnergy = 100;
                this.incorporeal = true;
                this.visible = false;
                this.physicalConnection = 10;
                this.abilities.put("invisibility", 90);
                this.abilities.put("fear", 60);
                break;
            case WRAITH:
                this.health = 80;
                this.maxHealth = 80;
                this.spiritualEnergy = 120;
                this.maxSpiritualEnergy = 120;
                this.alignment = SpiritAlignment.MALEVOLENT;
                this.abilities.put("drain_life", 80);
                this.abilities.put("curse", 70);
                break;
            case ELEMENTAL:
                this.health = 100;
                this.maxHealth = 100;
                this.spiritualEnergy = 150;
                this.maxSpiritualEnergy = 150;
                this.incorporeal = false;
                this.canInteractPhysically = true;
                this.physicalConnection = 70;
                this.fades = false;
                this.abilities.put("elemental_control", 90);
                break;
            case GUARDIAN:
                this.health = 120;
                this.maxHealth = 120;
                this.spiritualEnergy = 100;
                this.maxSpiritualEnergy = 100;
                this.alignment = SpiritAlignment.PROTECTIVE;
                this.fades = false;
                this.abilities.put("protection", 80);
                this.abilities.put("banish_evil", 70);
                break;
            case ANCESTOR:
                this.health = 90;
                this.maxHealth = 90;
                this.spiritualEnergy = 110;
                this.maxSpiritualEnergy = 110;
                this.alignment = SpiritAlignment.BENEVOLENT;
                this.abilities.put("wisdom", 90);
                this.abilities.put("guidance", 80);
                break;
            case WISP:
                this.health = 20;
                this.maxHealth = 20;
                this.spiritualEnergy = 60;
                this.maxSpiritualEnergy = 60;
                this.hauntingRange = 5.0;
                this.abilities.put("illuminate", 80);
                this.abilities.put("misdirect", 60);
                break;
            default:
                this.health = 70;
                this.maxHealth = 70;
                this.spiritualEnergy = 100;
                this.maxSpiritualEnergy = 100;
                break;
        }
    }

    /**
     * Adjust properties based on alignment
     */
    private void adjustForAlignment() {
        switch (alignment) {
            case BENEVOLENT:
                this.energyRegenRate = (int)(energyRegenRate * 1.2);
                this.fadeRate = (int)(fadeRate * 0.8);
                break;
            case MALEVOLENT:
                this.spiritualEnergy = (int)(spiritualEnergy * 1.3);
                this.maxSpiritualEnergy = (int)(maxSpiritualEnergy * 1.3);
                this.hauntingRange *= 1.5;
                break;
            case PROTECTIVE:
                this.health = (int)(health * 1.2);
                this.maxHealth = (int)(maxHealth * 1.2);
                this.fades = false;
                break;
            case VENGEFUL:
                this.spiritualEnergy = (int)(spiritualEnergy * 1.4);
                this.maxSpiritualEnergy = (int)(maxSpiritualEnergy * 1.4);
                this.fadeRate = 0; // Vengeance sustains them
                break;
        }
    }

    /**
     * Manifests the spirit more solidly
     */
    public boolean manifest() {
        if (spiritualEnergy < 20) {
            return false;
        }

        switch (manifestationState) {
            case INVISIBLE:
                manifestationState = ManifestationState.TRANSLUCENT;
                visible = true;
                break;
            case TRANSLUCENT:
                manifestationState = ManifestationState.SEMI_SOLID;
                canInteractPhysically = true;
                break;
            case SEMI_SOLID:
                manifestationState = ManifestationState.MANIFESTED;
                incorporeal = false;
                physicalConnection = Math.min(physicalConnection + 20, 100);
                break;
            default:
                return false;
        }

        spiritualEnergy -= 20;
        return true;
    }

    /**
     * De-manifests the spirit
     */
    public void dematerialize() {
        switch (manifestationState) {
            case MANIFESTED:
                manifestationState = ManifestationState.SEMI_SOLID;
                incorporeal = true;
                break;
            case SEMI_SOLID:
                manifestationState = ManifestationState.TRANSLUCENT;
                canInteractPhysically = false;
                break;
            case TRANSLUCENT:
                manifestationState = ManifestationState.INVISIBLE;
                visible = false;
                break;
        }

        spiritualEnergy += 10; // Costs less energy to be less solid
    }

    /**
     * Uses a spiritual ability
     */
    public boolean useAbility(String abilityName, int energyCost) {
        if (!abilities.containsKey(abilityName) || spiritualEnergy < energyCost) {
            return false;
        }

        spiritualEnergy -= energyCost;

        // Reduce energy if spirit is too weak
        if (spiritualEnergy <= 0) {
            manifestationState = ManifestationState.DORMANT;
            visible = false;
            canInteractPhysically = false;
        }

        return true;
    }

    /**
     * Restores spiritual energy
     */
    public void restoreEnergy(int amount) {
        spiritualEnergy = Math.min(spiritualEnergy + amount, maxSpiritualEnergy);

        if (spiritualEnergy > 20 && manifestationState == ManifestationState.DORMANT) {
            manifestationState = ManifestationState.TRANSLUCENT;
            visible = true;
        }
    }

    /**
     * Ages the spirit (called periodically)
     */
    public void age() {
        spiritAge++;

        if (fades && fadeRate > 0) {
            spiritualEnergy -= fadeRate;
            maxSpiritualEnergy -= fadeRate;

            if (maxSpiritualEnergy <= 0) {
                // Spirit fades away completely
                active = false;
                manifestationState = ManifestationState.BANISHED;
            }
        }
    }

    /**
     * Anchors the spirit to an object or location
     */
    public void anchor(String anchorId) {
        this.anchorId = anchorId;
        this.fades = false; // Anchored spirits don't fade
        this.physicalConnection += 20;
    }

    /**
     * Removes the anchor
     */
    public void removeAnchor() {
        this.anchorId = null;
        this.fades = true;
        this.physicalConnection = Math.max(physicalConnection - 20, 0);
    }

    /**
     * Adds a memory
     */
    public void addMemory(String key, Object memory) {
        memories.put(key, memory);
    }

    /**
     * Retrieves a memory
     */
    public Object getMemory(String key) {
        return memories.get(key);
    }

    /**
     * Sets an ability level
     */
    public void setAbility(String abilityName, int level) {
        abilities.put(abilityName, level);
    }

    /**
     * Gets an ability level
     */
    public int getAbilityLevel(String abilityName) {
        return abilities.getOrDefault(abilityName, 0);
    }

    /**
     * Checks if spirit is within haunting range of target
     */
    public boolean isInHauntingRange(double targetX, double targetY, double targetZ) {
        double distance = Math.sqrt(
            Math.pow(x - targetX, 2) +
            Math.pow(y - targetY, 2) +
            Math.pow(z - targetZ, 2)
        );
        return distance <= hauntingRange;
    }

    /**
     * Checks if spirit can be seen by living beings
     */
    public boolean isVisibleToLiving() {
        return visible && manifestationState != ManifestationState.INVISIBLE &&
               manifestationState != ManifestationState.DORMANT;
    }

    /**
     * Checks if spirit can interact with the physical world
     */
    public boolean canInteractWithPhysical() {
        return canInteractPhysically && manifestationState != ManifestationState.DORMANT;
    }

    /**
     * Banishes the spirit
     */
    public void banish() {
        manifestationState = ManifestationState.BANISHED;
        active = false;
        visible = false;
        canInteractPhysically = false;
    }

    @Override
    public String getTypeSpecificInfo() {
        return String.format("Spirit[type=%s, alignment=%s, manifestation=%s, energy=%d/%d, age=%d, anchored=%s]",
                           spiritType, alignment, manifestationState, spiritualEnergy, maxSpiritualEnergy,
                           spiritAge, anchorId != null);
    }
}
