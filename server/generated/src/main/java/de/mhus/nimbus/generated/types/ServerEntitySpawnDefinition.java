/*
 * Source TS: ServerEntitySpawnDefinition.ts
 * Original TS: 'interface ServerEntitySpawnDefinition'
 */
package de.mhus.nimbus.generated.types;

@lombok.Data
@lombok.Builder
public class ServerEntitySpawnDefinition extends Object {
    private String entityId;
    private String entityModelId;
    private Vector3 initialPosition;
    private Rotation initialRotation;
    private Vector3 middlePoint;
    private double radius;
    private double speed;
    private String behaviorModel;
    private BehaviorConfig behaviorConfig;
    private EntityPathway currentPathway;
    private java.util.List<Vector2> chunks;
    private Vector3 position;
    private Vector3 velocity;
    private Rotation rotation;
    private boolean grounded;
}
