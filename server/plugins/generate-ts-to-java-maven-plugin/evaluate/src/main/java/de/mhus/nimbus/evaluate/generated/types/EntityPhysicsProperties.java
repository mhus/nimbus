/*
 * Source TS: EntityData.ts
 * Original TS: 'interface EntityPhysicsProperties'
 */
package de.mhus.nimbus.evaluate.generated.types;

@lombok.Data
@lombok.experimental.SuperBuilder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class EntityPhysicsProperties {
    private double mass;
    private double friction;
    private java.lang.Double restitution;
    private java.lang.Double drag;
}
