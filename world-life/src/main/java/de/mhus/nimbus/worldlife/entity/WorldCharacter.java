package de.mhus.nimbus.worldlife.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.mhus.nimbus.shared.character.AbstractCharacter;
import de.mhus.nimbus.shared.character.CharacterType;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;

/**
 * JPA Entity for storing world characters (players, NPCs, flora, fauna)
 * This entity represents living entities in the world with their position and type
 */
@Entity
@Table(name = "world_characters",
       indexes = {
           @Index(name = "idx_world_character_position", columnList = "worldId, x, y, z"),
           @Index(name = "idx_world_character_world", columnList = "worldId"),
           @Index(name = "idx_world_character_type", columnList = "characterType"),
           @Index(name = "idx_world_character_world_type", columnList = "worldId, characterType"),
           @Index(name = "idx_world_character_created", columnList = "createdAt")
       },
       uniqueConstraints = {
           @UniqueConstraint(name = "uk_world_character_position", columnNames = {"worldId", "x", "y", "z", "characterType"})
       })
@Data
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString
public class WorldCharacter {

    private static final Logger LOGGER = LoggerFactory.getLogger(WorldCharacter.class);

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    /**
     * World identifier - references the world this character belongs to
     */
    @Column(nullable = false, length = 255)
    @EqualsAndHashCode.Include
    private String worldId;

    /**
     * Type of character (PLAYER, NPC, FLORA, FAUNA)
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @EqualsAndHashCode.Include
    private CharacterType characterType;

    /**
     * World coordinates of the character
     */
    @Column(nullable = false)
    @EqualsAndHashCode.Include
    private double x;

    @Column(nullable = false)
    @EqualsAndHashCode.Include
    private double y;

    @Column(nullable = false)
    @EqualsAndHashCode.Include
    private double z;

    /**
     * Optional name or identifier for the character
     */
    @Column(length = 255)
    private String name;

    /**
     * Display name for the character (shown to other players)
     */
    @Column(length = 255)
    private String displayName;

    /**
     * Optional description or additional information
     */
    @Column(length = 1000)
    private String description;

    /**
     * Health points (if applicable)
     */
    @Column
    private Integer health;

    /**
     * Maximum health points (if applicable)
     */
    @Column
    private Integer maxHealth;

    /**
     * Whether the character is currently active/alive
     */
    @Column(nullable = false)
    private boolean active = true;

    /**
     * Character data stored as JSON (AbstractCharacter implementations)
     */
    @Lob
    @Column(name = "character_data", columnDefinition = "TEXT")
    private String characterData;

    /**
     * Timestamps for tracking creation and changes
     */
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime lastModified;

    /**
     * Version for optimistic locking
     */
    @Version
    private Long version;

    /**
     * Transient field for the actual character object
     */
    @Transient
    @JsonIgnore
    private AbstractCharacter character;

    /**
     * Static ObjectMapper for JSON operations
     */
    @Transient
    @JsonIgnore
    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Constructor with essential fields
     *
     * @param worldId       The world identifier
     * @param characterType The type of character
     * @param x             X coordinate
     * @param y             Y coordinate
     * @param z             Z coordinate
     */
    public WorldCharacter(String worldId, CharacterType characterType, double x, double y, double z) {
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
     * @param y             Y Coordinate
     * @param z             Z coordinate
     * @param name          Character name
     */
    public WorldCharacter(String worldId, CharacterType characterType, double x, double y, double z, String name) {
        this(worldId, characterType, x, y, z);
        this.name = name;
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
        this.lastModified = LocalDateTime.now();

        LOGGER.debug("Updated position for character {} in world {} to ({}, {}, {})",
                    id, worldId, x, y, z);
    }

    /**
     * Updates the health of the character
     *
     * @param health New health value
     */
    public void updateHealth(int health) {
        this.health = Math.max(0, health); // Ensure health doesn't go below 0
        this.lastModified = LocalDateTime.now();

        if (this.health == 0) {
            this.active = false;
            LOGGER.info("Character {} in world {} has died (health reached 0)", id, worldId);
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
            this.lastModified = LocalDateTime.now();

            LOGGER.debug("Healed character {} in world {} by {} points, new health: {}",
                        id, worldId, healAmount, this.health);
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
            LOGGER.debug("Character {} in world {} took {} damage, remaining health: {}",
                        id, worldId, damage, this.health);
        }
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
     * Calculates distance to another character
     *
     * @param other Other character
     * @return Distance between characters
     */
    public double distanceTo(WorldCharacter other) {
        if (other == null || !this.worldId.equals(other.worldId)) {
            return Double.MAX_VALUE;
        }

        double dx = this.x - other.x;
        double dy = this.y - other.y;
        double dz = this.z - other.z;

        return Math.sqrt(dx * dx + dy * dy + dz * dz);
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
     * Sets the character data and serializes it to JSON
     *
     * @param character The AbstractCharacter implementation to store
     */
    public void setCharacter(AbstractCharacter character) {
        this.character = character;
        if (character != null) {
            try {
                this.characterData = objectMapper.writeValueAsString(character);
                this.lastModified = LocalDateTime.now();

                LOGGER.debug("Serialized character data for character {} in world {}", id, worldId);
            } catch (JsonProcessingException e) {
                LOGGER.error("Failed to serialize character data for character {} in world {}: {}",
                           id, worldId, e.getMessage(), e);
                throw new RuntimeException("Failed to serialize character data", e);
            }
        } else {
            this.characterData = null;
        }
    }

    /**
     * Gets the character object, deserializing from JSON if necessary
     *
     * @param characterClass The class of the AbstractCharacter implementation
     * @return The character object or null if not available
     */
    public <T extends AbstractCharacter> T getCharacter(Class<T> characterClass) {
        if (character == null && characterData != null) {
            try {
                character = objectMapper.readValue(characterData, characterClass);
                LOGGER.debug("Deserialized character data for character {} in world {}", id, worldId);
            } catch (JsonProcessingException e) {
                LOGGER.error("Failed to deserialize character data for character {} in world {}: {}",
                           id, worldId, e.getMessage(), e);
                throw new RuntimeException("Failed to deserialize character data", e);
            }
        }
        return characterClass.cast(character);
    }

    /**
     * Gets the character object as AbstractCharacter (generic method)
     *
     * @return The character object or null if not available
     */
    public AbstractCharacter getCharacter() {
        return getCharacter(AbstractCharacter.class);
    }

    /**
     * Updates the character data and marks as modified
     *
     * @param character The updated character
     */
    public void updateCharacter(AbstractCharacter character) {
        setCharacter(character);
        this.lastModified = LocalDateTime.now();
    }

    /**
     * Checks if character data is available
     *
     * @return true if character data is stored
     */
    public boolean hasCharacterData() {
        return characterData != null && !characterData.trim().isEmpty();
    }

    /**
     * Clears the character data
     */
    public void clearCharacterData() {
        this.character = null;
        this.characterData = null;
        this.lastModified = LocalDateTime.now();

        LOGGER.debug("Cleared character data for character {} in world {}", id, worldId);
    }

    /**
     * Pre-persist callback to set creation timestamp
     */
    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        lastModified = now;
    }

    /**
     * Pre-update callback to update modification timestamp
     */
    @PreUpdate
    protected void onUpdate() {
        lastModified = LocalDateTime.now();
    }
}
