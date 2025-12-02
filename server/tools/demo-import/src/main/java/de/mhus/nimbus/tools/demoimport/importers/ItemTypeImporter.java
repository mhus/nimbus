package de.mhus.nimbus.tools.demoimport.importers;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.mhus.nimbus.generated.types.ItemType;
import de.mhus.nimbus.tools.demoimport.ImportStats;
import de.mhus.nimbus.world.shared.world.WItemType;
import de.mhus.nimbus.world.shared.world.WItemTypeService;
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
 * Imports ItemType definitions from test_server files.
 * Reads from: {source-path}/itemtypes/
 *
 * Expected files: sword.json, axe.json, pickaxe.json, wand.json, potion.json
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ItemTypeImporter {

    private final WItemTypeService service;
    private final ObjectMapper objectMapper;

    @Value("${import.source-path:../../client/packages/test_server/files}")
    private String sourcePath;

    public ImportStats importAll() throws Exception {
        log.info("Starting ItemType import from: {}/itemtypes/", sourcePath);

        ImportStats stats = new ImportStats();
        Path itemTypesDir = Path.of(sourcePath, "itemtypes");

        if (!Files.exists(itemTypesDir)) {
            log.warn("ItemTypes directory not found: {}", itemTypesDir);
            return stats;
        }

        List<WItemType> entities = new ArrayList<>();
        File[] files = itemTypesDir.toFile().listFiles((d, name) -> name.endsWith(".json"));

        if (files == null || files.length == 0) {
            log.warn("No ItemType files found in: {}", itemTypesDir);
            return stats;
        }

        log.info("Found {} ItemType files", files.length);

        for (File file : files) {
            try {
                // Read and parse JSON
                ItemType itemType = objectMapper.readValue(file, ItemType.class);

                if (itemType.getType() == null || itemType.getType().isBlank()) {
                    log.warn("Skipping file with missing type: {}", file.getName());
                    stats.incrementSkipped();
                    continue;
                }

                // Create entity
                WItemType entity = WItemType.builder()
                        .itemType(itemType.getType())
                        .publicData(itemType)
                        .regionId(null)
                        .worldId(null)
                        .enabled(true)
                        .build();
                entity.touchCreate();

                entities.add(entity);
                stats.incrementSuccess();

                log.debug("Loaded ItemType: {}", itemType.getType());

            } catch (Exception e) {
                log.error("Failed to import ItemType from file: {}", file.getName(), e);
                stats.incrementFailure();
            }
        }

        // Batch save
        if (!entities.isEmpty()) {
            log.info("Saving {} ItemTypes to database...", entities.size());
            service.saveAll(entities);
        }

        log.info("ItemType import completed: {} imported, {} skipped, {} failed",
                stats.getSuccessCount(), stats.getSkippedCount(), stats.getFailureCount());

        return stats;
    }
}
