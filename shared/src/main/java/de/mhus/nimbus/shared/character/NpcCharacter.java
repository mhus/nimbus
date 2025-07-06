package de.mhus.nimbus.shared.character;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.util.Map;
import java.util.HashMap;

/**
 * Non-Player Character class for AI-controlled characters.
 * Contains NPC-specific properties and behaviors.
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class NpcCharacter extends AbstractCharacter {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * NPC's behavioral pattern
     */
    public enum BehaviorType {
        PASSIVE, AGGRESSIVE, NEUTRAL, FRIENDLY, GUARD, MERCHANT, QUESTGIVER
    }

    /**
     * AI behavior type
     */
    private BehaviorType behaviorType = BehaviorType.NEUTRAL;

    /**
     * NPC's level or difficulty rating
     */
    private Integer level = 1;

    /**
     * Detection range for interactions
     */
    private Double detectionRange = 5.0;

    /**
     * Movement speed
     */
    private Double movementSpeed = 1.0;

    /**
     * Whether NPC can move or is stationary
     */
    private boolean canMove = true;

    /**
     * Whether NPC respawns after being destroyed
     */
    private boolean respawns = true;

    /**
     * Respawn time in seconds
     */
    private Integer respawnTime = 300; // 5 minutes

    /**
     * Custom properties for specific NPC behaviors
     */
    private Map<String, Object> customProperties = new HashMap<>();

    /**
     * Constructor with essential fields
     */
    public NpcCharacter(String worldId, double x, double y, double z, String name) {
        super(worldId, CharacterType.NPC, x, y, z, name);
        this.health = 50;
        this.maxHealth = 50;
    }

    /**
     * Constructor with behavior type
     */
    public NpcCharacter(String worldId, double x, double y, double z, String name,
                       BehaviorType behaviorType) {
        this(worldId, x, y, z, name);
        this.behaviorType = behaviorType;
    }

    /**
     * Constructor with full configuration
     */
    public NpcCharacter(String worldId, double x, double y, double z, String name,
                       String displayName, BehaviorType behaviorType, Integer level) {
        super(worldId, CharacterType.NPC, x, y, z, name, displayName);
        this.behaviorType = behaviorType;
        this.level = level;
        this.health = level * 25; // Health scales with level
        this.maxHealth = level * 25;
    }

    /**
     * Adds a custom property for this NPC
     */
    public void setCustomProperty(String key, Object value) {
        customProperties.put(key, value);
    }

    /**
     * Gets a custom property for this NPC
     */
    public Object getCustomProperty(String key) {
        return customProperties.get(key);
    }

    /**
     * Checks if NPC is hostile based on behavior type
     */
    public boolean isHostile() {
        return behaviorType == BehaviorType.AGGRESSIVE;
    }

    /**
     * Checks if NPC can interact with players
     */
    public boolean canInteract() {
        return behaviorType == BehaviorType.FRIENDLY ||
               behaviorType == BehaviorType.MERCHANT ||
               behaviorType == BehaviorType.QUESTGIVER;
    }

    /**
     * Calculates if target is within detection range
     */
    public boolean isInDetectionRange(double targetX, double targetY, double targetZ) {
        double distance = Math.sqrt(
            Math.pow(x - targetX, 2) +
            Math.pow(y - targetY, 2) +
            Math.pow(z - targetZ, 2)
        );
        return distance <= detectionRange;
    }

    @Override
    public String getTypeSpecificInfo() {
        return String.format("NPC[behavior=%s, level=%d, detectionRange=%.1f, canMove=%s]",
                           behaviorType, level, detectionRange, canMove);
    }
}
