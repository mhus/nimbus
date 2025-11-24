/*
 * Source TS: BlockType.ts
 * Original TS: 'enum BlockStatus'
 */
package de.mhus.nimbus.evaluate.generated.types;

public enum BlockStatus {
    DEFAULT(1),
    OPEN(2),
    CLOSED(3),
    LOCKED(4),
    DESTROYED(5),
    WINTER(6),
    WINTER_SPRING(7),
    SPRING(8),
    SPRING_SUMMER(9),
    SUMMER(10),
    SUMMER_AUTUMN(11),
    AUTUMN(12),
    AUTUMN_WINTER(13),
    CUSTOM_START(14);

    @lombok.Getter
    private final int tsIndex;
    BlockStatus(int tsIndex) { this.tsIndex = tsIndex; }
}
