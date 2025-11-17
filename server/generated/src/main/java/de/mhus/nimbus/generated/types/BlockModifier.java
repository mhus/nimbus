package de.mhus.nimbus.generated.types;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Generated from BlockModifier.ts
 * DO NOT EDIT MANUALLY - This file is auto-generated
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BlockModifier {

    /**
     * visibility (optional)
     */
    private VisibilityModifier visibility;

    /**
     * wind (optional)
     */
    private WindModifier wind;

    /**
     * illumination (optional)
     */
    private IlluminationModifier illumination;

    /**
     * physics (optional)
     */
    private PhysicsModifier physics;

    /**
     * effects (optional)
     */
    private EffectsModifier effects;

    /**
     * audio (optional)
     */
    private java.util.List<AudioDefinition> audio;
}
