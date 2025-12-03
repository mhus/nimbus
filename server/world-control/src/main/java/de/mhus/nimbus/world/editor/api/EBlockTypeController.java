package de.mhus.nimbus.world.editor.api;

import de.mhus.nimbus.generated.types.BlockType;
import de.mhus.nimbus.world.shared.world.WBlockType;
import de.mhus.nimbus.world.shared.world.WBlockTypeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * REST Controller for BlockType CRUD operations.
 * Base path: /api/worlds/{worldId}/blocktypes
 * <p>
 * BlockTypes are templates that define how blocks look and behave.
 * BlockType IDs have the format {group}:{key} (e.g., "core:stone", "w:123").
 */
@RestController
@RequestMapping("/api/worlds/{worldId}/blocktypes")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "BlockTypes", description = "BlockType template management")
public class EBlockTypeController extends BaseEditorController {

    private final WBlockTypeService blockTypeService;

    // DTOs
    public record BlockTypeDto(
            String blockId,
            String blockTypeGroup,
            BlockType publicData,
            String worldId,
            String regionId,
            boolean enabled,
            Instant createdAt,
            Instant updatedAt
    ) {
    }

    public record CreateBlockTypeRequest(String blockId, BlockType publicData, String blockTypeGroup) {
    }

    public record UpdateBlockTypeRequest(BlockType publicData, String blockTypeGroup, Boolean enabled) {
    }

