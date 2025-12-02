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

    public void touchForCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    public void touchForUpdate() {
        updatedAt = Instant.now();
    }
}
