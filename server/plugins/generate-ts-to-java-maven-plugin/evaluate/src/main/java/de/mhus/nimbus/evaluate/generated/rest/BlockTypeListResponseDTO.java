/*
 * Source TS: BlockTypeDTO.ts
 * Original TS: 'interface BlockTypeListResponseDTO'
 */
package de.mhus.nimbus.evaluate.generated.rest;

@Deprecated
@lombok.Data
@lombok.experimental.SuperBuilder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class BlockTypeListResponseDTO {
    @Deprecated
    @SuppressWarnings("required")
    private java.util.List<BlockTypeDTO> blockTypes;
    @Deprecated
    @SuppressWarnings("required")
    private double count;
    @Deprecated
    @SuppressWarnings("required")
    private double limit;
    @Deprecated
    @SuppressWarnings("required")
    private double offset;
}
