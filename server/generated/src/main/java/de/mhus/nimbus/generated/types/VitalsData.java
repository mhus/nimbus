package de.mhus.nimbus.generated.types;

@lombok.Data
@lombok.Builder
public class VitalsData extends Object {
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
