package de.mhus.nimbus.tools.demoimport.importers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.mhus.nimbus.generated.types.ItemBlockRef;
import de.mhus.nimbus.tools.demoimport.ImportStats;
import de.mhus.nimbus.world.shared.world.WItemRegistryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Imports Item positions from test_server data.
 * Reads from: {data-path}/worlds/main/items.json
 *
 * Creates WItemPosition entities with ItemBlockRef publicData.
 * Each item is stored with its chunk key for efficient spatial queries.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ItemImporter {

    private final WItemRegistryService itemRegistryService;
    private final ObjectMapper objectMapper;

    @Value("${import.data-path:../../client/packages/test_server/data}")
    private String dataPath;

    @Value("${import.world-id:main}")
    private String worldId;

    /**
     * Import all items from items.json file.
     *
     * @return Import statistics
     * @throws Exception if import fails
     */
    public ImportStats importAll() throws Exception {
        log.info("Starting Item import from: {}/worlds/{}/items.json", dataPath, worldId);

        ImportStats stats = new ImportStats();
        Path itemsFilePath = Path.of(dataPath, "worlds", worldId, "items.json");

        if (!Files.exists(itemsFilePath)) {
            log.warn("Items file not found: {} - no items to import", itemsFilePath);
            return stats;
        }

        File itemsFile = itemsFilePath.toFile();

        // Read items.json as JSON array
        JsonNode rootNode = objectMapper.readTree(itemsFile);

        if (!rootNode.isArray()) {
            log.error("Items file is not an array: {}", itemsFilePath);
            stats.incrementFailure();
            return stats;
        }

        // Iterate over ServerItem array
        for (JsonNode serverItemNode : rootNode) {
            try {
                importServerItem(serverItemNode, stats);
            } catch (Exception e) {
                log.error("Failed to import item", e);
                stats.incrementFailure();
            }
        }

        log.info("Item import completed: {} imported, {} skipped, {} failed",
                stats.getSuccessCount(), stats.getSkippedCount(), stats.getFailureCount());

        return stats;
    }

    /**
     * Import a single ServerItem.
     * Only imports items that have an itemBlockRef (position in world).
     *
     * @param serverItemNode JSON node containing ServerItem data
     * @param stats Import statistics
     */
    private void importServerItem(JsonNode serverItemNode, ImportStats stats) throws Exception {
        // Check if this item has an itemBlockRef (position in world)
        if (!serverItemNode.has("itemBlockRef")) {
            log.trace("Item has no itemBlockRef - skipping (inventory item)");
            stats.incrementSkipped();
            return;
        }

        // Extract itemBlockRef
        JsonNode itemBlockRefNode = serverItemNode.get("itemBlockRef");
        ItemBlockRef itemBlockRef = objectMapper.treeToValue(itemBlockRefNode, ItemBlockRef.class);

        // Validate required fields
        if (itemBlockRef.getId() == null || itemBlockRef.getId().isBlank()) {
            log.warn("Item has no ID - skipping");
            stats.incrementSkipped();
            return;
        }

        if (itemBlockRef.getPosition() == null) {
            log.warn("ItemBlockRef has no position - skipping: {}", itemBlockRef.getId());
            stats.incrementSkipped();
            return;
        }

        // Check if item already exists
        if (itemRegistryService.findItem(worldId, worldId, itemBlockRef.getId()).isPresent()) {
            log.trace("Item already exists: {} - skipping", itemBlockRef.getId());
            stats.incrementSkipped();
            return;
        }

        // Save item position via service (universeId = worldId for main world)
        itemRegistryService.saveItemPosition(worldId, worldId, itemBlockRef);
        stats.incrementSuccess();

        log.debug("Imported item: {} at position ({}, {}, {})",
                itemBlockRef.getId(),
                itemBlockRef.getPosition().getX(),
                itemBlockRef.getPosition().getY(),
                itemBlockRef.getPosition().getZ());
    }
}
