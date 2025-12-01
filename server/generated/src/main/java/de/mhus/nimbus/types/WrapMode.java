/*
 * Source TS: BlockModifier.ts
 * Original TS: 'enum WrapMode'
 */
package de.mhus.nimbus.types;

public enum WrapMode implements de.mhus.nimbus.types.TsEnum {
    CLAMP(0),
    REPEAT(1),
    MIRROR(2);

    @lombok.Getter
    private final int tsIndex;
    WrapMode(int tsIndex) { this.tsIndex = tsIndex; }
}
