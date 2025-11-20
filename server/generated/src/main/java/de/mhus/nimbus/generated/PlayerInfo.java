package de.mhus.nimbus.generated;

@lombok.Data
@lombok.Builder
public class PlayerInfo extends Object {
    private String playerId;
    private String displayName;
    private java.util.Map<MovementStateKey, MovementStateValues> stateValues;
    private double baseWalkSpeed;
    private double baseRunSpeed;
    private double baseUnderwaterSpeed;
    private double baseCrawlSpeed;
    private double baseRidingSpeed;
    private double baseJumpSpeed;
    private double effectiveWalkSpeed;
    private double effectiveRunSpeed;
    private double effectiveUnderwaterSpeed;
    private double effectiveCrawlSpeed;
    private double effectiveRidingSpeed;
    private double effectiveJumpSpeed;
    private double eyeHeight;
    private double stealthRange;
    private double distanceNotifyReductionWalk;
    private double distanceNotifyReductionCrouch;
    private double selectionRadius;
    private double baseTurnSpeed;
    private double effectiveTurnSpeed;
    private double baseUnderwaterTurnSpeed;
    private double effectiveUnderwaterTurnSpeed;
    private String thirdPersonModelId;
}
