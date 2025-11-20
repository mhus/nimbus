package de.mhus.nimbus.generated.types;

@lombok.Data
@lombok.Builder
public class ClientEntity extends Object {
    private String id;
    private EntityModel model;
    private Entity entity;
    private boolean visible;
    private java.util.List<Object> meshes;
    private Vector3 currentPosition;
    private Rotation currentRotation;
    private double currentWaypointIndex;
    private double currentPose;
    private java.util.List<Waypoint> currentWaypoints;
    private double lastAccess;
    private java.lang.Double lastStepTime;
}
