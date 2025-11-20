package de.mhus.nimbus.generated.types;

public interface ClientEntity {
    String getId();
    EntityModel getModel();
    Entity getEntity();
    boolean getVisible();
    java.util.List<Object> getMeshes();
    Vector3 getCurrentPosition();
    Rotation getCurrentRotation();
    double getCurrentWaypointIndex();
    double getCurrentPose();
    java.util.List<Waypoint> getCurrentWaypoints();
    double getLastAccess();
    java.lang.Double getLastStepTime();
}
