/*
 * Source TS: BlockType.ts
 * Original TS: 'enum BlockStatus'
 */
package de.mhus.nimbus.evaluate.generated.types;

public enum BlockStatus {
    DEFAULT(0),
    OPEN(1),
    CLOSED(2),
    LOCKED(3),
    DESTROYED(5),
    WINTER(10),
    WINTER_SPRING(11),
    SPRING(12),
    SPRING_SUMMER(13),
    SUMMER(14),
    SUMMER_AUTUMN(15),
    AUTUMN(16),
    AUTUMN_WINTER(17),
    CUSTOM_START(100);

    @lombok.Getter
    private final int tsIndex;
    BlockStatus(int tsIndex) { this.tsIndex = tsIndex; }
}
