/*
 * Source TS: Block.ts
 * Original TS: 'enum FaceFlag'
 */
package de.mhus.nimbus.generated.types;

public enum FaceFlag {
    TOP(1),
    BOTTOM(2),
    LEFT(3),
    RIGHT(4),
    FRONT(5),
    BACK(6),
    FIXED(7);

    @lombok.Getter
    private final int tsIndex;
    FaceFlag(int tsIndex) { this.tsIndex = tsIndex; }
}
