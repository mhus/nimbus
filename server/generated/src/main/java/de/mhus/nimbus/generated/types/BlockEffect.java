/*
 * Source TS: BlockModifier.ts
 * Original TS: 'enum BlockEffect'
 */
package de.mhus.nimbus.generated.types;

public enum BlockEffect {
    NONE(1),
    WIND(2);

    @lombok.Getter
    private final int tsIndex;
    BlockEffect(int tsIndex) { this.tsIndex = tsIndex; }
}
