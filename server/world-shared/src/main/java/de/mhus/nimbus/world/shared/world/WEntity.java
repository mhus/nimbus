package de.mhus.nimbus.world.shared.world;

import de.mhus.nimbus.generated.types.Entity;
import de.mhus.nimbus.shared.persistence.SchemaVersion;
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
 * MongoDB Entity for Entity instances in the world.
 * Wraps generated Entity DTO in 'publicData' field.
 * Entities are actual instances of EntityModels placed in the world (e.g., specific NPCs, players).
 */
@Document(collection = "w_entities")
@SchemaVersion("1.0.0")
@CompoundIndexes({
        @CompoundIndex(name = "worldId_entityId_idx", def = "{ 'worldId': 1, 'entityId': 1 }", unique = true)
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WEntity {

    @Id
    private String id;

    /**
     * World identifier where this entity exists.
     * Required for all entities.
     */
    @Indexed
    private String worldId;

    /**
     * Unique entity identifier within the world.
     * Combined with worldId forms a unique constraint.
     */
    private String entityId;

    /**
     * Public data containing the generated Entity DTO.
     * This is what gets serialized and sent to clients.
     */
    private Entity publicData;

    /**
     * Current chunk location of the entity.
     * Used for spatial queries and chunk-based entity loading.
     */
    @Indexed
    private String chunk;

    /**
     * Reference to the EntityModel template ID.
     * E.g., "cow1", "farmer1" - links to WEntityModel.
     */
    @Indexed
    private String modelId;

    /**
     * Server-side simulation data (not sent to clients).
     * Stored separately from publicData which is client-facing.
     */

    /**
     * Entity's current position in the world.
     * Updated during simulation.
     */
    private de.mhus.nimbus.generated.types.Vector3 position;

    /**
     * Entity's current rotation.
     */
    private de.mhus.nimbus.generated.types.Rotation rotation;

    /**
     * Middle point for entity movement (center of roaming area).
     * Entities typically roam around this point within a certain radius.
     */
    private de.mhus.nimbus.generated.types.Vector3 middlePoint;

    /**
     * Movement radius around middle point (blocks).
     */
    private Double radius;

    /**
     * Movement speed (blocks per second).
     */
    private Double speed;

    /**
     * Behavior model identifier (e.g., "PreyAnimalBehavior").
     * Determines simulation behavior for this entity.
     */
    private String behaviorModel;

    /**
     * Behavior-specific configuration (JSON).
     * Stored as Map for flexibility.
     */
    private java.util.Map<String, Object> behaviorConfig;

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
