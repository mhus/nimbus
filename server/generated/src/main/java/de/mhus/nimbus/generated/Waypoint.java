package de.mhus.nimbus.generated;

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
public class Waypoint {

    /**
     * timestamp
     */
    private double timestamp;

    /**
     * target
     */
    private Vector3 target;

    /**
     * rotation
     */
    private Rotation rotation;

    /**
     * pose
     */
    private ENTITY_POSES pose;
}
