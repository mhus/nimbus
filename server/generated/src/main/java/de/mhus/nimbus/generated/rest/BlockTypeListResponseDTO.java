/*
 * Source TS: BlockTypeDTO.ts
 * Original TS: 'interface BlockTypeListResponseDTO'
 */
package de.mhus.nimbus.generated.rest;

@com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
@org.springframework.aot.hint.annotation.Reflective
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
