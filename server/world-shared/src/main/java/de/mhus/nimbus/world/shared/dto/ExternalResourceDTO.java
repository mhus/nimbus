package de.mhus.nimbus.world.shared.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

/**
 * Configuration for import/export definitions.
 * Stored in WAnything collection with collection="external-resources".
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExternalResourceDTO {

    /**
     * World identifier (as String).
     */
    private String worldId;

    /**
     * Filesystem path for export/import (e.g., "/data/exports/world1").
     */
    private String localPath;

    /**
     * Timestamp of last successful sync.
     */
    private Instant lastSync;

    /**
     * Status or error message from last sync operation.
     */
    private String lastSyncResult;

    /**
     * Types to sync: "asset", "backdrop", "blocktype", "model", "ground".
     * Empty list means export all types.
     */
    private List<String> types;

    /**
     * Enable automatic git pull/commit/push operations.
     */
    private boolean autoGit;
}
