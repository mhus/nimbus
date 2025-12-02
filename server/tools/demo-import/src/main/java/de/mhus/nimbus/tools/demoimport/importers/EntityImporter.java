package de.mhus.nimbus.tools.demoimport.importers;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.mhus.nimbus.generated.types.Entity;
import de.mhus.nimbus.tools.demoimport.ImportStats;
import de.mhus.nimbus.world.shared.world.WEntity;
import de.mhus.nimbus.world.shared.world.WEntityService;
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
 * Imports Entity instances from test_server files.
 * Reads from: {source-path}/entity/ (or entities/)
 *
 * Note: test_server typically has player entity template.
 * This imports entity definitions (not runtime instances).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class EntityImporter {

    private final WEntityService service;
    private final ObjectMapper objectMapper;

    @Value("${import.source-path:../../client/packages/test_server/files}")
    private String sourcePath;

    @Value("${import.default-world-id:main}")
    private String defaultWorldId;

    public ImportStats importAll() throws Exception {
        log.info("Starting Entity import from: {}/entity/ (or entities/)", sourcePath);

        ImportStats stats = new ImportStats();

        // Try both "entity" and "entities" directories
        Path entityDir = Path.of(sourcePath, "entity");
        if (!Files.exists(entityDir)) {
            entityDir = Path.of(sourcePath, "entities");
        }

        if (!Files.exists(entityDir)) {
            log.warn("Entity directory not found: {}/entity/ or {}/entities/", sourcePath, sourcePath);
            return stats;
        }

        List<WEntity> entities = new ArrayList<>();
        File[] files = entityDir.toFile().listFiles((d, name) -> name.endsWith(".json"));

        if (files == null || files.length == 0) {
            log.warn("No Entity files found in: {}", entityDir);
            return stats;
        }

        log.info("Found {} Entity files", files.length);

        for (File file : files) {
            try {
                // Read and parse JSON
                Entity entityData = objectMapper.readValue(file, Entity.class);

                if (entityData.getId() == null || entityData.getId().isBlank()) {
                    log.warn("Skipping file with missing id: {}", file.getName());
                    stats.incrementSkipped();
                    continue;
                }

                // Extract modelId from entity data
                String modelId = entityData.getModel();

                // Create entity instance
                // Note: These are templates, not actual world instances
                // For world instances, chunk and position would be set
                WEntity entity = WEntity.builder()
                        .worldId(defaultWorldId)
                        .entityId(entityData.getId())
                        .publicData(entityData)
                        .chunk(null)  // No chunk for templates
                        .modelId(modelId)
                        .enabled(true)
                        .build();
                entity.touchCreate();

                entities.add(entity);
                stats.incrementSuccess();

                log.debug("Loaded Entity: {} (model: {})", entityData.getId(), modelId);

            } catch (Exception e) {
                log.error("Failed to import Entity from file: {}", file.getName(), e);
                stats.incrementFailure();
            }
        }

        // Batch save
        if (!entities.isEmpty()) {
            log.info("Saving {} Entities to database...", entities.size());
            service.saveAll(entities);
        }

        log.info("Entity import completed: {} imported, {} skipped, {} failed",
                stats.getSuccessCount(), stats.getSkippedCount(), stats.getFailureCount());

        return stats;
    }
}
