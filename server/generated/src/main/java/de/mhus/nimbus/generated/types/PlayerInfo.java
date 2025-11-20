package de.mhus.nimbus.generated.types;

public interface PlayerInfo {
    String getPlayerId();
    String getDisplayName();
    java.util.Map<MovementStateKey, MovementStateValues> getStateValues();
    double getBaseWalkSpeed();
    double getBaseRunSpeed();
    double getBaseUnderwaterSpeed();
    double getBaseCrawlSpeed();
    double getBaseRidingSpeed();
    double getBaseJumpSpeed();
    double getEffectiveWalkSpeed();
    double getEffectiveRunSpeed();
    double getEffectiveUnderwaterSpeed();
    double getEffectiveCrawlSpeed();
    double getEffectiveRidingSpeed();
    double getEffectiveJumpSpeed();
    double getEyeHeight();
    double getStealthRange();
    double getDistanceNotifyReductionWalk();
    double getDistanceNotifyReductionCrouch();
    double getSelectionRadius();
    double getBaseTurnSpeed();
    double getEffectiveTurnSpeed();
    double getBaseUnderwaterTurnSpeed();
    double getEffectiveUnderwaterTurnSpeed();
    String getThirdPersonModelId();
}
