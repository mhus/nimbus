package de.mhus.nimbus.shared.character;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.joml.Vector3d;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Abstract base class for all character types in the Nimbus platform.
 * Contains common properties and behaviors shared across different character implementations.
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public abstract class AbstractCharacter implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Unique identifier for the character
     */
    @EqualsAndHashCode.Include
    protected Long id;

    /**
     * World identifier - references the world this character belongs to
     */
    @EqualsAndHashCode.Include
    protected String worldId;

    /**
     * Type of character (PLAYER, NPC, FLORA, FAUNA)
     */
    @EqualsAndHashCode.Include
    protected CharacterType characterType;

    /**
     * World coordinates of the character
     */
    @EqualsAndHashCode.Include
    protected double x;

    @EqualsAndHashCode.Include
    protected double y;

    @EqualsAndHashCode.Include
    protected double z;

    /**
     * Internal name or identifier for the character (system use)
     */
    protected String name;

    /**
     * Display name for the character (shown to other players/users)
     */
    protected String displayName;

    /**
     * Optional description or additional information
     */
    protected String description;

    /**
     * Health points (if applicable)
     */
    protected Integer health;

    /**
     * Maximum health points (if applicable)
     */
    protected Integer maxHealth;

    /**
     * Whether the character is currently active/alive
     */
    protected boolean active = true;

    /**
     * Timestamps for tracking creation and changes
     */
    protected LocalDateTime createdAt;

    protected LocalDateTime lastModified;

    /**
     * Constructor with essential fields
     *
     * @param worldId       The world identifier
     * @param characterType The type of character
     * @param x             X coordinate
     * @param y             Y coordinate
     * @param z             Z coordinate
     */
    public AbstractCharacter(String worldId, CharacterType characterType, double x, double y, double z) {
        this.worldId = worldId;
        this.characterType = characterType;
        this.x = x;
        this.y = y;
        this.z = z;
        this.active = true;
        this.createdAt = LocalDateTime.now();
        this.lastModified = LocalDateTime.now();
    }

    /**
     * Constructor with name
     *
     * @param worldId       The world identifier
     * @param characterType The type of character
     * @param x             X coordinate
     * @param y             Y coordinate
     * @param z             Z coordinate
     * @param name          Character name
     */
    public AbstractCharacter(String worldId, CharacterType characterType, double x, double y, double z, String name) {
        this(worldId, characterType, x, y, z);
        this.name = name;
    }

    /**
     * Constructor with name and display name
     *
     * @param worldId       The world identifier
     * @param characterType The type of character
     * @param x             X coordinate
     * @param y             Y coordinate
     * @param z             Z coordinate
     * @param name          Character name
     * @param displayName   Character display name
     */
    public AbstractCharacter(String worldId, CharacterType characterType, double x, double y, double z,
                           String name, String displayName) {
        this(worldId, characterType, x, y, z, name);
        this.displayName = displayName;
    }

    /**
     * Updates the position of the character
     *
     * @param x New X coordinate
     * @param y New Y coordinate
     * @param z New Z coordinate
     */
    public void updatePosition(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
        touch();
    }

    /**
     * Updates the position using a Vector3d
     *
     * @param position New position vector
     */
    public void updatePosition(Vector3d position) {
        updatePosition(position.x, position.y, position.z);
    }

    /**
     * Gets the position as a Vector3d
     *
     * @return Position vector
     */
    public Vector3d getPosition() {
        return new Vector3d(x, y, z);
    }

    /**
     * Updates the health of the character
     *
     * @param health New health value
     */
    public void updateHealth(int health) {
        this.health = Math.max(0, health); // Ensure health doesn't go below 0
        touch();

        if (this.health == 0) {
            this.active = false;
        }
    }

    /**
     * Heals the character
     *
     * @param healAmount Amount to heal
     */
    public void heal(int healAmount) {
        if (this.health != null && this.maxHealth != null) {
            this.health = Math.min(this.maxHealth, this.health + healAmount);
            touch();

            // Reactivate character if health is restored and was previously dead
            if (this.health > 0 && !this.active) {
                this.active = true;
            }
        }
    }

    /**
     * Damages the character
     *
     * @param damage Amount of damage
     */
    public void takeDamage(int damage) {
        if (this.health != null) {
            updateHealth(this.health - damage);
        }
    }

    /**
     * Sets the maximum health and initializes current health if not set
     *
     * @param maxHealth Maximum health value
     */
    public void setMaxHealth(int maxHealth) {
        this.maxHealth = maxHealth;
        if (this.health == null) {
            this.health = maxHealth;
        }
        touch();
    }

    /**
     * Checks if the character is alive
     *
     * @return true if character is active and has health > 0
     */
    public boolean isAlive() {
        return active && (health == null || health > 0);
    }

    /**
     * Checks if the character is dead
     *
     * @return true if character is not active or health is 0
     */
    public boolean isDead() {
        return !isAlive();
    }

    /**
     * Gets the health percentage (0.0 to 1.0)
     *
     * @return Health percentage or 1.0 if health is not applicable
     */
    public double getHealthPercentage() {
        if (health == null || maxHealth == null || maxHealth == 0) {
            return 1.0;
        }
        return (double) health / maxHealth;
    }

    /**
     * Calculates distance to another character
     *
     * @param other Other character
     * @return Distance between characters, or Double.MAX_VALUE if in different worlds
     */
    public double distanceTo(AbstractCharacter other) {
        if (other == null || !this.worldId.equals(other.worldId)) {
            return Double.MAX_VALUE;
        }

        double dx = this.x - other.x;
        double dy = this.y - other.y;
        double dz = this.z - other.z;

        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    /**
     * Checks if another character is within a certain range
     *
     * @param other Other character
     * @param range Maximum distance
     * @return true if other character is within range
     */
    public boolean isWithinRange(AbstractCharacter other, double range) {
        return distanceTo(other) <= range;
    }

    /**
     * Gets a string representation of the position
     *
     * @return Position as "x,y,z"
     */
    public String getPositionString() {
        return String.format("%.2f,%.2f,%.2f", x, y, z);
    }

    /**
     * Gets the effective name for display (displayName if available, otherwise name)
     *
     * @return Display name or name
     */
    public String getEffectiveDisplayName() {
        return displayName != null && !displayName.trim().isEmpty() ? displayName : name;
    }

    /**
     * Updates the last modified timestamp
     */
    public void touch() {
        this.lastModified = LocalDateTime.now();
    }

    /**
     * Creates a summary string for the character
     *
     * @return Character summary
     */
    public String getSummary() {
        return String.format("%s '%s' (%s) at %s in world %s",
                           characterType.getDisplayName(),
                           getEffectiveDisplayName(),
                           isAlive() ? "alive" : "dead",
                           getPositionString(),
                           worldId);
    }

    /**
     * Abstract method to be implemented by subclasses for type-specific behavior
     *
     * @return Type-specific information about the character
     */
    public abstract String getTypeSpecificInfo();
}
