package de.mhus.nimbus.generated.rest;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Generated from BlockTypeDTO.ts
 * DO NOT EDIT MANUALLY - This file is auto-generated
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BlockTypeDTO {

    /**
     * id
     */
    private double id;

    /**
     * name
     */
    private String name;

    /**
     * displayName
     */
    private String displayName;

    /**
     * shape
     */
    private String shape;

    /**
     * texture
     */
    private String texture;

    /**
     * options
     */
    private BlockTypeOptionsDTO options;

    /**
     * hardness
     */
    private double hardness;

    /**
     * miningtime
     */
    private double miningtime;

    /**
     * tool
     */
    private String tool;

    /**
     * unbreakable
     */
    private boolean unbreakable;

    /**
     * solid
     */
    private boolean solid;

    /**
     * transparent
     */
    private boolean transparent;

    /**
     * windLeafiness
     */
    private double windLeafiness;

    /**
     * windStability
     */
    private double windStability;
}
