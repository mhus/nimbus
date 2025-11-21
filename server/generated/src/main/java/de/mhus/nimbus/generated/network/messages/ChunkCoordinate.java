/*
 * Source TS: ChunkMessage.ts
 * Original TS: 'interface ChunkCoordinate'
 */
package de.mhus.nimbus.generated.network.messages;

@com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
@lombok.Data
@lombok.Builder
public class ChunkCoordinate extends Object {
    private double cx;
    private double cz;
}
