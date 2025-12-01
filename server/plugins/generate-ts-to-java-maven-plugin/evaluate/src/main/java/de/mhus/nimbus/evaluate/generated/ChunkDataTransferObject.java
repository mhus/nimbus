/*
 * Source TS: ChunkMessage.ts
 * Original TS: 'interface ChunkDataTransferObject'
 */
package de.mhus.nimbus.evaluate.generated;

@lombok.Data
@lombok.experimental.SuperBuilder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class ChunkDataTransferObject {
    private double cx;
    private double cz;
    private java.util.List<Block> b;
    private java.util.List<ItemBlockRef> i;
    private java.util.List<HeightData> h;
    private java.util.List<AreaData> a;
    private ChunkDataTransferObjectBackdrop backdrop;

    /* Nested helper for inline 'backdrop' */
    @lombok.Data
    @lombok.experimental.SuperBuilder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor(access = lombok.AccessLevel.PROTECTED)
    public static class ChunkDataTransferObjectBackdrop {
        private java.util.List<Backdrop> n;
        private java.util.List<Backdrop> e;
        private java.util.List<Backdrop> s;
        private java.util.List<Backdrop> w;
    }
}
