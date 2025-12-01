/*
 * Source TS: BlockModifier.ts
 * Original TS: 'enum WrapMode'
 */
package de.mhus.nimbus.generated.types;

public enum WrapMode {
    CLAMP(0),
    REPEAT(1),
    MIRROR(2);

    @lombok.Getter
    private final int tsIndex;
    WrapMode(int tsIndex) { this.tsIndex = tsIndex; }
}
