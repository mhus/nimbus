package de.mhus.nimbus.world.shared.layer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * Terrain layer entity - chunk-oriented storage.
 * One document per chunk, similar to WChunk.
 * Actual chunk data stored externally via StorageService.
 */
@Document(collection = "w_layer_terrain")
@CompoundIndexes({
        @CompoundIndex(name = "layerData_chunk_idx", def = "{ 'layerDataId': 1, 'chunkKey': 1 }", unique = true),
        @CompoundIndex(name = "world_layerData_idx", def = "{ 'worldId': 1, 'layerDataId': 1 }")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WLayerTerrain {

    @Id
    private String id;

    @Indexed
    private String worldId;

    /**
     * References WLayer.layerDataId.
     */
    @Indexed
    private String layerDataId;

    /**
     * Chunk key identifier (format: "cx:cz").
     */
    @Indexed
    private String chunkKey;

    /**
     * External storage reference (StorageService ID).
     * Similar to WChunk.storageId.
     */
    @Indexed
    private String storageId;

    private Instant createdAt;
    private Instant updatedAt;

    /**
     * Set creation and update timestamps.
     */
    public void touchCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    /**
     * Update the update timestamp.
     */
    public void touchUpdate() {
        updatedAt = Instant.now();
    }
}
