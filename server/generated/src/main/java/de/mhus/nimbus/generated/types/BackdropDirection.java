/*
 * Source TS: Backdrop.ts
 * Original TS: 'enum BackdropDirection'
 */
package de.mhus.nimbus.generated.types;

public enum BackdropDirection {
    NORTH(1),
    EAST(2),
    SOUTH(3),
    WEST(4);

    @lombok.Getter
    private final int tsIndex;
    BackdropDirection(int tsIndex) { this.tsIndex = tsIndex; }
}
