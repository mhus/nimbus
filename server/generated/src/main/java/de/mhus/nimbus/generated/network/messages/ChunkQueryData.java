/*
 * Source TS: ChunkMessage.ts
 * Original TS: 'interface ChunkQueryData'
 */
package de.mhus.nimbus.generated.network.messages;

@com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
@lombok.Data
@lombok.Builder
public class ChunkQueryData extends Object {
    private java.util.List<ChunkCoordinate> c;
}
