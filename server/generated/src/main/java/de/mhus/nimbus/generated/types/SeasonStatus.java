/*
 * Source TS: World.ts
 * Original TS: 'enum SeasonStatus'
 */
package de.mhus.nimbus.generated.types;

public enum SeasonStatus {
    NONE(1),
    WINTER(2),
    SPRING(3),
    SUMMER(4),
    AUTUMN(5);

    @lombok.Getter
    private final int tsIndex;
    SeasonStatus(int tsIndex) { this.tsIndex = tsIndex; }
}
