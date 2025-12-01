/*
 * Source TS: ServerEntitySpawnDefinition.ts
 * Original TS: 'interface ServerEntitySpawnDefinition'
 */
package de.mhus.nimbus.evaluate.generated.types;

@lombok.Data
@lombok.experimental.SuperBuilder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class ServerEntitySpawnDefinition {
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
    private java.util.Map<String, Object> physicsState;
}
