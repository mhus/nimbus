package de.mhus.nimbus.generated;

@lombok.Data
@lombok.Builder
public class Waypoint extends Object {
    private double timestamp;
    private Vector3 target;
    private Rotation rotation;
    private ENTITY_POSES pose;
}
