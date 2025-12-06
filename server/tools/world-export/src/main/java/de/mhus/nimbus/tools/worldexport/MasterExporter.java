package de.mhus.nimbus.tools.worldexport;

import de.mhus.nimbus.shared.service.ExportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

        Path outputDir = Path.of(outputPath);

        // Use configured collections or warn if none configured
        if (collectionsToExport.isEmpty()) {
            log.warn("No collections configured for export in application.yaml");
            return stats;
        }

        stats.setTotalCollections(collectionsToExport.size());

        for (String collectionName : collectionsToExport) {
            try {
                log.info(">>> Exporting collection: {}", collectionName);

                Path outputFile = outputDir.resolve(collectionName + ".jsonl");

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

        long duration = System.currentTimeMillis() - startTime;
        stats.setDurationMs(duration);

        return stats;
    }
}
