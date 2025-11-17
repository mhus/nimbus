package de.mhus.nimbus.generated;

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
    private Offsets offsets;

    /**
     * cornerHeights (optional)
     */
    private [number, number, number, number] cornerHeights;

    /**
     * faceVisibility (optional)
     */
    private FaceVisibility faceVisibility;

    /**
     * status (optional)
     */
    private double status;
}
