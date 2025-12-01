/*
 * Source TS: BlockTypeDTO.ts
 * Original TS: 'interface BlockTypeListResponseDTO'
 */
package de.mhus.nimbus.evaluate.generated.rest;

@lombok.Data
@lombok.experimental.SuperBuilder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class BlockTypeListResponseDTO {
    private java.util.List<BlockTypeDTO> blockTypes;
    private double count;
    private double limit;
    private double offset;
}
