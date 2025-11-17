package de.mhus.nimbus.generated.types;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Generated from PlayerInfo.ts
 * DO NOT EDIT MANUALLY - This file is auto-generated
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MovementStateValues {

    /**
     * dimensions
     */
    private java.util.Map<String, Object> dimensions;

    /**
     * baseMoveSpeed
     */
    private double baseMoveSpeed;

    /**
     * effectiveMoveSpeed
     */
    private double effectiveMoveSpeed;

    /**
     * baseJumpSpeed
     */
    private double baseJumpSpeed;

    /**
     * effectiveJumpSpeed
     */
    private double effectiveJumpSpeed;

    /**
     * eyeHeight
     */
    private double eyeHeight;

    /**
     * baseTurnSpeed
     */
    private double baseTurnSpeed;

    /**
     * effectiveTurnSpeed
     */
    private double effectiveTurnSpeed;

    /**
     * selectionRadius
     */
    private double selectionRadius;

    /**
     * stealthRange
     */
    private double stealthRange;

    /**
     * distanceNotifyReduction
     */
    private double distanceNotifyReduction;
}
