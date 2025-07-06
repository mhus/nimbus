package de.mhus.nimbus.shared.character;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.time.LocalDateTime;

/**
 * Fauna character class for animal life and creatures.
 * Contains animal-specific properties and behaviors.
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class FaunaCharacter extends AbstractCharacter {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Animal types for different fauna
     */
    public enum AnimalType {
        MAMMAL, BIRD, REPTILE, AMPHIBIAN, FISH, INSECT, MYTHICAL
    }

    /**
     * Animal behavior patterns
     */
    public enum AnimalBehavior {
        PASSIVE, AGGRESSIVE, DEFENSIVE, TIMID, PREDATOR, PREY, DOMESTICATED
    }

    /**
     * Age stages of the animal
     */
    public enum AgeStage {
        BABY, JUVENILE, ADULT, ELDER
    }

    /**
     * Type of animal
     */
    private AnimalType animalType = AnimalType.MAMMAL;

    /**
     * Behavioral pattern
     */
    private AnimalBehavior behavior = AnimalBehavior.PASSIVE;

    /**
     * Current age stage
     */
    private AgeStage ageStage = AgeStage.ADULT;

    /**
     * Movement speed
     */
    private Double movementSpeed = 2.0;

    /**
     * Detection range for threats/food
     */
    private Double detectionRange = 8.0;

    /**
     * Attack range (for aggressive animals)
     */
    private Double attackRange = 2.0;

    /**
     * Flee range (distance at which animal flees)
     */
    private Double fleeRange = 5.0;

    /**
     * Whether the animal can fly
     */
    private boolean canFly = false;

    /**
     * Whether the animal can swim
     */
    private boolean canSwim = false;

    /**
     * Whether the animal is tameable
     */
    private boolean tameable = false;

    /**
     * Whether the animal is currently tamed
     */
    private boolean tamed = false;

    /**
     * Owner ID if tamed
     */
    private String ownerId;

    /**
     * Hunger level (0-100)
     */
    private Integer hunger = 50;

    /**
     * Happiness level (0-100)
     */
    private Integer happiness = 50;

    /**
     * Energy/stamina level
     */
    private Integer energy = 100;

    /**
     * Maximum energy
     */
    private Integer maxEnergy = 100;

    /**
     * Last feeding time
     */
    private LocalDateTime lastFedTime;

    /**
     * Breeding cooldown
     */
    private LocalDateTime lastBreedTime;

    /**
     * Constructor with essential fields
     */
    public FaunaCharacter(String worldId, double x, double y, double z, AnimalType animalType) {
        super(worldId, CharacterType.FAUNA, x, y, z);
        this.animalType = animalType;
        this.name = animalType.name().toLowerCase();
        this.displayName = animalType.name();
        initializeAnimalDefaults();
    }

    /**
     * Constructor with behavior
     */
    public FaunaCharacter(String worldId, double x, double y, double z, AnimalType animalType,
                         AnimalBehavior behavior) {
        this(worldId, x, y, z, animalType);
        this.behavior = behavior;
        adjustStatsForBehavior();
    }

    /**
     * Constructor with age stage
     */
    public FaunaCharacter(String worldId, double x, double y, double z, AnimalType animalType,
                         AnimalBehavior behavior, AgeStage ageStage) {
        this(worldId, x, y, z, animalType, behavior);
        this.ageStage = ageStage;
        adjustStatsForAge();
    }

    /**
     * Initialize default values based on animal type
     */
    private void initializeAnimalDefaults() {
        switch (animalType) {
            case MAMMAL:
                this.health = 80;
                this.maxHealth = 80;
                this.movementSpeed = 3.0;
                this.tameable = true;
                break;
            case BIRD:
                this.health = 40;
                this.maxHealth = 40;
                this.movementSpeed = 5.0;
                this.canFly = true;
                this.detectionRange = 12.0;
                break;
            case REPTILE:
                this.health = 60;
                this.maxHealth = 60;
                this.movementSpeed = 1.5;
                this.behavior = AnimalBehavior.DEFENSIVE;
                break;
            case FISH:
                this.health = 30;
                this.maxHealth = 30;
                this.movementSpeed = 4.0;
                this.canSwim = true;
                break;
            case INSECT:
                this.health = 10;
                this.maxHealth = 10;
                this.movementSpeed = 2.0;
                this.detectionRange = 3.0;
                break;
            case MYTHICAL:
                this.health = 200;
                this.maxHealth = 200;
                this.movementSpeed = 4.0;
                this.canFly = true;
                this.tameable = false;
                break;
            default:
                this.health = 50;
                this.maxHealth = 50;
                break;
        }
    }

    /**
     * Adjust stats based on behavior
     */
    private void adjustStatsForBehavior() {
        switch (behavior) {
            case AGGRESSIVE, PREDATOR:
                this.health = (int)(health * 1.2);
                this.maxHealth = (int)(maxHealth * 1.2);
                this.movementSpeed *= 1.1;
                this.attackRange = 3.0;
                break;
            case TIMID, PREY:
                this.movementSpeed *= 1.3;
                this.detectionRange *= 1.5;
                this.fleeRange *= 1.5;
                break;
            case DOMESTICATED:
                this.tameable = true;
                this.tamed = true;
                this.happiness = 80;
                break;
        }
    }

    /**
     * Adjust stats based on age
     */
    private void adjustStatsForAge() {
        switch (ageStage) {
            case BABY:
                this.health = (int)(health * 0.3);
                this.maxHealth = (int)(maxHealth * 0.3);
                this.movementSpeed *= 0.5;
                this.tameable = true;
                break;
            case JUVENILE:
                this.health = (int)(health * 0.7);
                this.maxHealth = (int)(maxHealth * 0.7);
                this.movementSpeed *= 0.8;
                break;
            case ADULT:
                // No changes - adult is the baseline
                break;
            case ELDER:
                this.health = (int)(health * 0.8);
                this.maxHealth = (int)(maxHealth * 0.8);
                this.movementSpeed *= 0.7;
                break;
        }
    }

    /**
     * Feeds the animal
     */
    public void feed() {
        hunger = Math.max(0, hunger - 30);
        happiness = Math.min(100, happiness + 10);
        lastFedTime = LocalDateTime.now();
    }

    /**
     * Tames the animal if possible
     */
    public boolean tame(String playerId) {
        if (!tameable || tamed) {
            return false;
        }

        this.tamed = true;
        this.ownerId = playerId;
        this.behavior = AnimalBehavior.DOMESTICATED;
        this.happiness = 80;
        return true;
    }

    /**
     * Checks if animal is hungry
     */
    public boolean isHungry() {
        return hunger > 70;
    }

    /**
     * Checks if animal is happy
     */
    public boolean isHappy() {
        return happiness > 60;
    }

    /**
     * Ages the animal to next stage
     */
    public boolean age() {
        AgeStage[] stages = AgeStage.values();
        int currentIndex = ageStage.ordinal();
        if (currentIndex < stages.length - 1) {
            ageStage = stages[currentIndex + 1];
            adjustStatsForAge();
            return true;
        }
        return false;
    }

    /**
     * Calculates if target is within detection range
     */
    public boolean canDetect(double targetX, double targetY, double targetZ) {
        double distance = Math.sqrt(
            Math.pow(x - targetX, 2) +
            Math.pow(y - targetY, 2) +
            Math.pow(z - targetZ, 2)
        );
        return distance <= detectionRange;
    }

    /**
     * Checks if animal should flee from target
     */
    public boolean shouldFlee(double targetX, double targetY, double targetZ) {
        if (behavior == AnimalBehavior.AGGRESSIVE || behavior == AnimalBehavior.PREDATOR) {
            return false;
        }

        double distance = Math.sqrt(
            Math.pow(x - targetX, 2) +
            Math.pow(y - targetY, 2) +
            Math.pow(z - targetZ, 2)
        );
        return distance <= fleeRange;
    }

    @Override
    public String getTypeSpecificInfo() {
        return String.format("Fauna[type=%s, behavior=%s, age=%s, tamed=%s, hunger=%d, happiness=%d]",
                           animalType, behavior, ageStage, tamed, hunger, happiness);
    }
}
