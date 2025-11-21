/*
 * Source TS: BlockTypeDTO.ts
 * Original TS: 'interface BlockTypeListResponseDTO'
 */
package de.mhus.nimbus.generated.rest;

@com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
@lombok.Data
@lombok.Builder
public class BlockTypeListResponseDTO extends Object {
    private java.util.List<BlockTypeDTO> blockTypes;
    private double count;
    private double limit;
    private double offset;
}
