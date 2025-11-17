package de.mhus.nimbus.generated;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Generated from EntityData.ts
 * DO NOT EDIT MANUALLY - This file is auto-generated
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EntityPathway {

    /**
     * entityId
     */
    private String entityId;

    /**
     * startAt
     */
    private double startAt;

    /**
     * waypoints
     */
    private java.util.List<Waypoint> waypoints;

    /**
     * isLooping (optional)
     */
    private boolean isLooping;

    /**
     * queryAt (optional)
     */
    private double queryAt;

    /**
     * idlePose (optional)
     */
    private ENTITY_POSES idlePose;

    /**
     * physicsEnabled (optional)
     */
    private boolean physicsEnabled;

    /**
     * velocity (optional)
     */
    private Vector3 velocity;

    /**
     * grounded (optional)
     */
    private boolean grounded;
}
