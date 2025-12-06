package de.mhus.nimbus.tools.worldexport;

import de.mhus.nimbus.shared.service.ExportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Master Exporter - Orchestrates export of all configured collections.
 *
 * <p>Exports collections specified in application.yaml to JSON Lines files
 * in the configured output directory.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MasterExporter {

    private final ExportService exportService;

    @Value("${export.output-path:./export}")
    private String outputPath;

    @Value("${export.world-id:*}")
    private String worldId;

    @Value("${export.collections:}")
    private List<String> collectionsToExport;

    /**
     * Exports all configured collections.
     *
     * @return ExportStats with overall statistics
     * @throws Exception if export fails
     */
    public ExportStats exportAll() throws Exception {
        log.info("=".repeat(70));
        log.info("MASTER EXPORTER - Starting export of all configured collections");
        log.info("Output path: {}", outputPath);
        log.info("World ID filter: {}", worldId);
        log.info("Collections to export: {}", collectionsToExport);
        log.info("=".repeat(70));
        log.info("");

        long startTime = System.currentTimeMillis();
        ExportStats stats = new ExportStats();

        // Use configured collections or warn if none configured
        if (collectionsToExport.isEmpty()) {
            log.warn("No collections configured for export in application.yaml");
            return stats;
        }

        stats.setTotalCollections(collectionsToExport.size());

        // Check if output should be a ZIP file
        boolean isZipFile = outputPath.toLowerCase().endsWith(".zip");
        Path workDir = null;

        try {
            if (isZipFile) {
                log.info("Detected ZIP output, exporting to temporary directory...");
                workDir = Files.createTempDirectory("nimbus-export-");
                log.info("Temporary directory: {}", workDir);
            } else {
                workDir = Path.of(outputPath);
            }

            // Export all collections
            for (String collectionName : collectionsToExport) {
                try {
                    log.info(">>> Exporting collection: {}", collectionName);

                    Path outputFile = workDir.resolve(collectionName + ".jsonl");

                    ExportService.ExportResult result = exportService.exportCollection(
                            collectionName,
                            outputFile,
                            worldId
                    );

                    if (result.isSuccess() || !result.hasErrors()) {
                        stats.incrementSuccess(result.getSuccessCount());
                        log.info("    {} entities exported to {}", result.getSuccessCount(), outputFile);
                    } else {
                        stats.incrementFailure(collectionName);
                        log.error("    Export failed with {} errors", result.getErrorCount());
                    }

                    log.info("");

                } catch (Exception e) {
                    log.error("Failed to export collection: {}", collectionName, e);
                    stats.incrementFailure(collectionName);
                    log.info("");
                }
            }

            // Create ZIP file if needed
            if (isZipFile) {
                log.info("Creating ZIP archive: {}", outputPath);
                createZipFile(workDir, Path.of(outputPath));
                log.info("ZIP archive created successfully");
            }

            long duration = System.currentTimeMillis() - startTime;
            stats.setDurationMs(duration);

            return stats;

        } finally {
            // Clean up temporary directory if ZIP was created
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
     * Creates a ZIP file from all files in a directory.
     *
     * @param sourceDir the source directory
     * @param zipFile   the target ZIP file path
     * @throws IOException if ZIP creation fails
     */
    private void createZipFile(Path sourceDir, Path zipFile) throws IOException {
        // Ensure parent directory exists
        Files.createDirectories(zipFile.getParent());

        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile.toFile()))) {
            Files.walk(sourceDir)
                    .filter(path -> !Files.isDirectory(path))
                    .forEach(path -> {
                        try {
                            String entryName = sourceDir.relativize(path).toString();
                            ZipEntry zipEntry = new ZipEntry(entryName);
                            zos.putNextEntry(zipEntry);
                            Files.copy(path, zos);
                            zos.closeEntry();
                        } catch (IOException e) {
                            throw new RuntimeException("Failed to add file to ZIP: " + path, e);
                        }
                    });
        }
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
}
