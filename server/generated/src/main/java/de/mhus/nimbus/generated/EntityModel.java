package de.mhus.nimbus.generated;

import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Generated from EntityData.ts
 * DO NOT EDIT MANUALLY - This file is auto-generated
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EntityModel {

    /**
     * id
     */
    private String id;

    /**
     * type
     */
    private String type;

    /**
     * modelPath
     */
    private String modelPath;

    /**
     * positionOffset
     */
    private Vector3 positionOffset;

    /**
     * rotationOffset
     */
    private Vector3 rotationOffset;

    /**
     * scale
     */
    private Vector3 scale;

    /**
     * maxPitch (optional)
     */
    private double maxPitch;

    /**
     * poseMapping
     */
    private java.util.Map<ENTITY_POSES, PoseAnimation> poseMapping;

    /**
     * poseType
     */
    private String poseType;

    /**
     * modelModifierMapping
     */
    private java.util.Map<String, String> modelModifierMapping;

    /**
     * dimensions
     */
    private EntityDimensions dimensions;

    /**
     * physicsProperties (optional)
     */
    private EntityPhysicsProperties physicsProperties;

    /**
     * audio (optional)
     */
    private java.util.List<AudioDefinition> audio;
}
