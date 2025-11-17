package de.mhus.nimbus.generated;

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
public class PlayerInfo {

    /**
     * playerId
     */
    private String playerId;

    /**
     * displayName
     */
    private String displayName;

    /**
     * shortcuts (optional)
     */
    private java.util.Map<String, import('./ShortcutDefinition').ShortcutDefinition> shortcuts;

    /**
     * stateValues
     */
    private java.util.Map<MovementStateKey, MovementStateValues> stateValues;

    /**
     * baseWalkSpeed
     */
    private double baseWalkSpeed;

    /**
     * baseRunSpeed
     */
    private double baseRunSpeed;

    /**
     * baseUnderwaterSpeed
     */
    private double baseUnderwaterSpeed;

    /**
     * baseCrawlSpeed
     */
    private double baseCrawlSpeed;

    /**
     * baseRidingSpeed
     */
    private double baseRidingSpeed;

    /**
     * baseJumpSpeed
     */
    private double baseJumpSpeed;

    /**
     * effectiveWalkSpeed
     */
    private double effectiveWalkSpeed;

    /**
     * effectiveRunSpeed
     */
    private double effectiveRunSpeed;

    /**
     * effectiveUnderwaterSpeed
     */
    private double effectiveUnderwaterSpeed;

    /**
     * effectiveCrawlSpeed
     */
    private double effectiveCrawlSpeed;

    /**
     * effectiveRidingSpeed
     */
    private double effectiveRidingSpeed;

    /**
     * effectiveJumpSpeed
     */
    private double effectiveJumpSpeed;

    /**
     * eyeHeight
     */
    private double eyeHeight;

    /**
     * dimensions
     */
    private { dimensions;
}
