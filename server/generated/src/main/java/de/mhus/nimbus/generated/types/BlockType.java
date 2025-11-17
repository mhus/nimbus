package de.mhus.nimbus.generated.types;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Generated from BlockType.ts
 * DO NOT EDIT MANUALLY - This file is auto-generated
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BlockType {

    /**
     * id
     */
    private double id;

    /**
     * initialStatus (optional)
     */
    private double initialStatus;

    /**
     * description (optional)
     */
    private String description;

    /**
     * modifiers
     */
    private java.util.Map<Double, BlockModifier> modifiers;
}
