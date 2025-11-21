/*
 * Source TS: EntityData.ts
 * Original TS: 'interface Waypoint'
 */
package de.mhus.nimbus.generated.types;

@com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
@lombok.Data
@lombok.Builder
public class Waypoint extends Object {
    private double timestamp;
    private Vector3 target;
    private Rotation rotation;
    private ENTITY_POSES pose;
}
