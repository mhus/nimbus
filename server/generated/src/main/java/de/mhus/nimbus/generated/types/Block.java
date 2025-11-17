package de.mhus.nimbus.generated.types;

import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Generated from Block.ts
 * DO NOT EDIT MANUALLY - This file is auto-generated
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Block {

    /**
     * position
     */
    private Vector3 position;

    /**
     * blockTypeId
     */
    private double blockTypeId;

    /**
     * offsets (optional)
     */
    private java.util.List<Double> offsets;

    /**
     * cornerHeights (optional)
     */
    private java.util.List<Double> cornerHeights;

    /**
     * faceVisibility (optional)
     */
    private FaceVisibility faceVisibility;

    /**
     * status (optional)
     */
    private double status;

    /**
     * modifiers (optional)
     */
    private java.util.Map<Double, BlockModifier> modifiers;

    /**
     * metadata (optional)
     */
    private BlockMetadata metadata;
}
