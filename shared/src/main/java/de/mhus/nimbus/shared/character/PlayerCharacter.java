package de.mhus.nimbus.shared.character;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serial;

/**
 * Player character class for user-controlled characters.
 * Contains player-specific properties and behaviors.
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class PlayerCharacter extends AbstractCharacter {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Player's username/account name
     */
    private String username;

    /**
     * Player's current level
     */
    private Integer level = 1;

    /**
     * Experience points
     */
    private Long experience = 0L;

    /**
     * Player's current energy/stamina
     */
    private Integer energy;

    /**
     * Maximum energy/stamina
     */
    private Integer maxEnergy;

    /**
     * Player's inventory capacity
     */
    private Integer inventoryCapacity = 20;

    /**
     * Whether the player is currently online
     */
    private boolean online = false;

    /**
     * Constructor with essential fields
     */
    public PlayerCharacter(String worldId, double x, double y, double z, String username) {
        super(worldId, CharacterType.PLAYER, x, y, z);
        this.username = username;
        this.name = username;
        this.displayName = username;
        this.level = 1;
        this.experience = 0L;
        this.health = 100;
        this.maxHealth = 100;
        this.energy = 100;
        this.maxEnergy = 100;
    }

    /**
     * Constructor with name and display name
     */
    public PlayerCharacter(String worldId, double x, double y, double z, String username,
                          String name, String displayName) {
        super(worldId, CharacterType.PLAYER, x, y, z, name, displayName);
        this.username = username;
        this.level = 1;
        this.experience = 0L;
        this.health = 100;
        this.maxHealth = 100;
        this.energy = 100;
        this.maxEnergy = 100;
    }

    /**
     * Adds experience points and handles level progression
     */
    public void addExperience(long exp) {
        this.experience += exp;
        checkLevelUp();
    }

    /**
     * Checks if player should level up based on experience
     */
    private void checkLevelUp() {
        long requiredExp = level * 1000L; // Simple formula: level * 1000
        if (experience >= requiredExp) {
            level++;
            // Increase max health and energy on level up
            maxHealth += 10;
            maxEnergy += 5;
            health = maxHealth; // Full heal on level up
            energy = maxEnergy; // Full energy on level up
        }
    }

    /**
     * Consumes energy for actions
     */
    public boolean consumeEnergy(int amount) {
        if (energy >= amount) {
            energy -= amount;
            return true;
        }
        return false;
    }

    /**
     * Restores energy
     */
    public void restoreEnergy(int amount) {
        energy = Math.min(energy + amount, maxEnergy);
    }

    @Override
    public String getTypeSpecificInfo() {
        return String.format("Player[username=%s, level=%d, exp=%d, energy=%d/%d, online=%s]",
                           username, level, experience, energy, maxEnergy, online);
    }
}
