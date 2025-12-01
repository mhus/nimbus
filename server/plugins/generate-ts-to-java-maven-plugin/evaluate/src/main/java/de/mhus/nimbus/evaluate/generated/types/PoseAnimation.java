/*
 * Source TS: EntityData.ts
 * Original TS: 'interface PoseAnimation'
 */
package de.mhus.nimbus.evaluate.generated.types;

@lombok.Data
@lombok.experimental.SuperBuilder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class PoseAnimation {
    private String animationName;
    private double speedMultiplier;
    private boolean loop;
}
