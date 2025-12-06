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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Layer entity - main registry for all layers.
 * References specific layer data (LayerTerrain or LayerModel).
 */
@Document(collection = "w_layers")
@CompoundIndexes({
        @CompoundIndex(name = "world_name_idx", def = "{ 'worldId': 1, 'name': 1 }", unique = true),
        @CompoundIndex(name = "world_order_idx", def = "{ 'worldId': 1, 'order': 1 }"),
        @CompoundIndex(name = "world_enabled_idx", def = "{ 'worldId': 1, 'enabled': 1 }")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WLayer {

    @Id
    private String id;

    @Indexed
    private String worldId;

    private String name;

    private LayerType layerType;

    /**
     * Reference to LayerTerrain or LayerModel collection.
     */
    private String layerDataId;

    /**
     * For ModelLayer: mount point X coordinate.
     */
    private Integer mountX;

    /**
     * For ModelLayer: mount point Y coordinate.
     */
    private Integer mountY;

    /**
     * For ModelLayer: mount point Z coordinate.
     */
    private Integer mountZ;

    /**
     * If true, this layer affects all chunks in the world.
     * If false, only chunks in affectedChunks list are affected.
     */
    @Builder.Default
    private boolean allChunks = true;

    /**
     * List of chunk keys (format: "cx:cz") affected by this layer.
     * Only used if allChunks is false.
     */
    @Builder.Default
    private List<String> affectedChunks = new ArrayList<>();

    /**
     * Layer overlay order.
     * Lower values are rendered first (bottom), higher values on top.
     */
    private int order;

    /**
     * Layer enabled flag (soft delete).
     */
    @Builder.Default
    private boolean enabled = true;

    /**
     * List of group names defined in this layer.
     * Blocks can be assigned to groups for organized management.
     */
    @Builder.Default
    private Map<Integer,String> groups = new HashMap<>();

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
