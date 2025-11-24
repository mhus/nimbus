/*
 * Source TS: EntityData.ts
 * Original TS: 'interface EntityModel'
 */
package de.mhus.nimbus.evaluate.generated.types;

@Deprecated
@lombok.Data
@lombok.experimental.SuperBuilder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class EntityModel {
    @Deprecated
    @SuppressWarnings("required")
    private String id;
    @Deprecated
    @SuppressWarnings("required")
    private String type;
    @Deprecated
    @SuppressWarnings("required")
    private String modelPath;
    @Deprecated
    @SuppressWarnings("required")
    private Vector3 positionOffset;
    @Deprecated
    @SuppressWarnings("required")
    private Vector3 rotationOffset;
    @Deprecated
    @SuppressWarnings("required")
    private Vector3 scale;
    @Deprecated
    @SuppressWarnings("optional")
    private java.lang.Double maxPitch;
    @Deprecated
    @SuppressWarnings("required")
    private java.util.Map<ENTITY_POSES, PoseAnimation> poseMapping;
    @Deprecated
    @SuppressWarnings("required")
    private PoseType poseType;
    @Deprecated
    @SuppressWarnings("required")
    private java.util.Map<String, String> modelModifierMapping;
    @Deprecated
    @SuppressWarnings("required")
    private EntityDimensions dimensions;
    @Deprecated
    @SuppressWarnings("optional")
    private EntityPhysicsProperties physicsProperties;
    @Deprecated
    @SuppressWarnings("optional")
    private java.util.List<AudioDefinition> audio;
}
