/*
 * Source TS: World.ts
 * Original TS: 'enum SeasonStatus'
 */
package de.mhus.nimbus.generated.types;

public enum SeasonStatus {
    NONE(0),
    WINTER(1),
    SPRING(2),
    SUMMER(3),
    AUTUMN(4);

    @lombok.Getter
    private final int tsIndex;
    SeasonStatus(int tsIndex) { this.tsIndex = tsIndex; }
}
