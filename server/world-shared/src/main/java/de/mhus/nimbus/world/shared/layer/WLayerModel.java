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
import java.util.List;

/**
 * Model layer entity - entity-oriented storage.
 * Entire block structure stored in MongoDB document.
 * Blocks have relative positions from mount point.
 */
@Document(collection = "w_layer_model")
@CompoundIndexes({
        @CompoundIndex(name = "layerData_idx", def = "{ 'layerDataId': 1 }", unique = true),
        @CompoundIndex(name = "world_layerData_idx", def = "{ 'worldId': 1, 'layerDataId': 1 }")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WLayerModel {

    @Id
    private String id;

    @Indexed
    private String worldId;

    /**
     * References WLayer.layerDataId (1:1 relationship).
     */
    @Indexed
    private String layerDataId;

    /**
     * Layer blocks with relative positions from mount point.
     * Position (0,0,0) = mount point.
     * Position (-1,2,3) = 1 left, 2 up, 3 forward from mount point.
     */
    @Builder.Default
    private List<LayerBlock> content = new ArrayList<>();

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
