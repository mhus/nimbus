package de.mhus.nimbus.tools.demoimport.importers;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.mhus.nimbus.generated.types.EntityModel;
import de.mhus.nimbus.tools.demoimport.ImportStats;
import de.mhus.nimbus.world.shared.world.WEntityModel;
import de.mhus.nimbus.world.shared.world.WEntityModelService;
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
 * Imports EntityModel definitions from test_server files.
 * Reads from: {source-path}/entitymodels/
 *
 * Expected files: cow1.json, pig1.json, wizard1.json, farmer1.json
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class EntityModelImporter {

    private final WEntityModelService service;
    private final ObjectMapper objectMapper;

    @Value("${import.source-path:../../client/packages/test_server/files}")
    private String sourcePath;

    @Value("${import.default-world-id:main}")
    private String defaultWorldId;

    public ImportStats importAll() throws Exception {
        log.info("Starting EntityModel import from: {}/entitymodels/", sourcePath);

        ImportStats stats = new ImportStats();
        Path entityModelsDir = Path.of(sourcePath, "entitymodels");

        if (!Files.exists(entityModelsDir)) {
            log.warn("EntityModels directory not found: {}", entityModelsDir);
            return stats;
        }

        List<WEntityModel> entities = new ArrayList<>();
        File[] files = entityModelsDir.toFile().listFiles((d, name) -> name.endsWith(".json"));

        if (files == null || files.length == 0) {
            log.warn("No EntityModel files found in: {}", entityModelsDir);
            return stats;
        }

        log.info("Found {} EntityModel files", files.length);

        for (File file : files) {
            try {
                // Read and parse JSON
                EntityModel entityModel = objectMapper.readValue(file, EntityModel.class);

                if (entityModel.getId() == null || entityModel.getId().isBlank()) {
                    log.warn("Skipping file with missing id: {}", file.getName());
                    stats.incrementSkipped();
                    continue;
                }

                // Check if already exists
                if (service.findByModelId(entityModel.getId()).isPresent()) {
                    log.trace("EntityModel already exists: {} - skipping", entityModel.getId());
                    stats.incrementSkipped();
                    continue;
                }

                // Create entity
                WEntityModel entity = WEntityModel.builder()
                        .modelId(entityModel.getId())
                        .publicData(entityModel)
                        .worldId(defaultWorldId)  // Set worldId from config (e.g., "main")
                        .enabled(true)
                        .build();
                entity.touchCreate();

                entities.add(entity);
                stats.incrementSuccess();

                log.debug("Loaded EntityModel: {}", entityModel.getId());

            } catch (Exception e) {
                log.error("Failed to import EntityModel from file: {}", file.getName(), e);
                stats.incrementFailure();
            }
        }

        // Batch save (only new entities)
        if (!entities.isEmpty()) {
            log.info("Saving {} new EntityModels to database...", entities.size());
            service.saveAll(entities);
        }

        log.info("EntityModel import completed: {} imported, {} skipped, {} failed",
                stats.getSuccessCount(), stats.getSkippedCount(), stats.getFailureCount());

        return stats;
    }
}
