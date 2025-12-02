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
@RequestMapping("/api/world/blocktypes")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "BlockTypes", description = "BlockType templates for rendering blocks")
public class BlockTypeController {

    private final WBlockTypeService service;

    @GetMapping("/{blockId}")
    @Operation(summary = "Get BlockType by ID", description = "Returns BlockType template for a specific block ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "BlockType found"),
            @ApiResponse(responseCode = "404", description = "BlockType not found")
    })
    public ResponseEntity<?> getBlockType(@PathVariable String blockId) {
        return service.findByBlockId(blockId)
                .map(WBlockType::getPublicData)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping
    @Operation(summary = "Get all BlockTypes", description = "Returns all enabled BlockType templates, optionally filtered")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List of BlockTypes")
    })
    public ResponseEntity<?> getAllBlockTypes(
            @RequestParam(required = false) String regionId,
            @RequestParam(required = false) String worldId) {

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
