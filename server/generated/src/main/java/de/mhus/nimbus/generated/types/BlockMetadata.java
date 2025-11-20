/*
 * Source TS: BlockMetadata.ts
 * Original TS: 'interface BlockMetadata'
 */
package de.mhus.nimbus.generated.types;

@lombok.Data
@lombok.Builder
public class BlockMetadata extends Object {
    @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
    private String id;
    @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
    private String displayName;
    @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
    private java.lang.Double groupId;
}
