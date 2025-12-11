package de.mhus.nimbus.world.control.api;

import de.mhus.nimbus.generated.types.ItemType;
import de.mhus.nimbus.shared.types.WorldId;
import de.mhus.nimbus.world.shared.world.WItemType;
import de.mhus.nimbus.world.shared.world.WItemTypeService;
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
 * REST Controller for ItemType CRUD operations.
 * Base path: /api/worlds/{worldId}/itemtypes
 * <p>
 * ItemTypes define the properties and behavior of items in the world.
 */
@RestController
@RequestMapping("/api/worlds/{worldId}/itemtypes")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "ItemTypes", description = "ItemType template management")
public class EItemTypeController extends BaseEditorController {

    private final WItemTypeService itemTypeService;

    // DTOs
    public record ItemTypeDto(
            String itemType,
            ItemType publicData,
            String worldId,
            boolean enabled,
            Instant createdAt,
            Instant updatedAt
    ) {
    }

    public record CreateItemTypeRequest(String itemType, ItemType publicData) {
    }

    public record UpdateItemTypeRequest(ItemType publicData, Boolean enabled) {
    }

    /**
     * Get single ItemType by ID.
     * GET /api/worlds/{worldId}/itemtypes/{itemType}
     */
    @GetMapping("/{itemType}")
    @Operation(summary = "Get ItemType by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "ItemType found"),
            @ApiResponse(responseCode = "400", description = "Invalid parameters"),
            @ApiResponse(responseCode = "404", description = "ItemType not found")
    })
    public ResponseEntity<?> get(
            @Parameter(description = "World identifier") @PathVariable String worldId,
            @Parameter(description = "ItemType identifier") @PathVariable String itemType) {

        log.debug("GET itemtype: worldId={}, itemType={}", worldId, itemType);

        ResponseEntity<?> validation = validateWorldId(worldId);
        if (validation != null) return validation;

        validation = validateId(itemType, "itemType");
        if (validation != null) return validation;

        Optional<WorldId> widOpt = WorldId.of(worldId);
        if (widOpt.isEmpty()) {
            return bad("invalid worldId");
        }

        Optional<WItemType> opt = itemTypeService.findByItemType(widOpt.get(), itemType);
        if (opt.isEmpty()) {
            log.warn("ItemType not found: itemType={}", itemType);
            return notFound("itemtype not found");
        }

        log.debug("Returning itemtype: itemType={}", itemType);
        // Return publicData only (match test_server format)
        return ResponseEntity.ok(opt.get().getPublicData());
    }

    /**
     * List all ItemTypes for a world with optional search filter and pagination.
     * GET /api/worlds/{worldId}/itemtypes?query=...&offset=0&limit=50
     */
    @GetMapping
    @Operation(summary = "List all ItemTypes")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Success"),
            @ApiResponse(responseCode = "400", description = "Invalid parameters")
    })
    public ResponseEntity<?> list(
            @Parameter(description = "World identifier") @PathVariable String worldId,
            @Parameter(description = "Search query") @RequestParam(required = false) String query,
            @Parameter(description = "Pagination offset") @RequestParam(defaultValue = "0") int offset,
            @Parameter(description = "Pagination limit") @RequestParam(defaultValue = "50") int limit) {

        log.debug("LIST itemtypes: worldId={}, query={}, offset={}, limit={}", worldId, query, offset, limit);

        ResponseEntity<?> validation = validateWorldId(worldId);
        if (validation != null) return validation;

        validation = validatePagination(offset, limit);
        if (validation != null) return validation;

        Optional<WorldId> widOpt = WorldId.of(worldId);
        if (widOpt.isEmpty()) {
            return bad("invalid worldId");
        }

        // Get all ItemTypes for this world
        List<WItemType> all = itemTypeService.findByWorldId(widOpt.get());

        // Apply search filter if provided
        if (query != null && !query.isBlank()) {
            all = filterByQuery(all, query);
        }

        int totalCount = all.size();

        // Apply pagination
        List<ItemType> publicDataList = all.stream()
                .skip(offset)
                .limit(limit)
                .map(WItemType::getPublicData)
                .collect(Collectors.toList());

        log.debug("Returning {} itemtypes (total: {})", publicDataList.size(), totalCount);

        // TypeScript compatible format (match test_server response)
        return ResponseEntity.ok(Map.of(
                "itemTypes", publicDataList,
                "count", totalCount,
                "limit", limit,
                "offset", offset
        ));
    }

    /**
     * Create new ItemType.
     * POST /api/worlds/{worldId}/itemtypes
     */
    @PostMapping
    @Operation(summary = "Create new ItemType")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "ItemType created"),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "409", description = "ItemType already exists")
    })
    public ResponseEntity<?> create(
            @Parameter(description = "World identifier") @PathVariable String worldId,
            @RequestBody CreateItemTypeRequest request) {

        log.debug("CREATE itemtype: worldId={}, itemType={}", worldId, request.itemType());

        ResponseEntity<?> validation = validateWorldId(worldId);
        if (validation != null) return validation;

        if (blank(request.itemType())) {
            return bad("itemType required");
        }

        if (request.publicData() == null) {
            return bad("publicData required");
        }

        Optional<WorldId> widOpt = WorldId.of(worldId);
        if (widOpt.isEmpty()) {
            return bad("invalid worldId");
        }
        WorldId wid = widOpt.get();

        // Check if ItemType already exists
        if (itemTypeService.findByItemType(wid, request.itemType()).isPresent()) {
            return conflict("itemtype already exists");
        }

        try {
            WItemType saved = itemTypeService.save(wid, request.itemType(), request.publicData());
            log.info("Created itemtype: itemType={}", request.itemType());
            return ResponseEntity.status(HttpStatus.CREATED).body(toDto(saved));
        } catch (IllegalArgumentException e) {
            log.warn("Validation error creating itemtype: {}", e.getMessage());
            return bad(e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error creating itemtype", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Internal server error"));
        }
    }

    /**
     * Update existing ItemType.
     * PUT /api/worlds/{worldId}/itemtypes/{itemType}
     */
    @PutMapping("/{itemType}")
    @Operation(summary = "Update ItemType")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "ItemType updated"),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "404", description = "ItemType not found")
    })
    public ResponseEntity<?> update(
            @Parameter(description = "World identifier") @PathVariable String worldId,
            @Parameter(description = "ItemType identifier") @PathVariable String itemType,
            @RequestBody UpdateItemTypeRequest request) {

        log.debug("UPDATE itemtype: worldId={}, itemType={}", worldId, itemType);

        ResponseEntity<?> validation = validateWorldId(worldId);
        if (validation != null) return validation;

        validation = validateId(itemType, "itemType");
        if (validation != null) return validation;

        if (request.publicData() == null && request.enabled() == null) {
            return bad("at least one field required for update");
        }

        Optional<WorldId> widOpt = WorldId.of(worldId);
        if (widOpt.isEmpty()) {
            return bad("invalid worldId");
        }

        Optional<WItemType> updated = itemTypeService.update(widOpt.get(), itemType, item -> {
            if (request.publicData() != null) {
                item.setPublicData(request.publicData());
            }
            if (request.enabled() != null) {
                item.setEnabled(request.enabled());
            }
            item.setWorldId(worldId);
        });

        if (updated.isEmpty()) {
            log.warn("ItemType not found for update: itemType={}", itemType);
            return notFound("itemtype not found");
        }

        log.info("Updated itemtype: itemType={}", itemType);
        return ResponseEntity.ok(toDto(updated.get()));
    }

    /**
     * Delete ItemType.
     * DELETE /api/worlds/{worldId}/itemtypes/{itemType}
     */
    @DeleteMapping("/{itemType}")
    @Operation(summary = "Delete ItemType")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "ItemType deleted"),
            @ApiResponse(responseCode = "400", description = "Invalid parameters"),
            @ApiResponse(responseCode = "404", description = "ItemType not found")
    })
    public ResponseEntity<?> delete(
            @Parameter(description = "World identifier") @PathVariable String worldId,
            @Parameter(description = "ItemType identifier") @PathVariable String itemType) {

        log.debug("DELETE itemtype: worldId={}, itemType={}", worldId, itemType);

        ResponseEntity<?> validation = validateWorldId(worldId);
        if (validation != null) return validation;

        validation = validateId(itemType, "itemType");
        if (validation != null) return validation;

        Optional<WorldId> widOpt = WorldId.of(worldId);
        if (widOpt.isEmpty()) {
            return bad("invalid worldId");
        }

        boolean deleted = itemTypeService.delete(widOpt.get(), itemType);
        if (!deleted) {
            log.warn("ItemType not found for deletion: itemType={}", itemType);
            return notFound("itemtype not found");
        }

        log.info("Deleted itemtype: itemType={}", itemType);
        return ResponseEntity.noContent().build();
    }

    // Helper methods

    private ItemTypeDto toDto(WItemType entity) {
        return new ItemTypeDto(
                entity.getItemType(),
                entity.getPublicData(),
                entity.getWorldId(),
                entity.isEnabled(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private List<WItemType> filterByQuery(List<WItemType> itemTypes, String query) {
        String lowerQuery = query.toLowerCase();
        return itemTypes.stream()
                .filter(itemType -> {
                    String type = itemType.getItemType();
                    ItemType publicData = itemType.getPublicData();
                    return (type != null && type.toLowerCase().contains(lowerQuery)) ||
                            (publicData != null && publicData.getName() != null &&
                                    publicData.getName().toLowerCase().contains(lowerQuery));
                })
                .collect(Collectors.toList());
    }
}
