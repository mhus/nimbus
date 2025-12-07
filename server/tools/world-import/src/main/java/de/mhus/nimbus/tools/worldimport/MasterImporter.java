package de.mhus.nimbus.tools.worldimport;

import de.mhus.nimbus.shared.service.ImportMode;
import de.mhus.nimbus.shared.service.ImportService;
import de.mhus.nimbus.shared.service.SchemaMigrationService;
import de.mhus.nimbus.shared.storage.StorageData;
import de.mhus.nimbus.shared.storage.StorageDataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

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
    private final SchemaMigrationService schemaMigrationService;
    private final StorageDataRepository storageDataRepository;

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

        Path inputPathObj = Path.of(inputPath);

        // Check if input is a ZIP file
        boolean isZipFile = inputPath.toLowerCase().endsWith(".zip");
        Path workDir = null;

        try {
            if (isZipFile) {
                if (!Files.exists(inputPathObj)) {
                    throw new IllegalArgumentException("ZIP file does not exist: " + inputPathObj);
                }
                log.info("Detected ZIP file, extracting to temporary directory...");
                workDir = extractZipFile(inputPathObj);
                log.info("Extracted ZIP to: {}", workDir);
            } else {
                if (!Files.exists(inputPathObj)) {
                    throw new IllegalArgumentException("Input directory does not exist: " + inputPathObj);
                }
                workDir = inputPathObj;
            }

            if (collectionsToImport.isEmpty()) {
                log.warn("No collections configured for import in application.yaml");
                return stats;
            }

            stats.setTotalCollections(collectionsToImport.size());

            for (String collectionName : collectionsToImport) {
                try {
                    Path inputFile = workDir.resolve(collectionName + ".jsonl");

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

                        // After importing storage_data, migrate all storage objects
                        if ("storage_data".equals(collectionName)) {
                            migrateImportedStorage(result.getSuccessCount());
                        }
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

        } finally {
            // Clean up temporary directory if ZIP was extracted
            if (isZipFile && workDir != null) {
                try {
                    deleteDirectory(workDir);
                    log.info("Cleaned up temporary directory: {}", workDir);
                } catch (IOException e) {
                    log.warn("Failed to clean up temporary directory: {}", workDir, e);
                }
            }
        }
    }

    /**
     * Extracts a ZIP file to a temporary directory.
     *
     * @param zipFile the ZIP file path
     * @return the temporary directory path
     * @throws IOException if extraction fails
     */
    private Path extractZipFile(Path zipFile) throws IOException {
        Path tempDir = Files.createTempDirectory("nimbus-import-");

        try (ZipFile zip = new ZipFile(zipFile.toFile())) {
            zip.stream().forEach(entry -> {
                try {
                    Path targetPath = tempDir.resolve(entry.getName());

                    if (entry.isDirectory()) {
                        Files.createDirectories(targetPath);
                    } else {
                        Files.createDirectories(targetPath.getParent());
                        try (InputStream in = zip.getInputStream(entry)) {
                            Files.copy(in, targetPath, StandardCopyOption.REPLACE_EXISTING);
                        }
                    }
                } catch (IOException e) {
                    throw new RuntimeException("Failed to extract: " + entry.getName(), e);
                }
            });
        }

        return tempDir;
    }

    /**
     * Recursively deletes a directory.
     *
     * @param directory the directory to delete
     * @throws IOException if deletion fails
     */
    private void deleteDirectory(Path directory) throws IOException {
        if (Files.exists(directory)) {
            Files.walk(directory)
                    .sorted((a, b) -> b.compareTo(a))
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                        } catch (IOException e) {
                            log.warn("Failed to delete: {}", path, e);
                        }
                    });
        }
    }

    /**
     * Migrates all imported storage objects to the latest schema version.
     * Called after storage_data collection import.
     *
     * @param importedCount number of imported storage_data records
     */
    private void migrateImportedStorage(int importedCount) {
        if (importedCount == 0) {
            log.info("No storage objects imported, skipping storage migration");
            return;
        }

        log.info("");
        log.info("=".repeat(70));
        log.info("POST-IMPORT: Migrating storage objects to latest schema version");
        log.info("=".repeat(70));

        // Get all unique storage IDs from imported storage_data
        List<StorageData> storageDataList = storageDataRepository.findAll(); // TODO use Stream or chunks, could be a huge amount of data

        if (storageDataList.isEmpty()) {
            log.info("No storage data found for migration");
            return;
        }

        // Get unique storage IDs (UUIDs)
        List<String> storageIds = storageDataList.stream()
                .filter(StorageData::isFinal)
                .map(StorageData::getUuid)
                .distinct()
                .toList();

        log.info("Found {} unique storage objects to migrate", storageIds.size());

        int successCount = 0;
        int failureCount = 0;
        int skippedCount = 0;

        for (String storageId : storageIds) {
            try {
                SchemaMigrationService.MigrationResult result = schemaMigrationService.migrateStorage(storageId);

                if (result.migrated()) {
                    successCount++;
                    log.debug("Migrated storage: {} ({} {} -> {})",
                            storageId, result.schema(), result.fromVersion(), result.toVersion());
                } else {
                    skippedCount++;
                    log.trace("Skipped storage {}: {}", storageId, result.message());
                }

            } catch (SchemaMigrationService.MigrationException e) {
                failureCount++;
                log.error("Failed to migrate storage {}: {}", storageId, e.getMessage());
            }
        }

        log.info("");
        log.info("Storage migration completed:");
        log.info("  Total storage objects: {}", storageIds.size());
        log.info("  Migrated:              {}", successCount);
        log.info("  Skipped (up-to-date):  {}", skippedCount);
        log.info("  Failed:                {}", failureCount);
        log.info("=".repeat(70));
        log.info("");
    }
}
