/*
 * Source TS: EntityData.ts
 * Original TS: 'interface Waypoint'
 */
package de.mhus.nimbus.evaluate.generated.types;

@Deprecated
@lombok.Data
@lombok.experimental.SuperBuilder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class Waypoint {
    @Deprecated
    @SuppressWarnings("required")
    private double timestamp;
    @Deprecated
    @SuppressWarnings("required")
    private Vector3 target;
    @Deprecated
    @SuppressWarnings("required")
    private Rotation rotation;
    @Deprecated
    @SuppressWarnings("required")
    private ENTITY_POSES pose;
}
