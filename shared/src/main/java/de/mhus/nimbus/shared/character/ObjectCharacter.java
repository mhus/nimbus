package de.mhus.nimbus.shared.character;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.HashMap;

/**
 * Object character class for interactive environmental objects and items.
 * Contains object-specific properties and behaviors.
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ObjectCharacter extends AbstractCharacter {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Types of interactive objects
     */
    public enum ObjectType {
        CONTAINER, DOOR, SWITCH, LEVER, BUTTON, CHEST, ALTAR, STATUE, FOUNTAIN, CRYSTAL, PORTAL, MACHINE
    }

    /**
     * Object states
     */
    public enum ObjectState {
        INACTIVE, ACTIVE, LOCKED, BROKEN, HIDDEN, TRIGGERED
    }

    /**
     * Type of object
     */
    private ObjectType objectType = ObjectType.CONTAINER;

    /**
     * Current state of the object
     */
    private ObjectState objectState = ObjectState.INACTIVE;

    /**
     * Whether the object can be interacted with
     */
    private boolean interactable = true;

    /**
     * Whether the object requires a key or permission
     */
    private boolean requiresKey = false;

    /**
     * Key ID required for interaction
     */
    private String requiredKeyId;

    /**
     * Maximum number of uses (null = unlimited)
     */
    private Integer maxUses;

    /**
     * Current use count
     */
    private Integer useCount = 0;

    /**
     * Cooldown time in seconds between uses
     */
    private Integer cooldownSeconds = 0;

    /**
     * Last interaction time
     */
    private LocalDateTime lastInteractionTime;

    /**
     * Whether the object respawns after being destroyed
     */
    private boolean respawns = false;

    /**
     * Respawn time in seconds
     */
    private Integer respawnTime = 300;

    /**
     * Object's durability
     */
    private Integer durability = 100;

    /**
     * Maximum durability
     */
    private Integer maxDurability = 100;

    /**
     * Size/scale of the object
     */
    private Double size = 1.0;

    /**
     * Whether the object can be moved
     */
    private boolean moveable = false;

    /**
     * Weight of the object (affects moveability)
     */
    private Double weight = 50.0;

    /**
     * Object properties for specific behaviors
     */
    private Map<String, Object> objectProperties = new HashMap<>();

    /**
     * Loot table or contents (for containers)
     */
    private Map<String, Integer> contents = new HashMap<>();

    /**
     * Constructor with essential fields
     */
    public ObjectCharacter(String worldId, double x, double y, double z, ObjectType objectType) {
        super(worldId, CharacterType.OBJECT, x, y, z);
        this.objectType = objectType;
        this.name = objectType.name().toLowerCase();
        this.displayName = objectType.name();
        initializeObjectDefaults();
    }

    /**
     * Constructor with state
     */
    public ObjectCharacter(String worldId, double x, double y, double z, ObjectType objectType,
                          ObjectState objectState) {
        this(worldId, x, y, z, objectType);
        this.objectState = objectState;
    }

    /**
     * Constructor with custom name
     */
    public ObjectCharacter(String worldId, double x, double y, double z, ObjectType objectType,
                          String name, String displayName) {
        super(worldId, CharacterType.OBJECT, x, y, z, name, displayName);
        this.objectType = objectType;
        initializeObjectDefaults();
    }

    /**
     * Initialize default values based on object type
     */
    private void initializeObjectDefaults() {
        switch (objectType) {
            case CONTAINER, CHEST:
                this.health = 100;
                this.maxHealth = 100;
                this.durability = 80;
                this.maxDurability = 80;
                this.interactable = true;
                this.size = 1.0;
                this.weight = 100.0;
                break;
            case DOOR:
                this.health = 150;
                this.maxHealth = 150;
                this.durability = 120;
                this.maxDurability = 120;
                this.size = 2.0;
                this.weight = 200.0;
                break;
            case SWITCH, LEVER, BUTTON:
                this.health = 50;
                this.maxHealth = 50;
                this.durability = 60;
                this.maxDurability = 60;
                this.size = 0.5;
                this.weight = 10.0;
                this.cooldownSeconds = 5;
                break;
            case ALTAR:
                this.health = 200;
                this.maxHealth = 200;
                this.durability = 180;
                this.maxDurability = 180;
                this.size = 2.5;
                this.weight = 500.0;
                this.requiresKey = true;
                break;
            case STATUE:
                this.health = 300;
                this.maxHealth = 300;
                this.durability = 250;
                this.maxDurability = 250;
                this.size = 3.0;
                this.weight = 1000.0;
                this.interactable = false;
                break;
            case FOUNTAIN:
                this.health = 200;
                this.maxHealth = 200;
                this.durability = 150;
                this.maxDurability = 150;
                this.size = 3.0;
                this.weight = 800.0;
                this.cooldownSeconds = 60;
                break;
            case CRYSTAL:
                this.health = 80;
                this.maxHealth = 80;
                this.durability = 40;
                this.maxDurability = 40;
                this.size = 1.5;
                this.weight = 30.0;
                this.respawns = true;
                this.respawnTime = 1800; // 30 minutes
                break;
            case PORTAL:
                this.health = 500;
                this.maxHealth = 500;
                this.durability = 400;
                this.maxDurability = 400;
                this.size = 4.0;
                this.weight = 0.0; // Magical, no physical weight
                this.requiresKey = true;
                break;
            case MACHINE:
                this.health = 120;
                this.maxHealth = 120;
                this.durability = 100;
                this.maxDurability = 100;
                this.size = 2.0;
                this.weight = 300.0;
                this.maxUses = 100;
                break;
            default:
                this.health = 100;
                this.maxHealth = 100;
                this.durability = 80;
                this.maxDurability = 80;
                break;
        }
    }

    /**
     * Interacts with the object
     */
    public boolean interact(String playerId, String keyId) {
        if (!canInteract()) {
            return false;
        }

        // Check if key is required and provided
        if (requiresKey && !keyId.equals(requiredKeyId)) {
            return false;
        }

        // Check cooldown
        if (isOnCooldown()) {
            return false;
        }

        // Check max uses
        if (maxUses != null && useCount >= maxUses) {
            return false;
        }

        // Perform interaction
        useCount++;
        lastInteractionTime = LocalDateTime.now();

        // Update state based on object type
        switch (objectType) {
            case DOOR:
                objectState = (objectState == ObjectState.ACTIVE) ?
                    ObjectState.INACTIVE : ObjectState.ACTIVE;
                break;
            case SWITCH, LEVER:
                objectState = ObjectState.TRIGGERED;
                break;
            case BUTTON:
                objectState = ObjectState.TRIGGERED;
                // Buttons reset after a short time
                break;
            case CONTAINER, CHEST:
                objectState = ObjectState.ACTIVE;
                break;
        }

        return true;
    }

    /**
     * Checks if object can be interacted with
     */
    public boolean canInteract() {
        return interactable &&
               objectState != ObjectState.BROKEN &&
               objectState != ObjectState.HIDDEN &&
               active;
    }

    /**
     * Checks if object is on cooldown
     */
    public boolean isOnCooldown() {
        if (cooldownSeconds == 0 || lastInteractionTime == null) {
            return false;
        }

        return lastInteractionTime.plusSeconds(cooldownSeconds).isAfter(LocalDateTime.now());
    }

    /**
     * Adds an item to the object's contents (for containers)
     */
    public void addContent(String itemId, int quantity) {
        contents.put(itemId, contents.getOrDefault(itemId, 0) + quantity);
    }

    /**
     * Removes an item from the object's contents
     */
    public boolean removeContent(String itemId, int quantity) {
        int currentAmount = contents.getOrDefault(itemId, 0);
        if (currentAmount >= quantity) {
            if (currentAmount == quantity) {
                contents.remove(itemId);
            } else {
                contents.put(itemId, currentAmount - quantity);
            }
            return true;
        }
        return false;
    }

    /**
     * Gets the quantity of a specific item in contents
     */
    public int getContentQuantity(String itemId) {
        return contents.getOrDefault(itemId, 0);
    }

    /**
     * Sets an object property
     */
    public void setObjectProperty(String key, Object value) {
        objectProperties.put(key, value);
    }

    /**
     * Gets an object property
     */
    public Object getObjectProperty(String key) {
        return objectProperties.get(key);
    }

    /**
     * Repairs the object
     */
    public void repair(int amount) {
        durability = Math.min(durability + amount, maxDurability);
        health = Math.min(health + amount, maxHealth);

        if (durability > 50 && objectState == ObjectState.BROKEN) {
            objectState = ObjectState.INACTIVE;
        }
    }

    /**
     * Damages the object
     */
    public void takeDamage(int damage) {
        durability -= damage;
        health -= damage;

        if (durability <= 0) {
            objectState = ObjectState.BROKEN;
            interactable = false;
        }

        if (health <= 0) {
            active = false;
            if (respawns) {
                // Mark for respawn
                objectProperties.put("respawnTime", LocalDateTime.now().plusSeconds(respawnTime));
            }
        }
    }

    /**
     * Resets the object to its initial state
     */
    public void reset() {
        objectState = ObjectState.INACTIVE;
        useCount = 0;
        lastInteractionTime = null;
        health = maxHealth;
        durability = maxDurability;
        interactable = true;
        active = true;
    }

    /**
     * Checks if object should respawn
     */
    public boolean shouldRespawn() {
        if (!respawns || active) {
            return false;
        }

        LocalDateTime respawnTime = (LocalDateTime) objectProperties.get("respawnTime");
        return respawnTime != null && respawnTime.isBefore(LocalDateTime.now());
    }

    @Override
    public String getTypeSpecificInfo() {
        return String.format("Object[type=%s, state=%s, interactable=%s, uses=%d/%s, durability=%d/%d]",
                           objectType, objectState, interactable, useCount,
                           maxUses != null ? maxUses.toString() : "∞", durability, maxDurability);
    }
}
