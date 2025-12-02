package de.mhus.nimbus.world.player.api;

import de.mhus.nimbus.generated.types.ItemType;
import de.mhus.nimbus.world.shared.world.WItemType;
import de.mhus.nimbus.world.shared.world.WItemTypeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST Controller for ItemType templates (read-only).
 * Returns only publicData from entities.
 */
@RestController
@RequestMapping("/api/world/itemtypes")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "ItemTypes", description = "ItemType templates for items and inventory")
public class ItemTypeController {

    private final WItemTypeService service;

    @GetMapping("/{itemType}")
    @Operation(summary = "Get ItemType by type", description = "Returns ItemType template for a specific item type")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "ItemType found"),
            @ApiResponse(responseCode = "404", description = "ItemType not found")
    })
    public ResponseEntity<?> getItemType(@PathVariable String itemType) {
        return service.findByItemType(itemType)
                .map(WItemType::getPublicData)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping
    @Operation(summary = "Get all ItemTypes", description = "Returns all enabled ItemType templates, optionally filtered")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List of ItemTypes")
    })
    public ResponseEntity<?> getAllItemTypes(
            @RequestParam(required = false) String regionId,
            @RequestParam(required = false) String worldId) {

        List<ItemType> itemTypes;

        if (regionId != null && !regionId.isBlank()) {
            itemTypes = service.findByRegionId(regionId).stream()
                    .filter(WItemType::isEnabled)
                    .map(WItemType::getPublicData)
                    .toList();
        } else if (worldId != null && !worldId.isBlank()) {
            itemTypes = service.findByWorldId(worldId).stream()
                    .filter(WItemType::isEnabled)
                    .map(WItemType::getPublicData)
                    .toList();
        } else {
            itemTypes = service.findAllEnabled().stream()
                    .map(WItemType::getPublicData)
                    .toList();
        }

        return ResponseEntity.ok(Map.of(
                "itemTypes", itemTypes,
                "count", itemTypes.size()
        ));
    }
}
