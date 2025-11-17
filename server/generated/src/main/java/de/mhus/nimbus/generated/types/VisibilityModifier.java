package de.mhus.nimbus.generated.types;

import java.util.List;
import java.util.Map;
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
public class VisibilityModifier {

    /**
     * shape (optional)
     */
    private Shape shape;

    /**
     * effect (optional)
     */
    private BlockEffect effect;

    /**
     * effectParameters (optional)
     */
    private String effectParameters;

    /**
     * offsets (optional)
     */
    private java.util.List<Double> offsets;

    /**
     * scalingX (optional)
     */
    private double scalingX;

    /**
     * scalingY (optional)
     */
    private double scalingY;

    /**
     * scalingZ (optional)
     */
    private double scalingZ;

    /**
     * rotationX (optional)
     */
    private double rotationX;

    /**
     * rotationY (optional)
     */
    private double rotationY;

    /**
     * path (optional)
     */
    private String path;

    /**
     * textures (optional)
     */
    private java.util.Map<Double, Object> textures;

    /**
     * faceVisibility (optional)
     */
    private FaceVisibility faceVisibility;
}
