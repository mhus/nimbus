/*
 * Source TS: MessageTypes.ts
 * Original TS: 'enum MixedEnum'
 */
package de.mhus.nimbus.evaluate.generated.network;

public enum MixedEnum {
    STRING_VAL("text"),
    NUMERIC_VAL("42"),
    ANOTHER_STRING("hello");

    @lombok.Getter
    private final String tsIndex;
    MixedEnum(String tsIndex) { this.tsIndex = tsIndex; }
}
