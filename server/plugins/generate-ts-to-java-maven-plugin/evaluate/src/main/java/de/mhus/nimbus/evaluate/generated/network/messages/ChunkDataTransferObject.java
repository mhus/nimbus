/*
 * Source TS: ChunkMessage.ts
 * Original TS: 'interface ChunkDataTransferObject'
 */
package de.mhus.nimbus.evaluate.generated.network.messages;

@Deprecated
@lombok.Data
@lombok.experimental.SuperBuilder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class ChunkDataTransferObject {
    @Deprecated
    @SuppressWarnings("required")
    private double cx;
    @Deprecated
    @SuppressWarnings("required")
    private double cz;
    @Deprecated
    @SuppressWarnings("required")
    private java.util.List<de.mhus.nimbus.evaluate.generated.types.Block> b;
    @Deprecated
    @SuppressWarnings("optional")
    private java.util.List<de.mhus.nimbus.evaluate.generated.types.ItemBlockRef> i;
    @Deprecated
    @SuppressWarnings("optional")
    private java.util.List<de.mhus.nimbus.evaluate.generated.types.HeightData> h;
    @Deprecated
    @SuppressWarnings("optional")
    private java.util.List<de.mhus.nimbus.evaluate.generated.types.AreaData> a;
    @Deprecated
    @SuppressWarnings("optional")
    private ChunkDataTransferObjectBackdrop backdrop;

    /* Nested helper for inline 'backdrop' */
    @Deprecated
    @lombok.Data
    @lombok.experimental.SuperBuilder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor(access = lombok.AccessLevel.PROTECTED)
    public static class ChunkDataTransferObjectBackdrop {
    @Deprecated
    @SuppressWarnings("optional")
        private java.util.List<de.mhus.nimbus.evaluate.generated.types.Backdrop> n;
    @Deprecated
    @SuppressWarnings("optional")
        private java.util.List<de.mhus.nimbus.evaluate.generated.types.Backdrop> e;
    @Deprecated
    @SuppressWarnings("optional")
        private java.util.List<de.mhus.nimbus.evaluate.generated.types.Backdrop> s;
    @Deprecated
    @SuppressWarnings("optional")
        private java.util.List<de.mhus.nimbus.evaluate.generated.types.Backdrop> w;
    }
}
