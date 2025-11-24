/*
 * Source TS: EntityData.ts
 * Original TS: 'interface Entity'
 */
package de.mhus.nimbus.evaluate.generated.types;

@Deprecated
@lombok.Data
@lombok.experimental.SuperBuilder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class Entity {
    @Deprecated
    @SuppressWarnings("required")
    private String id;
    @Deprecated
    @SuppressWarnings("required")
    private String name;
    @Deprecated
    @SuppressWarnings("required")
    private String model;
    @Deprecated
    @SuppressWarnings("required")
    private java.util.Map<String, Object> modelModifier;
    @Deprecated
    @SuppressWarnings("optional")
    private EntityModifier modifier;
    @Deprecated
    @SuppressWarnings("required")
    private MovementType movementType;
    @Deprecated
    @SuppressWarnings("required")
    private String controlledBy;
    @Deprecated
    @SuppressWarnings("optional")
    private java.lang.Boolean solid;
    @Deprecated
    @SuppressWarnings("optional")
    private java.lang.Boolean interactive;
    @Deprecated
    @SuppressWarnings("optional")
    private java.lang.Boolean physics;
    @Deprecated
    @SuppressWarnings("optional")
    private java.lang.Boolean clientPhysics;
    @Deprecated
    @SuppressWarnings("optional")
    private java.lang.Boolean notifyOnCollision;
    @Deprecated
    @SuppressWarnings("optional")
    private java.lang.Double notifyOnAttentionRange;
}
