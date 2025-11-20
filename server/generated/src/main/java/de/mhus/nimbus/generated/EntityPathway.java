package de.mhus.nimbus.generated;

@lombok.Data
@lombok.Builder
public class EntityPathway extends Object {
    private String entityId;
    private double startAt;
    private java.util.List<Waypoint> waypoints;
    private java.lang.Boolean isLooping;
    private java.lang.Double queryAt;
    private ENTITY_POSES idlePose;
    private java.lang.Boolean physicsEnabled;
    private Vector3 velocity;
    private java.lang.Boolean grounded;
}
