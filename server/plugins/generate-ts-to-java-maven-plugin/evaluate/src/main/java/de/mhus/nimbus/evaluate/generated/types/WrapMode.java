/*
 * Source TS: BlockModifier.ts
 * Original TS: 'enum WrapMode'
 */
package de.mhus.nimbus.evaluate.generated.types;

public enum WrapMode {
    CLAMP(1),
    REPEAT(2),
    MIRROR(3);

    @lombok.Getter
    private final int tsIndex;
    WrapMode(int tsIndex) { this.tsIndex = tsIndex; }
}
