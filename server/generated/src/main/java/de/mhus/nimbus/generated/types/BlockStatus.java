/*
 * Source TS: BlockType.ts
 * Original TS: 'enum BlockStatus'
 */
package de.mhus.nimbus.generated.types;

public enum BlockStatus {
    DEFAULT(1),
    OPEN(2),
    CLOSED(3),
    LOCKED(4),
    DESTROYED(5),
    WINTER(6),
    SPRING(7),
    SUMMER(8),
    AUTUMN(9),
    CUSTOM_START(10);

    @lombok.Getter
    private final int tsIndex;
    BlockStatus(int tsIndex) { this.tsIndex = tsIndex; }
}
