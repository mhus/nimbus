package de.mhus.nimbus.generated.rest;

import java.util.List;
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
public class BlockTypeListResponseDTO {

    /**
     * blockTypes
     */
    private java.util.List<BlockTypeDTO> blockTypes;

    /**
     * count
     */
    private double count;

    /**
     * limit
     */
    private double limit;

    /**
     * offset
     */
    private double offset;
}
