/*
 * Source TS: VitalsData.ts
 * Original TS: 'interface VitalsData'
 */
package de.mhus.nimbus.evaluate.generated.types;

@lombok.Data
@lombok.experimental.SuperBuilder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class VitalsData {
    private String type;
    private double current;
    private double max;
    private java.lang.Double extended;
    private java.lang.Double extendExpiry;
    private double regenRate;
    private double degenRate;
    private String color;
    private String name;
    private double order;
}
