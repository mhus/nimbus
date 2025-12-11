package de.mhus.nimbus.world.shared.world;

import de.mhus.nimbus.generated.types.WorldInfo;
import de.mhus.nimbus.shared.persistence.ActualSchemaVersion;
import de.mhus.nimbus.shared.types.Identifiable;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "worlds")
@ActualSchemaVersion("1.0.0")
public class WWorld implements Identifiable {

    @Id
    private String id; // MongoDB ObjectId als String oder UUID

    @Indexed(unique = true)
    private String worldId; // Externe World ID

    @Indexed
    private String regionId; // Zugehörige Region ID

    /**
     * Public data containing the generated WorldInfo DTO.
     * This is what gets serialized and sent to clients.
     */
    private WorldInfo publicData; // eingebettete Struktur aus generated Modul

    private Instant createdAt;
    private Instant updatedAt;

    @Builder.Default
    private boolean enabled = true; // Standardmäßig aktiviert
    private String parent; // optionale Referenz auf übergeordnete Welt / Gruppe
    private String branch; // z.B. Entwicklungszweig / Variante

    // Zugriff / Berechtigungen
    @Builder.Default
    private List<String> owner = List.of();
    @Builder.Default
    private List<String> editor = List.of();
    @Builder.Default
    private List<String> player = List.of();

    @Builder.Default
    private boolean publicFlag = false; // ob Welt öffentlich zugänglich ist

    @Builder.Default
    private List<WEntryPoint> entryPoints = List.of();

    /**
     * Default ground level for chunk generation (Y coordinate).
     * Used when no chunk data exists in database.
     */
    @Builder.Default
    private int groundLevel = 0;

    /**
     * Water level for ocean generation (Y coordinate).
     * If set, water blocks are generated up to this level.
     */
    private Integer waterLevel;

    /**
     * Block type ID for ground blocks (e.g., "r/grass" for grass).
     * Used when generating default chunks.
     */
    @Builder.Default
    private String groundBlockType = "r/grass";

    /**
     * Block type ID for water blocks (e.g., "core:water").
     * Used when generating ocean in default chunks.
     */
    @Builder.Default
    private String waterBlockType = "r/ocean";

    public void touchForCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    public void touchForUpdate() {
        updatedAt = Instant.now();
    }

    /**
     * Calculate chunk X coordinate from world X coordinate.
     * Uses configured chunkSize from WorldInfo.
     *
     * @param worldX World X coordinate
     * @return Chunk X coordinate
     */
    public int getChunkX(int worldX) {
        if (publicData == null) {
            throw new IllegalStateException("World has no publicData");
        }
        int chunkSize = publicData.getChunkSize();
        return Math.floorDiv(worldX, chunkSize);
    }

    /**
     * Calculate chunk Z coordinate from world Z coordinate.
     * Uses configured chunkSize from WorldInfo.
     *
     * @param worldZ World Z coordinate
     * @return Chunk Z coordinate
     */
    public int getChunkZ(int worldZ) {
        if (publicData == null) {
            throw new IllegalStateException("World has no publicData");
        }
        int chunkSize = publicData.getChunkSize();
        return Math.floorDiv(worldZ, chunkSize);
    }

    /**
     * Calculate chunk key from world coordinates.
     * Format: "{cx}:{cz}"
     *
     * @param worldX World X coordinate
     * @param worldZ World Z coordinate
     * @return Chunk key string
     */
    public String getChunkKey(int worldX, int worldZ) {
        return getChunkX(worldX) + ":" + getChunkZ(worldZ);
    }
}
