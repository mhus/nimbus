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

                // Create Entity DTO from test_server data (client-facing)
                Entity entityData = new Entity();
                entityData.setId(entityId);
                entityData.setName(json.has("name") ? json.get("name").asText() : entityId);
                entityData.setModel(modelId);

                // Set properties from JSON
                if (json.has("clientPhysics")) entityData.setClientPhysics(json.get("clientPhysics").asBoolean());
                if (json.has("interactive")) entityData.setInteractive(json.get("interactive").asBoolean());
                if (json.has("physics")) entityData.setPhysics(json.get("physics").asBoolean());
                if (json.has("solid")) entityData.setSolid(json.get("solid").asBoolean());

                // Parse simulation data (server-only, from ServerEntitySpawnDefinition)
                de.mhus.nimbus.generated.types.Vector3 initialPosition = null;
                String chunk = null;
                if (json.has("initialPosition")) {
                    JsonNode pos = json.get("initialPosition");
                    double x = pos.has("x") ? pos.get("x").asDouble() : 0;
                    double y = pos.has("y") ? pos.get("y").asDouble() : 64;
                    double z = pos.has("z") ? pos.get("z").asDouble() : 0;

                    initialPosition = new de.mhus.nimbus.generated.types.Vector3();
                    initialPosition.setX(x);
                    initialPosition.setY(y);
                    initialPosition.setZ(z);

                    int cx = (int) Math.floor(x / 16);
                    int cz = (int) Math.floor(z / 16);
                    chunk = cx + ":" + cz;
                }

                de.mhus.nimbus.generated.types.Rotation initialRotation = null;
                if (json.has("initialRotation")) {
                    JsonNode rot = json.get("initialRotation");
                    initialRotation = new de.mhus.nimbus.generated.types.Rotation();
                    initialRotation.setY(rot.has("y") ? rot.get("y").asDouble() : 0);
                    initialRotation.setP(rot.has("p") ? rot.get("p").asDouble() : 0);
                }

                de.mhus.nimbus.generated.types.Vector3 middlePoint = null;
                if (json.has("middlePoint")) {
                    JsonNode mp = json.get("middlePoint");
                    middlePoint = new de.mhus.nimbus.generated.types.Vector3();
                    middlePoint.setX(mp.has("x") ? mp.get("x").asDouble() : 0);
                    middlePoint.setY(mp.has("y") ? mp.get("y").asDouble() : 64);
                    middlePoint.setZ(mp.has("z") ? mp.get("z").asDouble() : 0);
                }

                Double radius = json.has("radius") ? json.get("radius").asDouble() : null;
                Double speed = json.has("speed") ? json.get("speed").asDouble() : 1.0;
                String behaviorModel = json.has("behaviorModel") ? json.get("behaviorModel").asText() : "PreyAnimalBehavior";

                // Parse behaviorConfig if present
                java.util.Map<String, Object> behaviorConfig = null;
                if (json.has("behaviorConfig")) {
                    behaviorConfig = objectMapper.convertValue(json.get("behaviorConfig"), java.util.Map.class);
                }

                // Create WEntity with simulation data
                WEntity entity = WEntity.builder()
                        .worldId(defaultWorldId)
                        .entityId(entityId)
                        .publicData(entityData)
                        .chunk(chunk)
                        .modelId(modelId)
                        .position(initialPosition)
                        .rotation(initialRotation)
                        .middlePoint(middlePoint)
                        .radius(radius)
                        .speed(speed)
                        .behaviorModel(behaviorModel)
                        .behaviorConfig(behaviorConfig)
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
