/*
 * Source TS: ChunkMessage.ts
 * Original TS: 'interface ChunkDataTransferObject'
 */
package de.mhus.nimbus.evaluate.generated.network.messages;

@lombok.Data
@lombok.experimental.SuperBuilder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class ChunkDataTransferObject {
    private double cx;
    private double cz;
    private java.util.List<de.mhus.nimbus.evaluate.generated.types.Block> b;
    private java.util.List<de.mhus.nimbus.evaluate.generated.types.ItemBlockRef> i;
    private java.util.List<de.mhus.nimbus.evaluate.generated.types.HeightData> h;
    private java.util.List<de.mhus.nimbus.evaluate.generated.types.AreaData> a;
    private ChunkDataTransferObjectBackdrop backdrop;

    /* Nested helper for inline 'backdrop' */
    @lombok.Data
    @lombok.experimental.SuperBuilder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor(access = lombok.AccessLevel.PROTECTED)
    public static class ChunkDataTransferObjectBackdrop {
        private java.util.List<de.mhus.nimbus.evaluate.generated.types.Backdrop> n;
        private java.util.List<de.mhus.nimbus.evaluate.generated.types.Backdrop> e;
        private java.util.List<de.mhus.nimbus.evaluate.generated.types.Backdrop> s;
        private java.util.List<de.mhus.nimbus.evaluate.generated.types.Backdrop> w;
    }
}
