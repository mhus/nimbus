package de.mhus.nimbus.generated.types;

@lombok.Data
@lombok.Builder
public class Waypoint extends Object {
    private double timestamp;
    private Vector3 target;
    private Rotation rotation;
    private ENTITY_POSES pose;
}
