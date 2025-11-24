/*
 * Source TS: ChunkMessage.ts
 * Original TS: 'interface ChunkRegisterData'
 */
package de.mhus.nimbus.evaluate.generated.network.messages;

@Deprecated
@lombok.Data
@lombok.experimental.SuperBuilder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class ChunkRegisterData {
    @Deprecated
    @SuppressWarnings("required")
    private java.util.List<ChunkCoordinate> c;
}
