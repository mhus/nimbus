package de.mhus.nimbus.generated;

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
public class TextureDefinition {

    /**
     * path
     */
    private String path;

    /**
     * uvMapping (optional)
     */
    private UVMapping uvMapping;

    /**
     * samplingMode (optional)
     */
    private SamplingMode samplingMode;

    /**
     * transparencyMode (optional)
     */
    private TransparencyMode transparencyMode;

    /**
     * opacity (optional)
     */
    private double opacity;

    /**
     * effect (optional)
     */
    private BlockEffect effect;

    /**
     * effectParameters (optional)
     */
    private String effectParameters;

    /**
     * color (optional)
     */
    private String color;

    /**
     * backFaceCulling (optional)
     */
    private boolean backFaceCulling;
}
