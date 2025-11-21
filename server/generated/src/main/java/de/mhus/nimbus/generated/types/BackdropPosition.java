/*
 * Source TS: Backdrop.ts
 * Original TS: 'interface BackdropPosition'
 */
package de.mhus.nimbus.generated.types;

@com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
@lombok.Data
@lombok.Builder
public class BackdropPosition extends Object {
    private double cx;
    private double cz;
    private java.util.List<BackdropDirection> directions;
}
