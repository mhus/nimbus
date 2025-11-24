/*
 * Source TS: WithBackdrop.ts
 * Original TS: 'interface WithBackdrop'
 */
package de.mhus.nimbus.evaluate.generated.types;

@Deprecated
@lombok.Data
@lombok.experimental.SuperBuilder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class WithBackdrop {
    @Deprecated
    @SuppressWarnings("required")
    private double cx;
    @Deprecated
    @SuppressWarnings("required")
    private double cz;
    @Deprecated
    @SuppressWarnings("required")
    private double size;
    @Deprecated
    @SuppressWarnings("optional")
    private WithBackdropBackdrop backdrop;

    /* Nested helper for inline 'backdrop' */
    @Deprecated
    @lombok.Data
    @lombok.experimental.SuperBuilder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor(access = lombok.AccessLevel.PROTECTED)
    public static class WithBackdropBackdrop {
    @Deprecated
    @SuppressWarnings("optional")
        private java.util.List<Backdrop> n;
    @Deprecated
    @SuppressWarnings("optional")
        private java.util.List<Backdrop> e;
    @Deprecated
    @SuppressWarnings("optional")
        private java.util.List<Backdrop> s;
    @Deprecated
    @SuppressWarnings("optional")
        private java.util.List<Backdrop> w;
    }
}
