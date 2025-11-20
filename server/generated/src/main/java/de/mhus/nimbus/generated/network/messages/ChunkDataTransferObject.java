/*
 * Source TS: ChunkMessage.ts
 * Original TS: 'interface ChunkDataTransferObject'
 */
package de.mhus.nimbus.generated.network.messages;

@lombok.Data
@lombok.Builder
public class ChunkDataTransferObject extends Object {
    private double cx;
    private double cz;
    private java.util.List<de.mhus.nimbus.generated.types.Block> b;
    private java.util.List<de.mhus.nimbus.generated.types.ItemBlockRef> i;
    private java.util.List<de.mhus.nimbus.generated.types.HeightData> h;
    private java.util.List<de.mhus.nimbus.generated.types.AreaData> a;
    private java.util.List<de.mhus.nimbus.generated.types.Backdrop> n;
    private java.util.List<de.mhus.nimbus.generated.types.Backdrop> e;
    private java.util.List<de.mhus.nimbus.generated.types.Backdrop> s;
    private java.util.List<de.mhus.nimbus.generated.types.Backdrop> w;
}
