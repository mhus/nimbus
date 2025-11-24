/*
 * Source TS: EntityData.ts
 * Original TS: 'interface EntityPhysicsProperties'
 */
package de.mhus.nimbus.evaluate.generated.types;

@Deprecated
@lombok.Data
@lombok.experimental.SuperBuilder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class EntityPhysicsProperties {
    @Deprecated
    @SuppressWarnings("required")
    private double mass;
    @Deprecated
    @SuppressWarnings("required")
    private double friction;
    @Deprecated
    @SuppressWarnings("optional")
    private java.lang.Double restitution;
    @Deprecated
    @SuppressWarnings("optional")
    private java.lang.Double drag;
}
