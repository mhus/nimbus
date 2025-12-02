package de.mhus.nimbus.tools.demoimport.importers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import de.mhus.nimbus.generated.types.BlockType;
import de.mhus.nimbus.tools.demoimport.ImportStats;
import de.mhus.nimbus.world.shared.world.WBlockType;
import de.mhus.nimbus.world.shared.world.WBlockTypeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Imports BlockType definitions from test_server files.
 * Reads from: {source-path}/blocktypes/
 *
 * Structure:
 * - blocktypes/manifest.json (index)
 * - blocktypes/core/*.json (2 files)
 * - blocktypes/w/*.json (612 files)
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class BlockTypeImporter {

    private final WBlockTypeService service;
    private final ObjectMapper objectMapper;

    @Value("${import.source-path:../../client/packages/test_server/files}")
    private String sourcePath;

    public ImportStats importAll() throws Exception {
        log.info("Starting BlockType import from: {}/blocktypes/", sourcePath);

        ImportStats stats = new ImportStats();
        Path blockTypesDir = Path.of(sourcePath, "blocktypes");

        if (!Files.exists(blockTypesDir)) {
            log.warn("BlockTypes directory not found: {}", blockTypesDir);
            return stats;
        }

        // Import from subdirectories (core, w)
        List<WBlockType> entities = new ArrayList<>();

        // Import core blocktypes
        Path coreDir = blockTypesDir.resolve("core");
        if (Files.exists(coreDir)) {
            importFromDirectory(coreDir, "core", entities, stats);
        }

        // Import w blocktypes
        Path wDir = blockTypesDir.resolve("w");
        if (Files.exists(wDir)) {
            importFromDirectory(wDir, "w", entities, stats);
        }

        // Batch save with detailed error tracking
        if (!entities.isEmpty()) {
            log.info("Saving {} BlockTypes to database...", entities.size());

            // Try batch save first
            try {
                service.saveAll(entities);
            } catch (Exception e) {
                log.error("Batch save failed, trying individual saves to identify problem entities...", e);

                // Fall back to individual saves to identify the problematic entity
                for (WBlockType entity : entities) {
                    try {
                        service.save(
                            entity.getBlockId(),
                            entity.getPublicData(),
                            entity.getRegionId(),
                            entity.getWorldId()
                        );
                    } catch (Exception individualError) {
                        log.error("Failed to save BlockType: blockId={}, file might be: {}.json",
                                entity.getBlockId(), entity.getBlockId(), individualError);
                        log.error("PublicData content: {}",
                                objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(entity.getPublicData()));
                        stats.incrementFailure();
                        stats.decrementSuccess(); // Correct the counter
                    }
                }
            }
        }

        log.info("BlockType import completed: {} imported, {} skipped, {} failed",
                stats.getSuccessCount(), stats.getSkippedCount(), stats.getFailureCount());

        return stats;
    }

    private void importFromDirectory(Path dir, String prefix, List<WBlockType> entities, ImportStats stats) throws Exception {
        File[] files = dir.toFile().listFiles((d, name) -> name.endsWith(".json"));
        if (files == null) return;

        log.info("Importing from {}: {} files", prefix, files.length);

        for (File file : files) {
            try {
                // Read and parse JSON
                BlockType blockType = objectMapper.readValue(file, BlockType.class);

                if (blockType.getId() == null || blockType.getId().isBlank()) {
                    log.warn("Skipping file with missing ID: {}", file.getName());
                    stats.incrementSkipped();
                    continue;
                }

                // Check if already exists (handle duplicates gracefully)
                try {
                    if (service.findByBlockId(blockType.getId()).isPresent()) {
                        log.trace("BlockType already exists: {} - skipping", blockType.getId());
                        stats.incrementSkipped();
                        continue;
                    }
                } catch (Exception e) {
                    // If query fails due to duplicates, skip this entry
                    log.warn("Duplicate blockId detected in DB: {} - skipping import", blockType.getId());
                    stats.incrementSkipped();
                    continue;
                }

                // Create entity with blockTypeGroup
                WBlockType entity = WBlockType.builder()
                        .blockId(blockType.getId())
                        .blockTypeGroup(prefix)  // Store group separately (e.g., "w", "core")
                        .publicData(blockType)
                        .regionId(null)
                        .worldId(null)
                        .enabled(true)
                        .build();
                entity.touchCreate();

                entities.add(entity);
                stats.incrementSuccess();

                if (stats.getSuccessCount() % 100 == 0) {
                    log.debug("Progress: {} BlockTypes loaded", stats.getSuccessCount());
                }

            } catch (Exception e) {
                log.error("Failed to parse BlockType from file: {} - Error: {}",
                        file.getAbsolutePath(), e.getMessage(), e);
                stats.incrementFailure();
            }
        }
    }
}
