/*
 * Source TS: BlockModifier.ts
 * Original TS: 'enum AudioType'
 */
package de.mhus.nimbus.evaluate.generated.types;

public enum AudioType {
    STEPS(1),
    PERMANENT(2),
    COLLISION(3);

    @lombok.Getter
    private final int tsIndex;
    AudioType(int tsIndex) { this.tsIndex = tsIndex; }
}
