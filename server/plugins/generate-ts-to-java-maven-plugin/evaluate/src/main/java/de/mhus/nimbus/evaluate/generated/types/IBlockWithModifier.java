/*
 * Source TS: Block.ts
 * Original TS: 'interface IBlockWithModifier'
 */
package de.mhus.nimbus.evaluate.generated.types;

@Deprecated
@lombok.Data
@lombok.experimental.SuperBuilder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class IBlockWithModifier {
    @Deprecated
    @SuppressWarnings("required")
    private Block block;
    @Deprecated
    @SuppressWarnings("required")
    private BlockModifier currentModifier;
}