    /**
     * Get single BlockType by ID.
     * GET /api/worlds/{worldId}/blocktypes/{blockId}
     */
    @GetMapping("/{blockId}")
    @Operation(summary = "Get BlockType by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "BlockType found"),
            @ApiResponse(responseCode = "400", description = "Invalid parameters"),
            @ApiResponse(responseCode = "404", description = "BlockType not found")
    })
    public ResponseEntity<?> get(
            @Parameter(description = "World identifier") @PathVariable String worldId,
            @Parameter(description = "Block identifier") @PathVariable String blockId) {

        log.debug("GET blocktype: worldId={}, blockId={}", worldId, blockId);

        ResponseEntity<?> validation = validateWorldId(worldId);
        if (validation != null) return validation;

        validation = validateId(blockId, "blockId");
        if (validation != null) return validation;

        Optional<WBlockType> opt = blockTypeService.findByBlockId(blockId);
        if (opt.isEmpty()) {
            log.warn("BlockType not found: blockId={}", blockId);
            return notFound("blocktype not found");
        }

        log.debug("Returning blocktype: blockId={}", blockId);
        return ResponseEntity.ok(toDto(opt.get()));
    }

    /**
     * List all BlockTypes for a world with optional search filter and pagination.
     * GET /api/worlds/{worldId}/blocktypes?query=...&offset=0&limit=50
     */
    @GetMapping
    @Operation(summary = "List all BlockTypes")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Success"),
            @ApiResponse(responseCode = "400", description = "Invalid parameters")
    })
    public ResponseEntity<?> list(
            @Parameter(description = "World identifier") @PathVariable String worldId,
            @Parameter(description = "Search query") @RequestParam(required = false) String query,
            @Parameter(description = "Pagination offset") @RequestParam(defaultValue = "0") int offset,
            @Parameter(description = "Pagination limit") @RequestParam(defaultValue = "50") int limit) {

        log.debug("LIST blocktypes: worldId={}, query={}, offset={}, limit={}", worldId, query, offset, limit);

        ResponseEntity<?> validation = validateWorldId(worldId);
        if (validation != null) return validation;

        validation = validatePagination(offset, limit);
        if (validation != null) return validation;

        // Get all BlockTypes for this world
        List<WBlockType> all = blockTypeService.findByWorldId(worldId);

        // Apply search filter if provided
        if (query != null && !query.isBlank()) {
            all = filterByQuery(all, query);
        }

        int totalCount = all.size();

        // Apply pagination
        List<BlockTypeDto> dtos = all.stream()
                .skip(offset)
                .limit(limit)
                .map(this::toDto)
                .collect(Collectors.toList());

        log.debug("Returning {} blocktypes (total: {})", dtos.size(), totalCount);

        // TypeScript compatible format
        return ResponseEntity.ok(Map.of(
                "blocktypes", dtos,
                "count", totalCount
        ));
    }

    /**
     * Get BlockTypes by group.
     * GET /api/worlds/{worldId}/blocktypeschunk/{groupName}
     * <p>
     * This is a special endpoint to load BlockTypes grouped by their group prefix
     * (e.g., "core" for "core:stone", "w" for "w:123").
     */
    @GetMapping("../blocktypeschunk/{groupName}")
    @Operation(summary = "Get BlockTypes by group")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Success"),
            @ApiResponse(responseCode = "400", description = "Invalid parameters")
    })
    public ResponseEntity<?> getByGroup(
            @Parameter(description = "World identifier") @PathVariable String worldId,
            @Parameter(description = "BlockType group name") @PathVariable String groupName) {

        log.debug("GET blocktypes by group: worldId={}, groupName={}", worldId, groupName);

        ResponseEntity<?> validation = validateWorldId(worldId);
        if (validation != null) return validation;

        if (blank(groupName)) {
            return bad("groupName required");
        }

        // Validate group name format (lowercase alphanumeric, dash, underscore)
        if (!groupName.matches("^[a-z0-9_-]+$")) {
            return bad("groupName must be lowercase alphanumeric with dash or underscore");
        }

        List<WBlockType> blockTypes = blockTypeService.findByBlockTypeGroup(groupName);

        // Map to DTOs (publicData only for TypeScript compatibility)
        List<BlockType> publicDataList = blockTypes.stream()
                .map(WBlockType::getPublicData)
                .collect(Collectors.toList());

        log.debug("Returning {} blocktypes for group: {}", publicDataList.size(), groupName);

        // Return array directly (TypeScript test_server format)
        return ResponseEntity.ok(publicDataList);
    }

    /**
     * Create new BlockType.
     * POST /api/worlds/{worldId}/blocktypes
     */
    @PostMapping
    @Operation(summary = "Create new BlockType")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "BlockType created"),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "409", description = "BlockType already exists")
    })
    public ResponseEntity<?> create(
            @Parameter(description = "World identifier") @PathVariable String worldId,
            @RequestBody CreateBlockTypeRequest request) {

        log.debug("CREATE blocktype: worldId={}, blockId={}", worldId, request.blockId());

        ResponseEntity<?> validation = validateWorldId(worldId);
        if (validation != null) return validation;

        if (blank(request.blockId())) {
            return bad("blockId required");
        }

        if (request.publicData() == null) {
            return bad("publicData required");
        }

        // Check if BlockType already exists
        if (blockTypeService.findByBlockId(request.blockId()).isPresent()) {
            return conflict("blocktype already exists");
        }

        try {
            // Extract or set blockTypeGroup
            final String blockTypeGroup = blank(request.blockTypeGroup())
                    ? extractGroupFromBlockId(request.blockId())
                    : request.blockTypeGroup();

            WBlockType saved = blockTypeService.save(request.blockId(), request.publicData(), null, worldId);

            // Set the blockTypeGroup
            blockTypeService.update(request.blockId(), blockType -> {
                blockType.setBlockTypeGroup(blockTypeGroup);
            });

            // Reload to get updated entity
            saved = blockTypeService.findByBlockId(request.blockId()).orElse(saved);

            log.info("Created blocktype: blockId={}, group={}", request.blockId(), blockTypeGroup);
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("blockId", saved.getBlockId()));
        } catch (IllegalArgumentException e) {
            log.warn("Validation error creating blocktype: {}", e.getMessage());
            return bad(e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error creating blocktype", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Internal server error"));
        }
    }

    /**
     * Update existing BlockType.
     * PUT /api/worlds/{worldId}/blocktypes/{blockId}
     */
    @PutMapping("/{blockId}")
    @Operation(summary = "Update BlockType")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "BlockType updated"),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "404", description = "BlockType not found")
    })
    public ResponseEntity<?> update(
            @Parameter(description = "World identifier") @PathVariable String worldId,
            @Parameter(description = "Block identifier") @PathVariable String blockId,
            @RequestBody UpdateBlockTypeRequest request) {

        log.debug("UPDATE blocktype: worldId={}, blockId={}", worldId, blockId);

        ResponseEntity<?> validation = validateWorldId(worldId);
        if (validation != null) return validation;

        validation = validateId(blockId, "blockId");
        if (validation != null) return validation;

        if (request.publicData() == null && request.blockTypeGroup() == null && request.enabled() == null) {
            return bad("at least one field required for update");
        }

        Optional<WBlockType> updated = blockTypeService.update(blockId, blockType -> {
            if (request.publicData() != null) {
                blockType.setPublicData(request.publicData());
            }
            if (request.blockTypeGroup() != null) {
                blockType.setBlockTypeGroup(request.blockTypeGroup());
            }
            if (request.enabled() != null) {
                blockType.setEnabled(request.enabled());
            }
            blockType.setWorldId(worldId);
        });

        if (updated.isEmpty()) {
            log.warn("BlockType not found for update: blockId={}", blockId);
            return notFound("blocktype not found");
        }

        log.info("Updated blocktype: blockId={}", blockId);
        return ResponseEntity.ok(toDto(updated.get()));
    }

    /**
     * Delete BlockType.
     * DELETE /api/worlds/{worldId}/blocktypes/{blockId}
     */
    @DeleteMapping("/{blockId}")
    @Operation(summary = "Delete BlockType")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "BlockType deleted"),
            @ApiResponse(responseCode = "400", description = "Invalid parameters"),
            @ApiResponse(responseCode = "404", description = "BlockType not found")
    })
    public ResponseEntity<?> delete(
            @Parameter(description = "World identifier") @PathVariable String worldId,
            @Parameter(description = "Block identifier") @PathVariable String blockId) {

        log.debug("DELETE blocktype: worldId={}, blockId={}", worldId, blockId);

        ResponseEntity<?> validation = validateWorldId(worldId);
        if (validation != null) return validation;

        validation = validateId(blockId, "blockId");
        if (validation != null) return validation;

        boolean deleted = blockTypeService.delete(blockId);
        if (!deleted) {
            log.warn("BlockType not found for deletion: blockId={}", blockId);
            return notFound("blocktype not found");
        }

        log.info("Deleted blocktype: blockId={}", blockId);
        return ResponseEntity.noContent().build();
    }

    // Helper methods

    private BlockTypeDto toDto(WBlockType entity) {
        return new BlockTypeDto(
                entity.getBlockId(),
                entity.getBlockTypeGroup(),
                entity.getPublicData(),
                entity.getWorldId(),
                entity.getRegionId(),
                entity.isEnabled(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private List<WBlockType> filterByQuery(List<WBlockType> blockTypes, String query) {
        String lowerQuery = query.toLowerCase();
        return blockTypes.stream()
                .filter(blockType -> {
                    String blockId = blockType.getBlockId();
                    BlockType publicData = blockType.getPublicData();
                    return (blockId != null && blockId.toLowerCase().contains(lowerQuery)) ||
                            (publicData != null && publicData.getDescription() != null &&
                                    publicData.getDescription().toLowerCase().contains(lowerQuery));
                })
                .collect(Collectors.toList());
    }

    /**
     * Extract group from blockId.
     * Format: "{group}:{key}" (e.g., "core:stone" → "core", "w:123" → "w")
     * If no group prefix, defaults to "w".
     */
    private String extractGroupFromBlockId(String blockId) {
        if (blockId == null || !blockId.contains(":")) {
            return "w";  // default group
        }
        String[] parts = blockId.split(":", 2);
        String group = parts[0].toLowerCase();
        // Validate group format
        if (group.matches("^[a-z0-9_-]+$")) {
            return group;
        }
        return "w";
    }
}
