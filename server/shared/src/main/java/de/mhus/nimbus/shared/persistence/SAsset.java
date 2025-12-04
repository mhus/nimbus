package de.mhus.nimbus.shared.persistence;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * MongoDB Asset Entity. Speichert kleine Binärdaten direkt (content) bis zur konfigurierten Grenze.
 * Größere Inhalte werden über einen externen StorageService ausgelagert und via storageId referenziert.
 * Metadaten aus *.info Dateien werden in publicData gespeichert.
 */
@Document(collection = "s_assets")
@CompoundIndexes({
        @CompoundIndex(name = "region_world_path_idx", def = "{ 'regionId': 1, 'worldId': 1, 'path': 1 }", unique = true)
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SAsset {

    @Id
    private String id;

    /** Voller Pfad inkl. Dateiname (Unique innerhalb Region/Welt-Kombination). */
    @Indexed
    private String path;

    /** Nur Dateiname extrahiert aus path. */
    @Indexed
    private String name;

    /** Gesamtgröße des Inhalts (direkt oder extern) in Bytes. */
    private long size;

    /** Falls ausgelagert im StorageService. */
    @Indexed
    private String storageId;

    /**
     * Public metadata from *.info files.
     * Contains description, dimensions, color, mimeType, etc.
     */
    private AssetMetadata publicData;

    @CreatedDate
    private Instant createdAt;

    private String createdBy;

    @Builder.Default
    private boolean enabled = true;

    /** Pflicht: Region Identifier. */
    @Indexed
    private String regionId;

    /** Optional: Welt Identifier. */
    @Indexed
    private String worldId; // kann null sein

}

