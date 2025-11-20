package de.mhus.nimbus.generated.types;

public interface Entity {
    String getId();
    String getName();
    String getModel();
    java.util.Map<String, Object> getModelModifier();
    EntityModifier getModifier();
    MovementType getMovementType();
    String getControlledBy();
    java.lang.Boolean getSolid();
    java.lang.Boolean getInteractive();
    java.lang.Boolean getPhysics();
    java.lang.Boolean getClientPhysics();
    java.lang.Boolean getNotifyOnCollision();
    java.lang.Double getNotifyOnAttentionRange();
}
