/*
 * Source TS: EntityData.ts
 * Original TS: 'interface EntityModel'
 */
package de.mhus.nimbus.generated.types;

@com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
@lombok.Data
@lombok.experimental.SuperBuilder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class EntityModel {
    private String id;
    private String type;
    private String modelPath;
    private Vector3 positionOffset;
    private Vector3 rotationOffset;
    private Vector3 scale;
    @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
    private java.lang.Double maxPitch;
    private java.util.Map<ENTITY_POSES, PoseAnimation> poseMapping;
    private PoseType poseType;
    private java.util.Map<String, String> modelModifierMapping;
    private EntityDimensions dimensions;
    @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
    private EntityPhysicsProperties physicsProperties;
    @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
    private java.util.List<AudioDefinition> audio;
}
