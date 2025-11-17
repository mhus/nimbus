package de.mhus.nimbus.generated.types;

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
public class SkyEffect {

    /**
     * intensity (optional)
     */
    private double intensity;

    /**
     * color (optional)
     */
    private String color;

    /**
     * wind (optional)
     */
    private Object wind;
}
