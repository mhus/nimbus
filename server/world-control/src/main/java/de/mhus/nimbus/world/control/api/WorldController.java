package de.mhus.nimbus.world.control.api;

import de.mhus.nimbus.world.shared.world.WWorld;
import de.mhus.nimbus.world.shared.world.WWorldService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * REST Controller for World operations.
 * Base path: /api/worlds
 * <p>
 * Provides access to world metadata and configuration.
 */
@RestController
@RequestMapping("/api/worlds")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Worlds", description = "World metadata and configuration")
public class WorldController extends BaseEditorController {

    private final WWorldService worldService;
    private final de.mhus.nimbus.world.control.service.BlockInfoService blockInfoService;

    // DTOs
    public record WorldListDto(
            String worldId,
            String name,
            String description,
            Integer chunkSize,
            String status
    ) {
    }

    public record WorldDetailDto(
            String worldId,
            String name,
            String description,
            Integer chunkSize,
            String status,
            String regionId,
            List<String> owner,
            Boolean publicFlag,
            List<String> editor,
            List<String> player
    ) {
    }

    /**
     * List all worlds.
     * GET /api/worlds
     */
    @GetMapping
    @Operation(summary = "List all worlds")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Success")
    })
    public ResponseEntity<?> list() {
        log.debug("LIST worlds");

        List<WWorld> all = worldService.findAll();

        List<WorldListDto> dtos = all.stream()
                .map(this::toListDto)
                .collect(Collectors.toList());

        log.debug("Returning {} worlds", dtos.size());
        return ResponseEntity.ok(dtos);
    }

    /**
     * Get single world by ID.
     * GET /api/worlds/{worldId}
     */
    @GetMapping("/{worldId}")
    @Operation(summary = "Get world by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "World found"),
            @ApiResponse(responseCode = "400", description = "Invalid parameters"),
            @ApiResponse(responseCode = "404", description = "World not found")
    })
    public ResponseEntity<?> get(
            @Parameter(description = "World identifier") @PathVariable String worldId) {

        log.debug("GET world: worldId={}", worldId);

        ResponseEntity<?> validation = validateId(worldId, "worldId");
        if (validation != null) return validation;

        Optional<WWorld> opt = worldService.getByWorldId(worldId);
        if (opt.isEmpty()) {
            log.warn("World not found: worldId={}", worldId);
            return notFound("world not found");
        }

        log.debug("Returning world: worldId={}", worldId);
        return ResponseEntity.ok(toDetailDto(opt.get()));
    }

    /**
     * Get block info with layer metadata.
     * GET /api/worlds/{worldId}/session/{sessionId}/block/{x}/{y}/{z}
     */
    @GetMapping("/{worldId}/session/{sessionId}/block/{x}/{y}/{z}")
    @Operation(summary = "Get block info with layer metadata")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Block info loaded"),
            @ApiResponse(responseCode = "400", description = "Invalid parameters"),
            @ApiResponse(responseCode = "404", description = "Block/Chunk not found")
    })
    public ResponseEntity<?> getBlock(
            @Parameter(description = "World identifier") @PathVariable String worldId,
            @Parameter(description = "Session identifier") @PathVariable String sessionId,
            @Parameter(description = "Block X coordinate") @PathVariable int x,
            @Parameter(description = "Block Y coordinate") @PathVariable int y,
            @Parameter(description = "Block Z coordinate") @PathVariable int z) {

        log.debug("GET block: worldId={} session={} pos=({},{},{})", worldId, sessionId, x, y, z);

        ResponseEntity<?> validation = validateId(worldId, "worldId");
        if (validation != null) return validation;

        validation = validateId(sessionId, "sessionId");
        if (validation != null) return validation;

        try {
            // Load block info with layer metadata
            java.util.Map<String, Object> blockInfo = blockInfoService.loadBlockInfo(worldId, sessionId, x, y, z);

            log.debug("Block info loaded: pos=({},{},{}) layer={} readOnly={}",
                    x, y, z, blockInfo.get("layer"), blockInfo.get("readOnly"));

            return ResponseEntity.ok(blockInfo);

        } catch (Exception e) {
            log.error("Failed to load block info: worldId={} pos=({},{},{})", worldId, x, y, z, e);
            return ResponseEntity.status(500).body(java.util.Map.of("error", "Failed to load block: " + e.getMessage()));
        }
    }

    // Helper methods

    private WorldListDto toListDto(WWorld world) {
        return new WorldListDto(
                world.getWorldId(),
                world.getPublicData() != null ? world.getPublicData().getName() : null,
                world.getPublicData() != null ? world.getPublicData().getDescription() : null,
                16, // Default chunk size
                world.isEnabled() ? "active" : "inactive"
        );
    }

    private WorldDetailDto toDetailDto(WWorld world) {
        return new WorldDetailDto(
                world.getWorldId(),
                world.getPublicData() != null ? world.getPublicData().getName() : null,
                world.getPublicData() != null ? world.getPublicData().getDescription() : null,
                16, // Default chunk size
                world.isEnabled() ? "active" : "inactive",
                world.getRegionId(),
                world.getOwner(),
                world.isPublicFlag(),
                world.getEditor(),
                world.getPlayer()
        );
    }
}
