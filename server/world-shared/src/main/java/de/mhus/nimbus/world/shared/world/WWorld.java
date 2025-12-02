package de.mhus.nimbus.world.shared.world;

import de.mhus.nimbus.generated.types.WorldInfo;
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
public class WWorld {

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
     * Block type ID for ground blocks (e.g., "w:310" for grass).
     * Used when generating default chunks.
     */
    @Builder.Default
    private String groundBlockType = "w:310";

    /**
     * Block type ID for water blocks (e.g., "core:water").
     * Used when generating ocean in default chunks.
     */
    @Builder.Default
    private String waterBlockType = "core:water";

    public void touchForCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    public void touchForUpdate() {
        updatedAt = Instant.now();
    }
}
