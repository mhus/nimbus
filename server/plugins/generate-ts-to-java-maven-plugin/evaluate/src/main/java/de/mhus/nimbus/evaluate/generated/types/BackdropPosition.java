/*
 * Source TS: Backdrop.ts
 * Original TS: 'interface BackdropPosition'
 */
package de.mhus.nimbus.evaluate.generated.types;

@Deprecated
@lombok.Data
@lombok.experimental.SuperBuilder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class BackdropPosition {
    @Deprecated
    @SuppressWarnings("required")
    private double cx;
    @Deprecated
    @SuppressWarnings("required")
    private double cz;
    @Deprecated
    @SuppressWarnings("required")
    private java.util.List<BackdropDirection> directions;
}
