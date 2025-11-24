/*
 * Source TS: ServerEntitySpawnDefinition.ts
 * Original TS: 'interface ServerEntitySpawnDefinition'
 */
package de.mhus.nimbus.evaluate.generated.types;

@Deprecated
@lombok.Data
@lombok.experimental.SuperBuilder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class ServerEntitySpawnDefinition {
    @Deprecated
    @SuppressWarnings("required")
    private String entityId;
    @Deprecated
    @SuppressWarnings("required")
    private String entityModelId;
    @Deprecated
    @SuppressWarnings("required")
    private Vector3 initialPosition;
    @Deprecated
    @SuppressWarnings("required")
    private Rotation initialRotation;
    @Deprecated
    @SuppressWarnings("required")
    private Vector3 middlePoint;
    @Deprecated
    @SuppressWarnings("required")
    private double radius;
    @Deprecated
    @SuppressWarnings("required")
    private double speed;
    @Deprecated
    @SuppressWarnings("required")
    private String behaviorModel;
    @Deprecated
    @SuppressWarnings("optional")
    private BehaviorConfig behaviorConfig;
    @Deprecated
    @SuppressWarnings("optional")
    private EntityPathway currentPathway;
    @Deprecated
    @SuppressWarnings("required")
    private java.util.List<Vector2> chunks;
    @Deprecated
    @SuppressWarnings("optional")
    private java.util.Map<String, Object> physicsState;
}
