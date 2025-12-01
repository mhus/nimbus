/*
 * Source TS: BlockModifier.ts
 * Original TS: 'enum SamplingMode'
 */
package de.mhus.nimbus.evaluate.generated.types;

public enum SamplingMode {
    NEAREST(0),
    LINEAR(1),
    MIPMAP(2);

    @lombok.Getter
    private final int tsIndex;
    SamplingMode(int tsIndex) { this.tsIndex = tsIndex; }
}
