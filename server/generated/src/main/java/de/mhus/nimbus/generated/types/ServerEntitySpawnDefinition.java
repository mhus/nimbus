/*
 * Source TS: ServerEntitySpawnDefinition.ts
 * Original TS: 'interface ServerEntitySpawnDefinition'
 */
package de.mhus.nimbus.generated.types;

@com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
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
    @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
    private BehaviorConfig behaviorConfig;
    @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
    private EntityPathway currentPathway;
    private java.util.List<Vector2> chunks;
    private Vector3 position;
    private Vector3 velocity;
    private Rotation rotation;
    private boolean grounded;
}
