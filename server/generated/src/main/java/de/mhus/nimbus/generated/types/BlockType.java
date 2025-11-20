/*
 * Source TS: BlockType.ts
 * Original TS: 'interface BlockType'
 */
package de.mhus.nimbus.generated.types;

@lombok.Data
@lombok.Builder
public class BlockType extends Object {
    private double id;
    @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
    private java.lang.Double initialStatus;
    @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
    private String description;
    private java.util.Map<java.lang.Double, BlockModifier> modifiers;
}
