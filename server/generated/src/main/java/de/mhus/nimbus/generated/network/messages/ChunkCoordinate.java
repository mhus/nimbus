/*
 * Source TS: ChunkMessage.ts
 * Original TS: 'interface ChunkCoordinate'
 */
package de.mhus.nimbus.generated.network.messages;

@com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
@lombok.Data
@lombok.experimental.SuperBuilder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class ChunkCoordinate {
    private double cx;
    private double cz;
}
