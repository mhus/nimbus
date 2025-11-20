package de.mhus.nimbus.generated.types;

public interface ServerEntitySpawnDefinition {
    String getEntityId();
    String getEntityModelId();
    Vector3 getInitialPosition();
    Rotation getInitialRotation();
    Vector3 getMiddlePoint();
    double getRadius();
    double getSpeed();
    String getBehaviorModel();
    BehaviorConfig getBehaviorConfig();
    EntityPathway getCurrentPathway();
    java.util.List<Vector2> getChunks();
    Vector3 getPosition();
    Vector3 getVelocity();
    Rotation getRotation();
    boolean getGrounded();
}
