/*
 * Source TS: ChunkMessage.ts
 * Original TS: 'interface ChunkRegisterData'
 */
package de.mhus.nimbus.generated.network.messages;

@com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
@lombok.Data
@lombok.Builder
public class ChunkRegisterData extends Object {
    private java.util.List<ChunkCoordinate> c;
}
