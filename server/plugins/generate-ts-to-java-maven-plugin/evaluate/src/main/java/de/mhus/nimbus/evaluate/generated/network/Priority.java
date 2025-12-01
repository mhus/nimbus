/*
 * Source TS: MessageTypes.ts
 * Original TS: 'enum Priority'
 */
package de.mhus.nimbus.evaluate.generated.network;

public enum Priority {
    LOW(0),
    MEDIUM(1),
    HIGH(2),
    CRITICAL(5);

    @lombok.Getter
    private final int tsIndex;
    Priority(int tsIndex) { this.tsIndex = tsIndex; }
}
