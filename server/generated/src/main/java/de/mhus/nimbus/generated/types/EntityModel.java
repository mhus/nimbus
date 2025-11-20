package de.mhus.nimbus.generated.types;

public class EntityModel extends Object {
    public String id;
    public String type;
    public String modelPath;
    public Vector3 positionOffset;
    public Vector3 rotationOffset;
    public Vector3 scale;
    public java.lang.Double maxPitch;
    public java.util.Map<ENTITY_POSES, PoseAnimation> poseMapping;
    public PoseType poseType;
    public java.util.Map<String, String> modelModifierMapping;
    public EntityDimensions dimensions;
    public EntityPhysicsProperties physicsProperties;
    public java.util.List<AudioDefinition> audio;
}
