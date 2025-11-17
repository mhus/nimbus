package de.mhus.nimbus.generated;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Generated from EffectData.ts
 * DO NOT EDIT MANUALLY - This file is auto-generated
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EffectData {

    /**
     * n
     */
    private String n;

    /**
     * p
     */
    private java.util.Map<String, Object> p;

    /**
     * intensity (optional)
     */
    private double intensity;

    /**
     * color (optional)
     */
    private String color;
}
