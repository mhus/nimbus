/*
 * Source TS: BlockType.ts
 * Original TS: 'interface BlockType'
 */
package de.mhus.nimbus.evaluate.generated.types;

@lombok.Data
@lombok.experimental.SuperBuilder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class BlockType {
    private double id;
    private java.lang.Double initialStatus;
    private String description;
    private java.util.Map<java.lang.Double, BlockModifier> modifiers;
}
