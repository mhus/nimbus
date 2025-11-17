package de.mhus.nimbus.generated.types;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Generated from ClientEntity.ts
 * DO NOT EDIT MANUALLY - This file is auto-generated
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClientEntity {

    /**
     * id
     */
    private String id;

    /**
     * model
     */
    private EntityModel model;

    /**
     * entity
     */
    private Entity entity;

    /**
     * visible
     */
    private boolean visible;

    /**
     * meshes
     */
    private java.util.List<Object> meshes;

    /**
     * currentPosition
     */
    private Vector3 currentPosition;

    /**
     * currentRotation
     */
    private Rotation currentRotation;

    /**
     * currentWaypointIndex
     */
    private double currentWaypointIndex;

    /**
     * currentPose
     */
    private double currentPose;

    /**
     * currentWaypoints
     */
    private java.util.List<Waypoint> currentWaypoints;

    /**
     * lastAccess
     */
    private double lastAccess;

    /**
     * lastStepTime (optional)
     */
    private double lastStepTime;
}
