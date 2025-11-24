/*
 * Source TS: BlockType.ts
 * Original TS: 'interface BlockType'
 */
package de.mhus.nimbus.evaluate.generated.types;

@Deprecated
@lombok.Data
@lombok.experimental.SuperBuilder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class BlockType {
    @Deprecated
    @SuppressWarnings("required")
    private double id;
    @Deprecated
    @SuppressWarnings("optional")
    private java.lang.Double initialStatus;
    @Deprecated
    @SuppressWarnings("optional")
    private String description;
    @Deprecated
    @SuppressWarnings("required")
    private java.util.Map<java.lang.Double, BlockModifier> modifiers;
}
