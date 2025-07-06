package de.mhus.nimbus.shared.character;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.HashMap;

/**
 * Construct character class for artificial beings and mechanical entities.
 * Contains construct-specific properties and behaviors.
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ConstructCharacter extends AbstractCharacter {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Types of constructs
     */
    public enum ConstructType {
        ROBOT, GOLEM, AUTOMATON, DRONE, ANDROID, MECH, TURRET, GUARDIAN
    }

    /**
     * Power source types
     */
    public enum PowerSource {
        ELECTRIC, MAGIC, STEAM, SOLAR, NUCLEAR, BIOMASS, CRYSTAL
    }

    /**
     * Operational states
     */
    public enum OperationalState {
        ACTIVE, IDLE, MAINTENANCE, POWERED_DOWN, MALFUNCTIONING, DESTROYED
    }

    /**
     * Type of construct
     */
    private ConstructType constructType = ConstructType.ROBOT;

    /**
     * Power source
     */
    private PowerSource powerSource = PowerSource.ELECTRIC;

    /**
     * Current operational state
     */
    private OperationalState operationalState = OperationalState.ACTIVE;

    /**
     * Current power level (0-100)
     */
    private Integer powerLevel = 100;

    /**
     * Maximum power capacity
     */
    private Integer maxPowerLevel = 100;

    /**
     * Power consumption rate per hour
     */
    private Integer powerConsumptionRate = 5;

    /**
     * Durability/condition (0-100)
     */
    private Integer durability = 100;

    /**
     * Maximum durability
     */
    private Integer maxDurability = 100;

    /**
     * Processing power/intelligence level
     */
    private Integer processingPower = 50;

    /**
     * Movement speed
     */
    private Double movementSpeed = 2.0;

    /**
     * Detection/sensor range
     */
    private Double sensorRange = 10.0;

    /**
     * Whether construct can be repaired
     */
    private boolean repairable = true;

    /**
     * Whether construct can be upgraded
     */
    private boolean upgradeable = true;

    /**
     * Last maintenance time
     */
    private LocalDateTime lastMaintenanceTime;

    /**
     * Maintenance interval in hours
     */
    private Integer maintenanceInterval = 168; // 1 week

    /**
     * Owner/creator ID
     */
    private String ownerId;

    /**
     * Programming/behavior settings
     */
    private Map<String, Object> programmingSettings = new HashMap<>();

    /**
     * Installed modules/components
     */
    private Map<String, String> installedModules = new HashMap<>();

    /**
     * Constructor with essential fields
     */
    public ConstructCharacter(String worldId, double x, double y, double z, ConstructType constructType) {
        super(worldId, CharacterType.CONSTRUCT, x, y, z);
        this.constructType = constructType;
        this.name = constructType.name().toLowerCase();
        this.displayName = constructType.name();
        initializeConstructDefaults();
    }

    /**
     * Constructor with owner
     */
    public ConstructCharacter(String worldId, double x, double y, double z, ConstructType constructType,
                             String ownerId) {
        this(worldId, x, y, z, constructType);
        this.ownerId = ownerId;
    }

    /**
     * Constructor with power source
     */
    public ConstructCharacter(String worldId, double x, double y, double z, ConstructType constructType,
                             PowerSource powerSource, String ownerId) {
        this(worldId, x, y, z, constructType, ownerId);
        this.powerSource = powerSource;
        adjustForPowerSource();
    }

    /**
     * Initialize default values based on construct type
     */
    private void initializeConstructDefaults() {
        switch (constructType) {
            case ROBOT:
                this.health = 100;
                this.maxHealth = 100;
                this.durability = 80;
                this.maxDurability = 80;
                this.processingPower = 60;
                this.movementSpeed = 3.0;
                break;
            case GOLEM:
                this.health = 200;
                this.maxHealth = 200;
                this.durability = 150;
                this.maxDurability = 150;
                this.processingPower = 30;
                this.movementSpeed = 1.5;
                this.powerSource = PowerSource.MAGIC;
                break;
            case DRONE:
                this.health = 50;
                this.maxHealth = 50;
                this.durability = 40;
                this.maxDurability = 40;
                this.processingPower = 70;
                this.movementSpeed = 6.0;
                this.sensorRange = 15.0;
                break;
            case ANDROID:
                this.health = 120;
                this.maxHealth = 120;
                this.durability = 90;
                this.maxDurability = 90;
                this.processingPower = 80;
                this.movementSpeed = 2.5;
                break;
            case MECH:
                this.health = 300;
                this.maxHealth = 300;
                this.durability = 200;
                this.maxDurability = 200;
                this.processingPower = 50;
                this.movementSpeed = 2.0;
                this.powerConsumptionRate = 15;
                break;
            case TURRET:
                this.health = 150;
                this.maxHealth = 150;
                this.durability = 120;
                this.maxDurability = 120;
                this.processingPower = 40;
                this.movementSpeed = 0.0; // Stationary
                this.sensorRange = 20.0;
                break;
            case GUARDIAN:
                this.health = 250;
                this.maxHealth = 250;
                this.durability = 180;
                this.maxDurability = 180;
                this.processingPower = 60;
                this.movementSpeed = 2.5;
                this.powerSource = PowerSource.MAGIC;
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
     * Adjust properties based on power source
     */
    private void adjustForPowerSource() {
        switch (powerSource) {
            case MAGIC:
                this.powerConsumptionRate = (int)(powerConsumptionRate * 0.5);
                this.processingPower = (int)(processingPower * 1.2);
                break;
            case SOLAR:
                this.powerConsumptionRate = (int)(powerConsumptionRate * 0.3);
                break;
            case NUCLEAR:
                this.maxPowerLevel = (int)(maxPowerLevel * 2.0);
                this.powerLevel = maxPowerLevel;
                this.powerConsumptionRate = (int)(powerConsumptionRate * 0.2);
                break;
            case CRYSTAL:
                this.processingPower = (int)(processingPower * 1.5);
                this.sensorRange *= 1.3;
                break;
        }
    }

    /**
     * Consumes power for operations
     */
    public boolean consumePower(int amount) {
        if (powerLevel >= amount && operationalState == OperationalState.ACTIVE) {
            powerLevel -= amount;
            if (powerLevel <= 0) {
                operationalState = OperationalState.POWERED_DOWN;
                powerLevel = 0;
            }
            return true;
        }
        return false;
    }

    /**
     * Recharges power
     */
    public void rechargePower(int amount) {
        powerLevel = Math.min(powerLevel + amount, maxPowerLevel);
        if (powerLevel > 20 && operationalState == OperationalState.POWERED_DOWN) {
            operationalState = OperationalState.ACTIVE;
        }
    }

    /**
     * Performs maintenance
     */
    public void performMaintenance() {
        durability = Math.min(durability + 20, maxDurability);
        lastMaintenanceTime = LocalDateTime.now();
        if (operationalState == OperationalState.MALFUNCTIONING) {
            operationalState = OperationalState.ACTIVE;
        }
    }

    /**
     * Repairs the construct
     */
    public boolean repair(int amount) {
        if (!repairable || operationalState == OperationalState.DESTROYED) {
            return false;
        }

        durability = Math.min(durability + amount, maxDurability);
        health = Math.min(health + amount, maxHealth);

        if (durability > 30 && operationalState == OperationalState.MALFUNCTIONING) {
            operationalState = OperationalState.ACTIVE;
        }

        return true;
    }

    /**
     * Installs a module/upgrade
     */
    public boolean installModule(String moduleName, String moduleType) {
        if (!upgradeable) {
            return false;
        }

        installedModules.put(moduleName, moduleType);

        // Apply module effects
        switch (moduleType.toLowerCase()) {
            case "speed":
                movementSpeed *= 1.2;
                break;
            case "sensor":
                sensorRange *= 1.3;
                break;
            case "power":
                maxPowerLevel = (int)(maxPowerLevel * 1.2);
                break;
            case "processing":
                processingPower = (int)(processingPower * 1.3);
                break;
        }

        return true;
    }

    /**
     * Sets a programming parameter
     */
    public void setProgrammingSetting(String key, Object value) {
        programmingSettings.put(key, value);
    }

    /**
     * Gets a programming parameter
     */
    public Object getProgrammingSetting(String key) {
        return programmingSettings.get(key);
    }

    /**
     * Checks if construct needs maintenance
     */
    public boolean needsMaintenance() {
        if (lastMaintenanceTime == null) {
            return true;
        }
        return lastMaintenanceTime.plusHours(maintenanceInterval).isBefore(LocalDateTime.now());
    }

    /**
     * Checks if construct is operational
     */
    public boolean isOperational() {
        return operationalState == OperationalState.ACTIVE ||
               operationalState == OperationalState.IDLE;
    }

    /**
     * Damages the construct
     */
    public void takeDamage(int damage) {
        health -= damage;
        durability -= damage / 2;

        if (health <= 0) {
            operationalState = OperationalState.DESTROYED;
            active = false;
        } else if (durability <= 30) {
            operationalState = OperationalState.MALFUNCTIONING;
        }
    }

    @Override
    public String getTypeSpecificInfo() {
        return String.format("Construct[type=%s, power=%d/%d, durability=%d/%d, state=%s, processing=%d]",
                           constructType, powerLevel, maxPowerLevel, durability, maxDurability,
                           operationalState, processingPower);
    }
}
