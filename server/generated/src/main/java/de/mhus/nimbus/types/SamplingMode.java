/*
 * Source TS: BlockModifier.ts
 * Original TS: 'enum SamplingMode'
 */
package de.mhus.nimbus.types;

public enum SamplingMode implements de.mhus.nimbus.types.TsEnum {
    NEAREST(0),
    LINEAR(1),
    MIPMAP(2);

    @lombok.Getter
    private final int tsIndex;
    SamplingMode(int tsIndex) { this.tsIndex = tsIndex; }
}
