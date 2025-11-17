package de.mhus.nimbus.generated;

import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Generated from ChunkData.ts
 * DO NOT EDIT MANUALLY - This file is auto-generated
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChunkData {

    /**
     * cx
     */
    private double cx;

    /**
     * cz
     */
    private double cz;

    /**
     * size
     */
    private double size;

    /**
     * blocks
     */
    private java.util.List<Block> blocks;

    /**
     * i (optional)
     */
    private java.util.List<Block> i;

    /**
     * heightData (optional)
     */
    private java.util.List<java.util.List<Double>> heightData;

    /**
     * status (optional)
     */
    private java.util.List<java.util.List<Double>> status;

    /**
     * backdrop (optional)
     */
    private java.util.Map<String, Object> backdrop;

    /**
     * n (optional)
     */
    private java.util.List<Backdrop> n;

    /**
     * e (optional)
     */
    private java.util.List<Backdrop> e;

    /**
     * s (optional)
     */
    private java.util.List<Backdrop> s;

    /**
     * w (optional)
     */
    private java.util.List<Backdrop> w;
}
