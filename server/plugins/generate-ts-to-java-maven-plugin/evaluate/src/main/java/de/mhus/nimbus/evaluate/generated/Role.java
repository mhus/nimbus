/*
 * Source TS: sample-types.ts
 * Original TS: 'enum Role'
 */
package de.mhus.nimbus.evaluate.generated;

public enum Role {
    USER(1),
    ADMIN(2);

    @lombok.Getter
    private final int tsIndex;
    Role(int tsIndex) { this.tsIndex = tsIndex; }
}
