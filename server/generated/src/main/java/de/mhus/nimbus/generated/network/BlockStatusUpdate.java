package de.mhus.nimbus.generated.network;

import de.mhus.nimbus.generated.types.AnimationData;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Generated from BlockMessage.ts
 * DO NOT EDIT MANUALLY - This file is auto-generated
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BlockStatusUpdate {

    /**
     * x
     */
    private double x;

    /**
     * y
     */
    private double y;

    /**
     * z
     */
    private double z;

    /**
     * s
     */
    private double s;

    /**
     * aa (optional)
     */
    private java.util.List<AnimationData> aa;

    /**
     * ab (optional)
     */
    private java.util.List<AnimationData> ab;
}
