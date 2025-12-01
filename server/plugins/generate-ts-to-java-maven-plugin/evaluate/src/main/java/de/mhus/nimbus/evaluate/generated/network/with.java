/*
 * Source TS: MessageTypes.ts
 * Original TS: 'enum with'
 */
package de.mhus.nimbus.evaluate.generated.network;

public enum with {
    STRING_VAL("text"),
    NUMERIC_VAL("42"),
    ANOTHER_STRING("hello");

    @lombok.Getter
    private final String tsIndex;
    with(String tsIndex) { this.tsIndex = tsIndex; }
}
