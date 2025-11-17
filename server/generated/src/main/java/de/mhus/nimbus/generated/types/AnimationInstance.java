package de.mhus.nimbus.generated.types;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Generated from AnimationData.ts
 * DO NOT EDIT MANUALLY - This file is auto-generated
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnimationInstance {

    /**
     * templateId
     */
    private String templateId;

    /**
     * animation
     */
    private AnimationData animation;

    /**
     * createdAt
     */
    private double createdAt;

    /**
     * triggeredBy (optional)
     */
    private String triggeredBy;
}
