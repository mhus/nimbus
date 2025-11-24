/*
 * Source TS: BlockModifier.ts
 * Original TS: 'enum SamplingMode'
 */
package de.mhus.nimbus.evaluate.generated.types;

public enum SamplingMode {
    NEAREST(1),
    LINEAR(2),
    MIPMAP(3);

    @lombok.Getter
    private final int tsIndex;
    SamplingMode(int tsIndex) { this.tsIndex = tsIndex; }
}
