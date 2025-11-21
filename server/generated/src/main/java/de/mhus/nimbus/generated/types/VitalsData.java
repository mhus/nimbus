/*
 * Source TS: VitalsData.ts
 * Original TS: 'interface VitalsData'
 */
package de.mhus.nimbus.generated.types;

@com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
@lombok.Data
@lombok.Builder
public class VitalsData extends Object {
    private String type;
    private double current;
    private double max;
    @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
    private java.lang.Double extended;
    @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
    private java.lang.Double extendExpiry;
    private double regenRate;
    private double degenRate;
    private String color;
    private String name;
    private double order;
}
