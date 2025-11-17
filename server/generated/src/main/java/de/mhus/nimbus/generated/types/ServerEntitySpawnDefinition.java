package de.mhus.nimbus.generated.types;

import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Generated from ServerEntitySpawnDefinition.ts
 * DO NOT EDIT MANUALLY - This file is auto-generated
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServerEntitySpawnDefinition {

    /**
     * entityId
     */
    private String entityId;

    /**
     * entityModelId
     */
    private String entityModelId;

    /**
     * initialPosition
     */
    private Vector3 initialPosition;

    /**
     * initialRotation
     */
    private Rotation initialRotation;

    /**
     * middlePoint
     */
    private Vector3 middlePoint;

    /**
     * radius
     */
    private double radius;

    /**
     * speed
     */
    private double speed;

    /**
     * behaviorModel
     */
    private String behaviorModel;

    /**
     * behaviorConfig (optional)
     */
    private BehaviorConfig behaviorConfig;

    /**
     * currentPathway (optional)
     */
    private EntityPathway currentPathway;

    /**
     * chunks
     */
    private java.util.List<Vector2> chunks;

    /**
     * physicsState (optional)
     */
    private java.util.Map<String, Object> physicsState;
}
