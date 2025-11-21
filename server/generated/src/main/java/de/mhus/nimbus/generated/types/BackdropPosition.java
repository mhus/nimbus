/*
 * Source TS: Backdrop.ts
 * Original TS: 'interface BackdropPosition'
 */
package de.mhus.nimbus.generated.types;

@com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
@lombok.Data
@lombok.experimental.SuperBuilder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class BackdropPosition {
    private double cx;
    private double cz;
    private java.util.List<BackdropDirection> directions;
}
