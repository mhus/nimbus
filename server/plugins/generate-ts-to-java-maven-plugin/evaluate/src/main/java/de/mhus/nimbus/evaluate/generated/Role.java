/*
 * Source TS: sample-types.ts
 * Original TS: 'enum Role'
 */
package de.mhus.nimbus.evaluate.generated;

public enum Role {
    USER("USER"),
    ADMIN("ADMIN");

    @lombok.Getter
    private final String tsIndex;
    Role(String tsIndex) { this.tsIndex = tsIndex; }
}
