/*
 * Source TS: BlockModifier.ts
 * Original TS: 'enum BlockEffect'
 */
package de.mhus.nimbus.types;

public enum BlockEffect implements de.mhus.nimbus.types.TsEnum {
    NONE(0),
    WIND(2);

    @lombok.Getter
    private final int tsIndex;
    BlockEffect(int tsIndex) { this.tsIndex = tsIndex; }
}
