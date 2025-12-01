/*
 * Source TS: BlockType.ts
 * Original TS: 'enum BlockStatus'
 */
package de.mhus.nimbus.generated.types;

public enum BlockStatus {
    DEFAULT(0),
    OPEN(1),
    CLOSED(2),
    LOCKED(3),
    DESTROYED(5),
    WINTER(10),
    SPRING(11),
    SUMMER(12),
    AUTUMN(13),
    CUSTOM_START(100);

    @lombok.Getter
    private final int tsIndex;
    BlockStatus(int tsIndex) { this.tsIndex = tsIndex; }
}
