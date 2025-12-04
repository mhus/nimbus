package de.mhus.nimbus.tools.demoimport.importers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.mhus.nimbus.generated.types.Item;
import de.mhus.nimbus.generated.types.ItemBlockRef;
import de.mhus.nimbus.tools.demoimport.ImportStats;
import de.mhus.nimbus.world.shared.world.WItemRegistryService;
import de.mhus.nimbus.world.shared.world.WItemService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Imports Items and ItemPositions from test_server data.
 * Reads from: {data-path}/worlds/main/items.json
 * <p>
 * Structure of items.json:
 * [
 *   {
 *     "item": { id, itemType, name, description, modifier, parameters },
 *     "itemBlockRef": { id, position, texture, scale... }  // optional, for placed items
 *   }
 * ]
 * <p>
 * - Items (without position) → WItem (w_items collection)
 * - ItemPositions (with position) → WItemPosition (w_item_positions collection)
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ItemImporter {

    private final WItemService itemService;
    private final WItemRegistryService itemRegistryService;
    private final ObjectMapper objectMapper;

    @Value("${import.data-path:../../client/packages/test_server/data}")
    private String dataPath;

    @Value("${import.world-id:main}")
    private String worldId;

    /**
     * Import all items from items.json file.
     * Imports both Items (w_items) and ItemPositions (w_item_positions).
     *
     * @return Import statistics
     * @throws Exception if import fails
     */
    public ImportStats importAll() throws Exception {
        log.info("Starting Item/ItemPosition import from: {}/worlds/{}/items.json", dataPath, worldId);

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

        log.info("Item/ItemPosition import completed: {} imported, {} skipped, {} failed",
                stats.getSuccessCount(), stats.getSkippedCount(), stats.getFailureCount());

        return stats;
    }

    /**
     * Import a single ServerItem.
     * Structure:
     * {
     *   "item": { id, itemType, name, ... },        // always present
     *   "itemBlockRef": { id, position, ... }      // optional, for placed items
     * }
     * <p>
     * - If only "item" exists → Import as WItem (inventory item)
     * - If "itemBlockRef" exists → Import as WItemPosition (placed item)
     *
     * @param serverItemNode JSON node containing ServerItem data
     * @param stats Import statistics
     */
    private void importServerItem(JsonNode serverItemNode, ImportStats stats) throws Exception {
        // Import Item (always present)
        if (serverItemNode.has("item")) {
            JsonNode itemNode = serverItemNode.get("item");
            Item item = objectMapper.treeToValue(itemNode, Item.class);

            if (item.getId() == null || item.getId().isBlank()) {
                log.warn("Item has no ID - skipping");
                stats.incrementSkipped();
                return;
            }

            // Check if item already exists
            if (itemService.findByItemId(worldId, item.getId()).isEmpty()) {
                // Save Item (regionId=null for world-level items)
                itemService.save(worldId, item.getId(), item, null);
                log.debug("Imported item: {} (type: {})", item.getId(), item.getItemType());
            } else {
                log.trace("Item already exists: {} - skipping", item.getId());
            }
        }

        // Import ItemPosition if present (placed item with position)
        if (serverItemNode.has("itemBlockRef")) {
            JsonNode itemBlockRefNode = serverItemNode.get("itemBlockRef");
            ItemBlockRef itemBlockRef = objectMapper.treeToValue(itemBlockRefNode, ItemBlockRef.class);

            // Validate required fields
            if (itemBlockRef.getId() == null || itemBlockRef.getId().isBlank()) {
                log.warn("ItemBlockRef has no ID - skipping");
                stats.incrementSkipped();
                return;
            }

            if (itemBlockRef.getPosition() == null) {
                log.warn("ItemBlockRef has no position - skipping: {}", itemBlockRef.getId());
                stats.incrementSkipped();
                return;
            }

            // Check if item position already exists
            if (itemRegistryService.findItem(worldId, worldId, itemBlockRef.getId()).isEmpty()) {
                // Save item position via service (universeId = worldId for main world)
                itemRegistryService.saveItemPosition(worldId, worldId, itemBlockRef);
                log.debug("Imported item position: {} at ({}, {}, {})",
                        itemBlockRef.getId(),
                        itemBlockRef.getPosition().getX(),
                        itemBlockRef.getPosition().getY(),
                        itemBlockRef.getPosition().getZ());
            } else {
                log.trace("Item position already exists: {} - skipping", itemBlockRef.getId());
            }
        }

        stats.incrementSuccess();
    }
}
