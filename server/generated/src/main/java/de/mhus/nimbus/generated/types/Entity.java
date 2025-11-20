package de.mhus.nimbus.generated.types;

@lombok.Data
@lombok.Builder
public class Entity extends Object {
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
