package de.mhus.nimbus.tools.worldimport;

import de.mhus.nimbus.shared.service.ImportMode;
import de.mhus.nimbus.shared.service.ImportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Master Importer - Orchestrates import of all configured collections with schema migration.
 *
 * <p>Imports collections in the order specified in application.yaml to ensure
 * referential integrity (e.g., worlds before chunks, templates before instances).</p>
 *
 * <p>Each entity is automatically migrated to the target schema version before saving.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MasterImporter {

    private final ImportService importService;

    @Value("${import.input-path:./export}")
    private String inputPath;

    @Value("${import.world-id:*}")
    private String worldId;

    @Value("${import.mode:skip}")
    private String importModeString;

    @Value("${import.collections:}")
    private List<String> collectionsToImport;

    /**
     * Imports all configured collections with schema migration.
     *
     * @return ImportStats with overall statistics
     * @throws Exception if import fails
     */
    public ImportStats importAll() throws Exception {
        ImportMode importMode = ImportMode.fromString(importModeString);

        log.info("=".repeat(70));
        log.info("MASTER IMPORTER - Starting import of all configured collections");
        log.info("Input path: {}", inputPath);
        log.info("World ID filter: {}", worldId);
        log.info("Import mode: {}", importMode);
        log.info("Collections to import: {}", collectionsToImport);
        log.info("=".repeat(70));
        log.info("");

        long startTime = System.currentTimeMillis();
        ImportStats stats = new ImportStats();

        Path inputDir = Path.of(inputPath);

        if (!Files.exists(inputDir)) {
            throw new IllegalArgumentException("Input directory does not exist: " + inputDir);
        }

        if (collectionsToImport.isEmpty()) {
            log.warn("No collections configured for import in application.yaml");
            return stats;
        }

        stats.setTotalCollections(collectionsToImport.size());

        for (String collectionName : collectionsToImport) {
            try {
                Path inputFile = inputDir.resolve(collectionName + ".jsonl");

                if (!Files.exists(inputFile)) {
                    log.warn("Input file not found, skipping collection: {}", inputFile);
                    continue;
                }

                log.info(">>> Importing collection: {}", collectionName);
                log.info("    Entity type and version will be detected from _class field");

                ImportService.ImportResult result = importService.importCollection(
                        collectionName,
                        inputFile,
                        worldId,
                        importMode
                );

                if (result.isSuccess() || !result.hasErrors()) {
                    stats.incrementSuccess(result.getSuccessCount(), result.getMigrationCount());
                    log.info("    {} entities imported, {} migrated, {} skipped (existing)",
                            result.getSuccessCount(), result.getMigrationCount(), result.getSkippedExistingCount());
                } else {
                    stats.incrementFailure(collectionName);
                    log.error("    Import failed with {} errors", result.getErrorCount());
                }

                log.info("");

            } catch (Exception e) {
                log.error("Failed to import collection: {}", collectionName, e);
                stats.incrementFailure(collectionName);
                log.info("");
            }
        }

        long duration = System.currentTimeMillis() - startTime;
        stats.setDurationMs(duration);

        return stats;
    }
}
