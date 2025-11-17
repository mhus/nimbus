package de.mhus.nimbus.generated;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Generated from BlockModifier.ts
 * DO NOT EDIT MANUALLY - This file is auto-generated
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PhysicsModifier {

    /**
     * solid (optional)
     */
    private boolean solid;

    /**
     * resistance (optional)
     */
    private double resistance;

    /**
     * climbable (optional)
     */
    private double climbable;

    /**
     * autoClimbable (optional)
     */
    private boolean autoClimbable;

    /**
     * autoMove (optional)
     */
    private Vector3 autoMove;

    /**
     * autoOrientationY (optional)
     */
    private double autoOrientationY;

    /**
     * interactive (optional)
     */
    private boolean interactive;

    /**
     * collisionEvent (optional)
     */
    private boolean collisionEvent;

    /**
     * passableFrom (optional)
     */
    private Direction passableFrom;

    /**
     * autoJump (optional)
     */
    private boolean autoJump;

    /**
     * cornerHeights (optional)
     */
    private java.util.List<Double> cornerHeights;
}
