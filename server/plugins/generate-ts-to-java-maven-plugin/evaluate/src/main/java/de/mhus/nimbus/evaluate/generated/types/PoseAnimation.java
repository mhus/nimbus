/*
 * Source TS: EntityData.ts
 * Original TS: 'interface PoseAnimation'
 */
package de.mhus.nimbus.evaluate.generated.types;

@Deprecated
@lombok.Data
@lombok.experimental.SuperBuilder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class PoseAnimation {
    @Deprecated
    @SuppressWarnings("required")
    private String animationName;
    @Deprecated
    @SuppressWarnings("required")
    private double speedMultiplier;
    @Deprecated
    @SuppressWarnings("required")
    private boolean loop;
}
