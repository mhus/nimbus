/*
 * Source TS: BlockModifier.ts
 * Original TS: 'enum AudioType'
 */
package de.mhus.nimbus.generated.types;

public enum AudioType {
    STEPS("steps"),
    PERMANENT("permanent"),
    COLLISION("collision");

    @lombok.Getter
    private final String tsIndex;
    AudioType(String tsIndex) { this.tsIndex = tsIndex; }
}
