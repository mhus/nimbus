package de.mhus.nimbus.generated;

import java.util.List;
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
    private java.util.List<HeightData> heightData;

    /**
     * status (optional)
     */
    private java.util.List<Status> status;

    /**
     * backdrop (optional)
     */
    private { backdrop;

    /**
     * n (optional)
     */
    private Array<Backdrop> n;

    /**
     * e (optional)
     */
    private Array<Backdrop> e;

    /**
     * s (optional)
     */
    private Array<Backdrop> s;

    /**
     * w (optional)
     */
    private Array<Backdrop> w;
}
