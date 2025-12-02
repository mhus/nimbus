package de.mhus.nimbus.tools.demoimport.importers;

import com.fasterxml.jackson.databind.JsonNode;
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
 * Imports Entity instances from test_server world data.
 * Reads from: {data-path}/worlds/main/entities/*.json
 *
 * These are actual entity instances placed in the world (cows, NPCs, etc.).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class WorldEntityImporter {

    private final WEntityService service;
    private final ObjectMapper objectMapper;

    @Value("${import.data-path:../../client/packages/test_server/data}")
    private String dataPath;

    @Value("${import.default-world-id:main}")
    private String defaultWorldId;

    public ImportStats importAll() throws Exception {
        log.info("Starting WorldEntity import from: {}/worlds/main/entities/", dataPath);

        ImportStats stats = new ImportStats();
        Path entitiesDir = Path.of(dataPath, "worlds", "main", "entities");

        if (!Files.exists(entitiesDir)) {
            log.warn("Entities directory not found: {}", entitiesDir);
            return stats;
        }

        List<WEntity> entities = new ArrayList<>();
        File[] files = entitiesDir.toFile().listFiles((d, name) -> name.endsWith(".json") && !name.equals("NOTIFICATION_EXAMPLES.md"));

        if (files == null || files.length == 0) {
            log.warn("No entity files found in: {}", entitiesDir);
            return stats;
        }

        log.info("Found {} entity files", files.length);

        for (File file : files) {
            try {
                // Read JSON as tree to extract custom fields
                JsonNode json = objectMapper.readTree(file);

                String entityId = json.has("entityId") ? json.get("entityId").asText() : null;
                String modelId = json.has("entityModelId") ? json.get("entityModelId").asText() : null;

                if (entityId == null || entityId.isBlank()) {
                    log.warn("Skipping file with missing entityId: {}", file.getName());
                    stats.incrementSkipped();
                    continue;
                }

                // Check if already exists
                if (service.findByWorldIdAndEntityId(defaultWorldId, entityId).isPresent()) {
                    log.trace("WorldEntity already exists: {} - skipping", entityId);
                    stats.incrementSkipped();
                    continue;
                }

                // Create Entity DTO from test_server data
                Entity entityData = new Entity();
                entityData.setId(entityId);
                entityData.setName(json.has("name") ? json.get("name").asText() : entityId);
                entityData.setModel(modelId);

                // Set properties from JSON
                if (json.has("clientPhysics")) entityData.setClientPhysics(json.get("clientPhysics").asBoolean());
                if (json.has("interactive")) entityData.setInteractive(json.get("interactive").asBoolean());
                if (json.has("physics")) entityData.setPhysics(json.get("physics").asBoolean());
                if (json.has("solid")) entityData.setSolid(json.get("solid").asBoolean());

                // Determine chunk from initialPosition
                String chunk = null;
                if (json.has("initialPosition")) {
                    JsonNode pos = json.get("initialPosition");
                    int x = pos.has("x") ? pos.get("x").asInt() : 0;
                    int z = pos.has("z") ? pos.get("z").asInt() : 0;
                    int cx = x >> 4;  // Divide by 16
                    int cz = z >> 4;
                    chunk = cx + ":" + cz;
                }

                // Create WEntity
                WEntity entity = WEntity.builder()
                        .worldId(defaultWorldId)
                        .entityId(entityId)
                        .publicData(entityData)
                        .chunk(chunk)
                        .modelId(modelId)
                        .enabled(true)
                        .build();
                entity.touchCreate();

                entities.add(entity);
                stats.incrementSuccess();

                log.debug("Loaded WorldEntity: {} (model: {}, chunk: {})", entityId, modelId, chunk);

            } catch (Exception e) {
                log.error("Failed to import WorldEntity from file: {}", file.getName(), e);
                stats.incrementFailure();
            }
        }

        // Batch save
        if (!entities.isEmpty()) {
            log.info("Saving {} WorldEntities to database...", entities.size());
            service.saveAll(entities);
        }

        log.info("WorldEntity import completed: {} imported, {} skipped, {} failed",
                stats.getSuccessCount(), stats.getSkippedCount(), stats.getFailureCount());

        return stats;
    }
}
