/*
 * Source TS: BlockModifier.ts
 * Original TS: 'interface PhysicsModifier'
 */
package de.mhus.nimbus.evaluate.generated.types;

@lombok.Data
@lombok.experimental.SuperBuilder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class PhysicsModifier {
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
