/*
 * Source TS: BlockTypeDTO.ts
 * Original TS: 'interface BlockTypeOptionsDTO'
 */
package de.mhus.nimbus.generated.rest;

@com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
@lombok.Data
@lombok.experimental.SuperBuilder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class BlockTypeOptionsDTO {
    private boolean solid;
    private boolean opaque;
    private boolean transparent;
    private String material;
}
