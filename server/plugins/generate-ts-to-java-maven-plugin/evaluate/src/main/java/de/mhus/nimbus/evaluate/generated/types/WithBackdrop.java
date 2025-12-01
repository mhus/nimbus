/*
 * Source TS: WithBackdrop.ts
 * Original TS: 'interface WithBackdrop'
 */
package de.mhus.nimbus.evaluate.generated.types;

@lombok.Data
@lombok.experimental.SuperBuilder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class WithBackdrop {
    private double cx;
    private double cz;
    private double size;
    private WithBackdropBackdrop backdrop;

    /* Nested helper for inline 'backdrop' */
    @lombok.Data
    @lombok.experimental.SuperBuilder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor(access = lombok.AccessLevel.PROTECTED)
    public static class WithBackdropBackdrop {
        private java.util.List<Backdrop> n;
        private java.util.List<Backdrop> e;
        private java.util.List<Backdrop> s;
        private java.util.List<Backdrop> w;
    }
}
