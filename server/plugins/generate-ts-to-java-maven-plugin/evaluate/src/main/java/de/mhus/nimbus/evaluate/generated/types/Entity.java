/*
 * Source TS: EntityData.ts
 * Original TS: 'interface Entity'
 */
package de.mhus.nimbus.evaluate.generated.types;

@lombok.Data
@lombok.experimental.SuperBuilder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class Entity {
    private String id;
    private String name;
    private String model;
    private java.util.Map<String, Object> modelModifier;
    private EntityModifier modifier;
    private MovementType movementType;
    private String controlledBy;
    private java.lang.Boolean solid;
    private java.lang.Boolean interactive;
    private java.lang.Boolean physics;
    private java.lang.Boolean clientPhysics;
    private java.lang.Boolean notifyOnCollision;
    private java.lang.Double notifyOnAttentionRange;
}
