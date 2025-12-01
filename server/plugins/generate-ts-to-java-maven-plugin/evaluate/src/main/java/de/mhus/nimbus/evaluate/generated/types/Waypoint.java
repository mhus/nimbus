/*
 * Source TS: EntityData.ts
 * Original TS: 'interface Waypoint'
 */
package de.mhus.nimbus.evaluate.generated.types;

@lombok.Data
@lombok.experimental.SuperBuilder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class Waypoint {
    private double timestamp;
    private Vector3 target;
    private Rotation rotation;
    private ENTITY_POSES pose;
}
