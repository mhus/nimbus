/*
 * Source TS: LayerType.ts
 * Original TS: 'enum LayerType'
 */
package de.mhus.nimbus.generated.generated;

public enum LayerType implements de.mhus.nimbus.types.TsEnum {
    GROUND("GROUND"),
    MODEL("MODEL");

    @lombok.Getter
    private final String tsIndex;
    LayerType(String tsIndex) { this.tsIndex = tsIndex; }
    public String tsString() { return this.tsIndex; }
}
