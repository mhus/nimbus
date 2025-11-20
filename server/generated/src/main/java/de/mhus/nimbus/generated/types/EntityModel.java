/*
 * Source TS: EntityData.ts
 * Original TS: 'interface EntityModel'
 */
package de.mhus.nimbus.generated.types;

@lombok.Data
@lombok.Builder
public class EntityModel extends Object {
    private String id;
    private String type;
    private String modelPath;
    private Vector3 positionOffset;
    private Vector3 rotationOffset;
    private Vector3 scale;
    private java.lang.Double maxPitch;
    private java.util.Map<ENTITY_POSES, PoseAnimation> poseMapping;
    private PoseType poseType;
    private java.util.Map<String, String> modelModifierMapping;
    private EntityDimensions dimensions;
    private EntityPhysicsProperties physicsProperties;
    private java.util.List<AudioDefinition> audio;
}
