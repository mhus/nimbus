/*
 * Source TS: Block.ts
 * Original TS: 'interface IBlockWithModifier'
 */
package de.mhus.nimbus.evaluate.generated.types;

@lombok.Data
@lombok.experimental.SuperBuilder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class IBlockWithModifier {
    private Block block;
    private BlockModifier currentModifier;
}
