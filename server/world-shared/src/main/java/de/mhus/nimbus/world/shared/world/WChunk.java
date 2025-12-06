package de.mhus.nimbus.world.shared.world;

import de.mhus.nimbus.shared.persistence.SchemaVersion;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * MongoDB Speicherung eines Welt-Chunks.
 * Uniqueness: (regionId, worldId, chunk).
 * Inline wird ein JSON String des ChunkData gespeichert (content). Ist der JSON zu groß,
 * wird er extern über storageId referenziert und content bleibt null.
 */
@Document(collection = "w_chunks")
@SchemaVersion("1.0.0")
@CompoundIndexes({
        @CompoundIndex(name = "region_world_chunk_idx", def = "{ 'regionId': 1, 'worldId': 1, 'chunk': 1 }", unique = true)
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WChunk {

    @Id
    private String id;

    @Indexed
    private String regionId;

    @Indexed
    private String worldId;

    /** Chunk Identifier (z.B. Koordinaten serialisiert). */
    @Indexed
    private String chunk;

    @Indexed
    private String storageId;

    private Instant createdAt;
    private Instant updatedAt;

    public void touchCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    public void touchUpdate() { updatedAt = Instant.now(); }
}
