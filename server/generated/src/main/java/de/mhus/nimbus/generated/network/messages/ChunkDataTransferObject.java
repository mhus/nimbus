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
    @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
    private java.util.List<de.mhus.nimbus.generated.types.ItemBlockRef> i;
    @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
    private java.util.List<de.mhus.nimbus.generated.types.HeightData> h;
    @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
    private java.util.List<de.mhus.nimbus.generated.types.AreaData> a;
    @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
    private java.util.List<de.mhus.nimbus.generated.types.Backdrop> n;
    @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
    private java.util.List<de.mhus.nimbus.generated.types.Backdrop> e;
    @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
    private java.util.List<de.mhus.nimbus.generated.types.Backdrop> s;
    @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
    private java.util.List<de.mhus.nimbus.generated.types.Backdrop> w;
}
