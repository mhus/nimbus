/*
 * Source TS: ChunkData.ts
 * Original TS: 'interface ChunkData'
 */
package de.mhus.nimbus.evaluate.generated.types;

@Deprecated
@lombok.Data
@lombok.experimental.SuperBuilder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class ChunkData {
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
    @SuppressWarnings("required")
    private java.util.List<Block> blocks;
    @Deprecated
    @SuppressWarnings("optional")
    private java.util.List<ItemBlockRef> i;
    @Deprecated
    @SuppressWarnings("optional")
    private java.util.List<HeightData> heightData;
    @Deprecated
    @SuppressWarnings("optional")
    private java.util.List<Status> status;
    @Deprecated
    @SuppressWarnings("optional")
    private ChunkDataBackdrop backdrop;

    /* Nested helper for inline 'backdrop' */
    @Deprecated
    @lombok.Data
    @lombok.experimental.SuperBuilder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor(access = lombok.AccessLevel.PROTECTED)
    public static class ChunkDataBackdrop {
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
