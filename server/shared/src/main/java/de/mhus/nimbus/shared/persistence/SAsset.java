package de.mhus.nimbus.shared.persistence;

import lombok.Data;
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
 */
@Document(collection = "s_assets")
@CompoundIndexes({
        @CompoundIndex(name = "region_world_path_idx", def = "{ 'regionId': 1, 'worldId': 1, 'path': 1 }")
})
@Data
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

    /** Inline Content (wenn size <= inlineMaxSize). */
    private byte[] content;

    @CreatedDate
    private Instant createdAt;

    private String createdBy;

    private boolean enabled = true;

    /** Pflicht: Region Identifier. */
    @Indexed
    private String regionId;

    /** Optional: Welt Identifier. */
    @Indexed
    private String worldId; // kann null sein

    public boolean isInline() { return content != null && storageId == null; }
    public boolean isStoredExternal() { return storageId != null; }
}

