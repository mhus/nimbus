package de.mhus.nimbus.generated;

import java.util.List;
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
public class AnimationEffect {

    /**
     * id (optional)
     */
    private String id;

    /**
     * type
     */
    private AnimationEffectType type;

    /**
     * positions (optional)
     */
    private java.util.List<PositionRef> positions;

    /**
     * params
     */
    private { params;

    /**
     * from (optional)
     */
    private Object from;

    /**
     * to (optional)
     */
    private Object to;

    /**
     * easing (optional)
     */
    private EasingType easing;

    /**
     * speed (optional)
     */
    private double speed;

    /**
     * projectileModel (optional)
     */
    private String projectileModel;

    /**
     * trajectory (optional)
     */
    private 'linear' | 'arc' | 'homing' trajectory;

    /**
     * radius (optional)
     */
    private double radius;

    /**
     * explosionIntensity (optional)
     */
    private double explosionIntensity;

    /**
     * color (optional)
     */
    private String color;

    /**
     * lightIntensity (optional)
     */
    private double lightIntensity;

    /**
     * soundPath (optional)
     */
    private String soundPath;

    /**
     * volume (optional)
     */
    private double volume;
}
