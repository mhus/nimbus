package de.mhus.nimbus.world.player.api;

import de.mhus.nimbus.generated.types.BlockType;
import de.mhus.nimbus.shared.types.WorldId;
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

        return WorldId.of(worldId)
                .<ResponseEntity<?>>map(wid -> {
                    List<BlockType> blockTypes = service.findByBlockTypeGroup(wid, groupName).stream()
                            .filter(WBlockType::isEnabled)
                            .map(WBlockType::getPublicData)
                            .toList();

                    log.debug("Returning {} BlockTypes for group: {}", blockTypes.size(), groupName);

                    return ResponseEntity.ok(blockTypes);
                })
                .orElseGet(() -> ResponseEntity.badRequest().body(Map.of("error", "Invalid worldId")));
    }

    /**
     * GET /api/worlds/{worldId}/blocktypes/{blockId}
     * Returns a single BlockType by ID.
     */
    @GetMapping("/blocktypes/{*blockId}")
    @Operation(summary = "Get BlockType by ID", description = "Returns BlockType template for a specific block ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "BlockType found"),
            @ApiResponse(responseCode = "404", description = "BlockType not found")
    })
    public ResponseEntity<?> getBlockType(
            @PathVariable String worldId,
            @PathVariable String blockId) {

        // Strip leading slash from wildcard pattern {*blockId}
        if (blockId != null && blockId.startsWith("/")) {
            blockId = blockId.substring(1);
        }

        // Extract ID from format "w/310" -> "310" or "310" -> "310"
        if (blockId != null && blockId.contains("/")) {
            String[] parts = blockId.split("/", 2);
            if (parts.length == 2) {
                blockId = parts[1];
            }
        }

        final String finalBlockId = blockId;
        log.debug("GET blocktype: blockId={}, worldId={}", finalBlockId, worldId);

        return WorldId.of(worldId)
                .map(wid -> service.findByBlockId(wid, finalBlockId)
                        .map(WBlockType::getPublicData)
                        .map(ResponseEntity::ok)
                        .orElseGet(() -> {
                            log.warn("BlockType not found: blockId={}", finalBlockId);
                            return ResponseEntity.notFound().build();
                        }))
                .orElseGet(() -> ResponseEntity.badRequest().build());
    }

}
