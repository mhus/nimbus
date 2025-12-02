package de.mhus.nimbus.tools.demoimport.importers;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.mhus.nimbus.generated.types.Backdrop;
import de.mhus.nimbus.tools.demoimport.ImportStats;
import de.mhus.nimbus.world.shared.world.WBackdrop;
import de.mhus.nimbus.world.shared.world.WBackdropService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Imports Backdrop configurations from test_server files.
 * Reads from: {source-path}/backdrops/
 *
 * Expected files: fog1.json, fog2.json, hills.json, sky.json, stone.json, etc. (9 files)
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class BackdropImporter {

    private final WBackdropService service;
    private final ObjectMapper objectMapper;

    @Value("${import.source-path:../../client/packages/test_server/files}")
    private String sourcePath;

    public ImportStats importAll() throws Exception {
        log.info("Starting Backdrop import from: {}/backdrops/", sourcePath);

        ImportStats stats = new ImportStats();
        Path backdropsDir = Path.of(sourcePath, "backdrops");

        if (!Files.exists(backdropsDir)) {
            log.warn("Backdrops directory not found: {}", backdropsDir);
            return stats;
        }

        List<WBackdrop> entities = new ArrayList<>();
        File[] files = backdropsDir.toFile().listFiles((d, name) -> name.endsWith(".json"));

        if (files == null || files.length == 0) {
            log.warn("No Backdrop files found in: {}", backdropsDir);
            return stats;
        }

        log.info("Found {} Backdrop files", files.length);

        for (File file : files) {
            try {
                // Read and parse JSON
                Backdrop backdrop = objectMapper.readValue(file, Backdrop.class);

                // Use filename (without .json) as ID if not present in data
                String backdropId = backdrop.getId();
                if (backdropId == null || backdropId.isBlank()) {
                    backdropId = file.getName().replace(".json", "");
                }

                // Check if already exists
                if (service.findByBackdropId(backdropId).isPresent()) {
                    log.trace("Backdrop already exists: {} - skipping", backdropId);
                    stats.incrementSkipped();
                    continue;
                }

                // Create entity
                WBackdrop entity = WBackdrop.builder()
                        .backdropId(backdropId)
                        .publicData(backdrop)
                        .regionId(null)
                        .worldId(null)
                        .enabled(true)
                        .build();
                entity.touchCreate();

                entities.add(entity);
                stats.incrementSuccess();

                log.debug("Loaded Backdrop: {}", backdropId);

            } catch (Exception e) {
                log.error("Failed to import Backdrop from file: {}", file.getName(), e);
                stats.incrementFailure();
            }
        }

        // Batch save
        if (!entities.isEmpty()) {
            log.info("Saving {} Backdrops to database...", entities.size());
            service.saveAll(entities);
        }

        log.info("Backdrop import completed: {} imported, {} skipped, {} failed",
                stats.getSuccessCount(), stats.getSkippedCount(), stats.getFailureCount());

        return stats;
    }
}
