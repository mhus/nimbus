package de.mhus.nimbus.world.shared.world;

import de.mhus.nimbus.shared.annotations.GenerateTypeScript;
import de.mhus.nimbus.shared.annotations.TypeScript;
import de.mhus.nimbus.shared.persistence.ActualSchemaVersion;
import de.mhus.nimbus.shared.types.Identifiable;
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
 * MongoDB Entity for storing arbitrary data objects.
 * Flexible storage for any kind of data that needs to be persisted
 * with optional scoping by region, world, and collection.
 */
@Document(collection = "w_anything")
@ActualSchemaVersion("1.0.0")
@CompoundIndexes({
        @CompoundIndex(name = "region_world_collection_name_idx",
                       def = "{ 'regionId': 1, 'worldId': 1, 'collection': 1, 'name': 1 }")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@GenerateTypeScript("entities")
public class WAnything implements Identifiable {

    @Id
    @TypeScript(ignore = true)
    private String id;

    /**
     * Optional region identifier for scoped search.
     */
    @Indexed
    @TypeScript(optional = true)
    private String regionId;

    /**
     * Optional world identifier for scoped search.
     */
    @Indexed
    @TypeScript(optional = true)
    private String worldId;

    /**
     * Collection identifier for grouping related data.
     * Required field to categorize the data.
     */
    @Indexed
    private String collection;

    /**
     * Name/identifier for this data object.
     * Required field for lookup within a collection.
     */
    @Indexed
    @TypeScript(optional = true)
    private String name;

    /**
     * Human-readable description of the data.
     */
    private String description;

    /**
     * Type identifier for categorizing the data.
     * Can be used to distinguish different data types within a collection.
     */
    @Indexed
    @TypeScript(optional = true)
    private String type;

    /**
     * Arbitrary data object stored as generic Object.
     * Can contain any serializable data structure.
     */
    private Object data;

    private Instant createdAt;
    private Instant updatedAt;

    /**
     * Soft delete flag.
     */
    @Indexed
    @Builder.Default
    private boolean enabled = true;

    /**
     * Initialize timestamps for new entity.
     */
    public void touchCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    /**
     * Update modification timestamp.
     */
    public void touchUpdate() {
        updatedAt = Instant.now();
    }
}
