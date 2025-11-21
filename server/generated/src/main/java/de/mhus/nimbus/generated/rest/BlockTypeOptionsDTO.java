/*
 * Source TS: BlockTypeDTO.ts
 * Original TS: 'interface BlockTypeOptionsDTO'
 */
package de.mhus.nimbus.generated.rest;

@com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
@lombok.Data
@lombok.Builder
public class BlockTypeOptionsDTO extends Object {
    private boolean solid;
    private boolean opaque;
    private boolean transparent;
    private String material;
}
