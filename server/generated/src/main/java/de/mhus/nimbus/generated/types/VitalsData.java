package de.mhus.nimbus.generated.types;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Generated from VitalsData.ts
 * DO NOT EDIT MANUALLY - This file is auto-generated
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VitalsData {

    /**
     * type
     */
    private String type;

    /**
     * current
     */
    private double current;

    /**
     * max
     */
    private double max;

    /**
     * extended (optional)
     */
    private double extended;

    /**
     * extendExpiry (optional)
     */
    private double extendExpiry;

    /**
     * regenRate
     */
    private double regenRate;

    /**
     * degenRate
     */
    private double degenRate;

    /**
     * color
     */
    private String color;

    /**
     * name
     */
    private String name;

    /**
     * order
     */
    private double order;
}
