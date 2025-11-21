/*
 * Source TS: Block.ts
 * Original TS: 'interface IBlockWithModifier'
 */
package de.mhus.nimbus.generated.types;

@com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
@lombok.Data
@lombok.experimental.SuperBuilder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class IBlockWithModifier {
    private Block block;
    private BlockModifier currentModifier;
}
