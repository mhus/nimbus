/*
 * Source TS: BlockModifier.ts
 * Original TS: 'enum Direction'
 */
package de.mhus.nimbus.evaluate.generated.types;

public enum Direction {
    NORTH("1 << 0"),
    SOUTH("1 << 1"),
    EAST("1 << 2"),
    WEST("1 << 3"),
    UP("1 << 4"),
    DOWN("1 << 5");

    @lombok.Getter
    private final String tsIndex;
    Direction(String tsIndex) { this.tsIndex = tsIndex; }
}
