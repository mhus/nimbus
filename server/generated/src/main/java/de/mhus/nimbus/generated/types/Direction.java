/*
 * Source TS: BlockModifier.ts
 * Original TS: 'enum Direction'
 */
package de.mhus.nimbus.generated.types;

public enum Direction {
    NORTH(1),
    SOUTH(2),
    EAST(3),
    WEST(4),
    UP(5),
    DOWN(6);

    @lombok.Getter
    private final int tsIndex;
    Direction(int tsIndex) { this.tsIndex = tsIndex; }
}
