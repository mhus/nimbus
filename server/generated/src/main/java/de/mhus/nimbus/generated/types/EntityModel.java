package de.mhus.nimbus.generated.types;

public interface EntityModel {
    String getId();
    String getType();
    String getModelPath();
    Vector3 getPositionOffset();
    Vector3 getRotationOffset();
    Vector3 getScale();
    java.lang.Double getMaxPitch();
    java.util.Map<ENTITY_POSES, PoseAnimation> getPoseMapping();
    PoseType getPoseType();
    java.util.Map<String, String> getModelModifierMapping();
    EntityDimensions getDimensions();
    EntityPhysicsProperties getPhysicsProperties();
    java.util.List<AudioDefinition> getAudio();
}
