package de.mhus.nimbus.generated.types;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Generated from ServerEntitySpawnDefinition.ts
 * DO NOT EDIT MANUALLY - This file is auto-generated
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BehaviorConfig {

    /**
     * minStepDistance (optional)
     */
    private double minStepDistance;

    /**
     * maxStepDistance (optional)
     */
    private double maxStepDistance;

    /**
     * waypointsPerPath (optional)
     */
    private double waypointsPerPath;

    /**
     * minIdleDuration (optional)
     */
    private double minIdleDuration;

    /**
     * maxIdleDuration (optional)
     */
    private double maxIdleDuration;

    /**
     * pathwayInterval (optional)
     */
    private double pathwayInterval;
}
