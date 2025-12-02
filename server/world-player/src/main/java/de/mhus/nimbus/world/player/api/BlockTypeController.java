package de.mhus.nimbus.world.player.api;

import de.mhus.nimbus.generated.types.BlockType;
import de.mhus.nimbus.world.shared.world.WBlockType;
import de.mhus.nimbus.world.shared.world.WBlockTypeService;
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
 * REST Controller for BlockType templates (read-only).
 * Returns only publicData from entities.
 */
@RestController
@RequestMapping("/api/worlds/{worldId}")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "BlockTypes", description = "BlockType templates for rendering blocks")
public class BlockTypeController {

    private final WBlockTypeService service;

    /**
     * GET /api/worlds/{worldId}/blocktypeschunk/{groupName}
     * Returns all BlockTypes in a specific group for chunked loading.
     */
    @GetMapping("/blocktypeschunk/{groupName}")
    @Operation(summary = "Get BlockTypes by group", description = "Returns all BlockTypes in a specific group for chunked loading")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List of BlockTypes in group"),
            @ApiResponse(responseCode = "400", description = "Invalid group name")
    })
    public ResponseEntity<?> getBlockTypesByGroup(
            @PathVariable String worldId,
            @PathVariable String groupName) {

        // Validate group name (only a-z0-9_- allowed)
        if (!groupName.matches("^[a-z0-9_-]+$")) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Invalid group name. Only lowercase letters, numbers, hyphens and underscores allowed."));
        }

        List<BlockType> blockTypes = service.findByBlockTypeGroup(groupName).stream()
                .filter(WBlockType::isEnabled)
                .map(WBlockType::getPublicData)
                .toList();

        log.debug("Returning {} BlockTypes for group: {}", blockTypes.size(), groupName);

        return ResponseEntity.ok(blockTypes);
    }

    /**
     * GET /api/worlds/{worldId}/blocktypes/{blockId}
     * Returns a single BlockType by ID.
     */
    @GetMapping("/blocktypes/{blockId}")
    @Operation(summary = "Get BlockType by ID", description = "Returns BlockType template for a specific block ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "BlockType found"),
            @ApiResponse(responseCode = "404", description = "BlockType not found")
    })
    public ResponseEntity<?> getBlockType(
            @PathVariable String worldId,
            @PathVariable String blockId) {
        return service.findByBlockId(blockId)
                .map(WBlockType::getPublicData)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * GET /api/worlds/{worldId}/blocktypes
     * Returns all BlockTypes, optionally filtered.
     */
    @GetMapping("/blocktypes")
    @Operation(summary = "Get all BlockTypes", description = "Returns all enabled BlockType templates, optionally filtered")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List of BlockTypes")
    })
    public ResponseEntity<?> getAllBlockTypes(
            @PathVariable String worldId,
            @RequestParam(required = false) String regionId) {

        List<BlockType> blockTypes;

        if (regionId != null && !regionId.isBlank()) {
            blockTypes = service.findByRegionId(regionId).stream()
                    .filter(WBlockType::isEnabled)
                    .map(WBlockType::getPublicData)
                    .toList();
        } else if (worldId != null && !worldId.isBlank()) {
            blockTypes = service.findByWorldId(worldId).stream()
                    .filter(WBlockType::isEnabled)
                    .map(WBlockType::getPublicData)
                    .toList();
        } else {
            blockTypes = service.findAllEnabled().stream()
                    .map(WBlockType::getPublicData)
                    .toList();
        }

        return ResponseEntity.ok(Map.of(
                "blockTypes", blockTypes,
                "count", blockTypes.size()
        ));
    }
}
