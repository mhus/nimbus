/*
 * Source TS: ChunkMessage.ts
 * Original TS: 'interface ChunkQueryData'
 */
package de.mhus.nimbus.generated.network.messages;

@com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
@lombok.Data
@lombok.experimental.SuperBuilder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class ChunkQueryData {
    private java.util.List<ChunkCoordinate> c;
}
