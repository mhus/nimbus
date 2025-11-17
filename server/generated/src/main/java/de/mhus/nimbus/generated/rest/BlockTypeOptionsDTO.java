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
public class BlockTypeOptionsDTO {

    /**
     * solid
     */
    private boolean solid;

    /**
     * opaque
     */
    private boolean opaque;

    /**
     * transparent
     */
    private boolean transparent;

    /**
     * material
     */
    private String material;
}
