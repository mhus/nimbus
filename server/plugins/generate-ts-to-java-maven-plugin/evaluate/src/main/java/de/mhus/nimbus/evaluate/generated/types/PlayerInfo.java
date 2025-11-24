/*
 * Source TS: PlayerInfo.ts
 * Original TS: 'interface PlayerInfo'
 */
package de.mhus.nimbus.evaluate.generated.types;

@Deprecated
@lombok.Data
@lombok.experimental.SuperBuilder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class PlayerInfo {
    @Deprecated
    @SuppressWarnings("required")
    private String playerId;
    @Deprecated
    @SuppressWarnings("required")
    private String displayName;
    @Deprecated
    @SuppressWarnings("required")
    private java.util.Map<MovementStateKey, MovementStateValues> stateValues;
    @Deprecated
    @SuppressWarnings("required")
    private double baseWalkSpeed;
    @Deprecated
    @SuppressWarnings("required")
    private double baseRunSpeed;
    @Deprecated
    @SuppressWarnings("required")
    private double baseUnderwaterSpeed;
    @Deprecated
    @SuppressWarnings("required")
    private double baseCrawlSpeed;
    @Deprecated
    @SuppressWarnings("required")
    private double baseRidingSpeed;
    @Deprecated
    @SuppressWarnings("required")
    private double baseJumpSpeed;
    @Deprecated
    @SuppressWarnings("required")
    private double effectiveWalkSpeed;
    @Deprecated
    @SuppressWarnings("required")
    private double effectiveRunSpeed;
    @Deprecated
    @SuppressWarnings("required")
    private double effectiveUnderwaterSpeed;
    @Deprecated
    @SuppressWarnings("required")
    private double effectiveCrawlSpeed;
    @Deprecated
    @SuppressWarnings("required")
    private double effectiveRidingSpeed;
    @Deprecated
    @SuppressWarnings("required")
    private double effectiveJumpSpeed;
    @Deprecated
    @SuppressWarnings("required")
    private double eyeHeight;
    @Deprecated
    @SuppressWarnings("required")
    private java.util.Map<String, Object> dimensions;
    @Deprecated
    @SuppressWarnings("required")
    private double stealthRange;
    @Deprecated
    @SuppressWarnings("required")
    private double distanceNotifyReductionWalk;
    @Deprecated
    @SuppressWarnings("required")
    private double distanceNotifyReductionCrouch;
    @Deprecated
    @SuppressWarnings("required")
    private double selectionRadius;
    @Deprecated
    @SuppressWarnings("required")
    private double baseTurnSpeed;
    @Deprecated
    @SuppressWarnings("required")
    private double effectiveTurnSpeed;
    @Deprecated
    @SuppressWarnings("required")
    private double baseUnderwaterTurnSpeed;
    @Deprecated
    @SuppressWarnings("required")
    private double effectiveUnderwaterTurnSpeed;
    @Deprecated
    @SuppressWarnings("optional")
    private String thirdPersonModelId;
}
