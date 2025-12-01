/*
 * Source TS: PlayerInfo.ts
 * Original TS: 'interface PlayerInfo'
 */
package de.mhus.nimbus.types;

@com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
@org.springframework.aot.hint.annotation.Reflective
@lombok.Data
@lombok.experimental.SuperBuilder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class PlayerInfo {
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
    private java.util.Map<String, Object> dimensions;
    private double stealthRange;
    private double distanceNotifyReductionWalk;
    private double distanceNotifyReductionCrouch;
    private double selectionRadius;
    private double baseTurnSpeed;
    private double effectiveTurnSpeed;
    private double baseUnderwaterTurnSpeed;
    private double effectiveUnderwaterTurnSpeed;
    @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
    private String thirdPersonModelId;
}
