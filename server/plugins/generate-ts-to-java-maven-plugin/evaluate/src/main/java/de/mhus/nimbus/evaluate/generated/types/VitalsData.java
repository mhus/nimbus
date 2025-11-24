/*
 * Source TS: VitalsData.ts
 * Original TS: 'interface VitalsData'
 */
package de.mhus.nimbus.evaluate.generated.types;

@Deprecated
@lombok.Data
@lombok.experimental.SuperBuilder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class VitalsData {
    @Deprecated
    @SuppressWarnings("required")
    private String type;
    @Deprecated
    @SuppressWarnings("required")
    private double current;
    @Deprecated
    @SuppressWarnings("required")
    private double max;
    @Deprecated
    @SuppressWarnings("optional")
    private java.lang.Double extended;
    @Deprecated
    @SuppressWarnings("optional")
    private java.lang.Double extendExpiry;
    @Deprecated
    @SuppressWarnings("required")
    private double regenRate;
    @Deprecated
    @SuppressWarnings("required")
    private double degenRate;
    @Deprecated
    @SuppressWarnings("required")
    private String color;
    @Deprecated
    @SuppressWarnings("required")
    private String name;
    @Deprecated
    @SuppressWarnings("required")
    private double order;
}
