/*
 * Source TS: PlayerInfo.ts
 * Original TS: 'interface MovementStateValues'
 */
package de.mhus.nimbus.generated.types;

@com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
@org.springframework.aot.hint.annotation.Reflective
@lombok.Data
@lombok.experimental.SuperBuilder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class MovementStateValues {
    private double height;
    private double width;
    private double footprint;
    private double baseMoveSpeed;
    private double effectiveMoveSpeed;
    private double baseJumpSpeed;
    private double effectiveJumpSpeed;
    private double eyeHeight;
    private double baseTurnSpeed;
    private double effectiveTurnSpeed;
    private double selectionRadius;
    private double stealthRange;
    private double distanceNotifyReduction;
}
