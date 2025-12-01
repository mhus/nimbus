/*
 * Source TS: ChunkData.ts
 * Original TS: 'interface ChunkData'
 */
package de.mhus.nimbus.evaluate.generated.types;

@lombok.Data
@lombok.experimental.SuperBuilder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class ChunkData {
    private double cx;
    private double cz;
    private double size;
    private java.util.List<Block> blocks;
    private java.util.List<ItemBlockRef> i;
    private java.util.List<HeightData> heightData;
    private java.util.List<Status> status;
    private ChunkDataBackdrop backdrop;

    /* Nested helper for inline 'backdrop' */
    @lombok.Data
    @lombok.experimental.SuperBuilder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor(access = lombok.AccessLevel.PROTECTED)
    public static class ChunkDataBackdrop {
        private java.util.List<Backdrop> n;
        private java.util.List<Backdrop> e;
        private java.util.List<Backdrop> s;
        private java.util.List<Backdrop> w;
    }
}
