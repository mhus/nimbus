package de.mhus.nimbus.generated.types;

public interface EntityPathway {
    String getEntityId();
    double getStartAt();
    java.util.List<Waypoint> getWaypoints();
    java.lang.Boolean getIsLooping();
    java.lang.Double getQueryAt();
    ENTITY_POSES getIdlePose();
    java.lang.Boolean getPhysicsEnabled();
    Vector3 getVelocity();
    java.lang.Boolean getGrounded();
}
