package de.mhus.nimbus.generated.types;

@lombok.Data
@lombok.Builder
public class PhysicsModifier extends Object {
    private java.lang.Boolean solid;
    private java.lang.Double resistance;
    private java.lang.Double climbable;
    private java.lang.Boolean autoClimbable;
    private Vector3 autoMove;
    private java.lang.Double autoOrientationY;
    private java.lang.Boolean interactive;
    private java.lang.Boolean collisionEvent;
    private Direction passableFrom;
    private java.lang.Boolean autoJump;
    private java.util.List<Object> cornerHeights;
}
