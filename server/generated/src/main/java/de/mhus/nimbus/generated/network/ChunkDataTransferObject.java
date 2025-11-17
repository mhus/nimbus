package de.mhus.nimbus.generated.network;

import de.mhus.nimbus.generated.types.AreaData;
import de.mhus.nimbus.generated.types.Block;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Generated from ChunkMessage.ts
 * DO NOT EDIT MANUALLY - This file is auto-generated
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChunkDataTransferObject {

    /**
     * cx
     */
    private double cx;

    /**
     * cz
     */
    private double cz;

    /**
     * b
     */
    private java.util.List<Block> b;

    /**
     * i (optional)
     */
    private java.util.List<Block> i;

    /**
     * h (optional)
     */
    private java.util.List<java.util.List<Double>> h;

    /**
     * a (optional)
     */
    private java.util.List<AreaData> a;

    /**
     * backdrop (optional)
     */
    private java.util.Map<String, Object> backdrop;
}
