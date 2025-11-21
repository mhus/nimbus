/*
 * Source TS: Block.ts
 * Original TS: 'interface IBlockWithModifier'
 */
package de.mhus.nimbus.generated.types;

@com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
@lombok.Data
@lombok.Builder
public class IBlockWithModifier extends Object {
    private Block block;
    private BlockModifier currentModifier;
}
