package de.mhus.nimbus.world.control.api;

import de.mhus.nimbus.shared.types.WorldId;
import de.mhus.nimbus.world.shared.layer.LayerType;
import de.mhus.nimbus.world.shared.layer.WLayer;
import de.mhus.nimbus.world.shared.layer.WLayerService;
import de.mhus.nimbus.world.shared.rest.BaseEditorController;
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
 * REST Controller for Layer CRUD operations.
 * Base path: /control/worlds/{worldId}/layers
 * <p>
 * Layers are used to organize and manage world content in separate overlays.
 */
@RestController
@RequestMapping("/control/worlds/{worldId}/layers")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Layers", description = "Layer management for world content organization")
public class ELayerController extends BaseEditorController {

    private final WLayerService layerService;

    // DTOs
    public record LayerDto(
            String id,
            String worldId,
            String name,
            LayerType layerType,
            String layerDataId,
            Integer mountX,
            Integer mountY,
            Integer mountZ,
            boolean ground,
            boolean allChunks,
            List<String> affectedChunks,
            int order,
            boolean enabled,
            Map<Integer, String> groups,
            Instant createdAt,
            Instant updatedAt
    ) {
    }

    public record CreateLayerRequest(
            String name,
            LayerType layerType,
            Integer mountX,
            Integer mountY,
            Integer mountZ,
            Boolean ground,
            Boolean allChunks,
            List<String> affectedChunks,
            Integer order,
            Boolean enabled,
            Map<Integer, String> groups
    ) {
    }

    public record UpdateLayerRequest(
            String name,
            Integer mountX,
            Integer mountY,
            Integer mountZ,
            Boolean ground,
            Boolean allChunks,
            List<String> affectedChunks,
            Integer order,
            Boolean enabled,
            Map<Integer, String> groups
    ) {
    }

    /**
     * Get single Layer by ID.
     * GET /control/worlds/{worldId}/layers/{id}
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get Layer by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Layer found"),
            @ApiResponse(responseCode = "400", description = "Invalid parameters"),
            @ApiResponse(responseCode = "404", description = "Layer not found")
    })
    public ResponseEntity<?> get(
            @Parameter(description = "World identifier") @PathVariable String worldId,
            @Parameter(description = "Layer identifier") @PathVariable String id) {

        log.debug("GET layer: worldId={}, id={}", worldId, id);

        WorldId.of(worldId).orElseThrow(
                () -> new IllegalStateException("Invalid worldId: " + worldId)
        );
        var validation = validateId(id, "id");
        if (validation != null) return validation;

        Optional<WLayer> opt = layerService.findById(id);
        if (opt.isEmpty()) {
            log.warn("Layer not found: id={}", id);
            return notFound("layer not found");
        }

        WLayer layer = opt.get();
        if (!layer.getWorldId().equals(worldId)) {
            log.warn("Layer worldId mismatch: expected={}, actual={}", worldId, layer.getWorldId());
            return notFound("layer not found");
        }

        log.debug("Returning layer: id={}", id);
        return ResponseEntity.ok(toDto(layer));
    }

    /**
     * List all Layers for a world with optional search filter and pagination.
     * GET /control/worlds/{worldId}/layers?query=...&offset=0&limit=50
     */
    @GetMapping
    @Operation(summary = "List all Layers")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Success"),
            @ApiResponse(responseCode = "400", description = "Invalid parameters")
    })
    public ResponseEntity<?> list(
            @Parameter(description = "World identifier") @PathVariable String worldId,
            @Parameter(description = "Search query") @RequestParam(required = false) String query,
            @Parameter(description = "Pagination offset") @RequestParam(defaultValue = "0") int offset,
            @Parameter(description = "Pagination limit") @RequestParam(defaultValue = "50") int limit) {

        log.debug("LIST layers: worldId={}, query={}, offset={}, limit={}", worldId, query, offset, limit);

        var wid = WorldId.of(worldId).orElseThrow(
                () -> new IllegalStateException("Invalid worldId: " + worldId)
        );
        var validation = validatePagination(offset, limit);
        if (validation != null) return validation;

        // IMPORTANT: Filter out instances - layers are per world/zone only
        String lookupWorldId = wid.withoutInstance().getId();

        // Get all Layers for this world with query filter
        List<WLayer> all = layerService.findByWorldIdAndQuery(lookupWorldId, query);

        int totalCount = all.size();

        // Apply pagination
        List<LayerDto> layerDtos = all.stream()
                .skip(offset)
                .limit(limit)
                .map(this::toDto)
                .collect(Collectors.toList());

        log.debug("Returning {} layers (total: {})", layerDtos.size(), totalCount);

        return ResponseEntity.ok(Map.of(
                "layers", layerDtos,
                "count", totalCount,
                "limit", limit,
                "offset", offset
        ));
    }

    /**
     * Create new Layer.
     * POST /control/worlds/{worldId}/layers
     */
    @PostMapping
    @Operation(summary = "Create new Layer")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Layer created"),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "409", description = "Layer name already exists")
    })
    public ResponseEntity<?> create(
            @Parameter(description = "World identifier") @PathVariable String worldId,
            @RequestBody CreateLayerRequest request) {

        log.debug("CREATE layer: worldId={}, name={}", worldId, request.name());

        var wid = WorldId.of(worldId).orElseThrow(
                () -> new IllegalStateException("Invalid worldId: " + worldId)
        );
        if (blank(request.name())) {
            return bad("name required");
        }

        if (request.layerType() == null) {
            return bad("layerType required");
        }

        // IMPORTANT: Filter out instances - layers are per world/zone only
        String lookupWorldId = wid.withoutInstance().getId();

        // Check if Layer with same name already exists
        if (layerService.findByWorldIdAndName(lookupWorldId, request.name()).isPresent()) {
            return conflict("layer name already exists");
        }

        try {
            WLayer layer = WLayer.builder()
                    .worldId(lookupWorldId)
                    .name(request.name())
                    .layerType(request.layerType())
                    .mountX(request.mountX())
                    .mountY(request.mountY())
                    .mountZ(request.mountZ())
                    .ground(request.ground() != null ? request.ground() : false)
                    .allChunks(request.allChunks() != null ? request.allChunks() : true)
                    .affectedChunks(request.affectedChunks() != null ? request.affectedChunks() : List.of())
                    .order(request.order() != null ? request.order() : 0)
                    .enabled(request.enabled() != null ? request.enabled() : true)
                    .groups(request.groups() != null ? request.groups() : Map.of())
                    .build();

            layer.touchCreate();

            WLayer saved = layerService.save(layer);

            log.info("Created layer: id={}, name={}, type={}", saved.getId(), saved.getName(), saved.getLayerType());
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("id", saved.getId()));
        } catch (IllegalArgumentException e) {
            log.warn("Validation error creating layer: {}", e.getMessage());
            return bad(e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error creating layer", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Internal server error"));
        }
    }

    /**
     * Update existing Layer.
     * PUT /control/worlds/{worldId}/layers/{id}
     */
    @PutMapping("/{id}")
    @Operation(summary = "Update Layer")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Layer updated"),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "404", description = "Layer not found")
    })
    public ResponseEntity<?> update(
            @Parameter(description = "World identifier") @PathVariable String worldId,
            @Parameter(description = "Layer identifier") @PathVariable String id,
            @RequestBody UpdateLayerRequest request) {

        log.debug("UPDATE layer: worldId={}, id={}", worldId, id);

        WorldId.of(worldId).orElseThrow(
                () -> new IllegalStateException("Invalid worldId: " + worldId)
        );
        var validation = validateId(id, "id");
        if (validation != null) return validation;

        Optional<WLayer> opt = layerService.findById(id);
        if (opt.isEmpty()) {
            log.warn("Layer not found for update: id={}", id);
            return notFound("layer not found");
        }

        WLayer layer = opt.get();
        if (!layer.getWorldId().equals(worldId)) {
            log.warn("Layer worldId mismatch: expected={}, actual={}", worldId, layer.getWorldId());
            return notFound("layer not found");
        }

        // Apply updates
        boolean changed = false;
        if (request.name() != null && !request.name().isBlank()) {
            layer.setName(request.name());
            changed = true;
        }
        if (request.mountX() != null) {
            layer.setMountX(request.mountX());
            changed = true;
        }
        if (request.mountY() != null) {
            layer.setMountY(request.mountY());
            changed = true;
        }
        if (request.mountZ() != null) {
            layer.setMountZ(request.mountZ());
            changed = true;
        }
        if (request.ground() != null) {
            layer.setGround(request.ground().booleanValue());
            changed = true;
        }
        if (request.allChunks() != null) {
            layer.setAllChunks(request.allChunks());
            changed = true;
        }
        if (request.affectedChunks() != null) {
            layer.setAffectedChunks(request.affectedChunks());
            changed = true;
        }
        if (request.order() != null) {
            layer.setOrder(request.order());
            changed = true;
        }
        if (request.enabled() != null) {
            layer.setEnabled(request.enabled());
            changed = true;
        }
        if (request.groups() != null) {
            layer.setGroups(request.groups());
            changed = true;
        }

        if (!changed) {
            return bad("at least one field required for update");
        }

        layer.touchUpdate();
        WLayer updated = layerService.save(layer);

        log.info("Updated layer: id={}, name={}", id, updated.getName());
        return ResponseEntity.ok(toDto(updated));
    }

    /**
     * Delete Layer.
     * DELETE /control/worlds/{worldId}/layers/{id}
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete Layer")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Layer deleted"),
            @ApiResponse(responseCode = "400", description = "Invalid parameters"),
            @ApiResponse(responseCode = "404", description = "Layer not found")
    })
    public ResponseEntity<?> delete(
            @Parameter(description = "World identifier") @PathVariable String worldId,
            @Parameter(description = "Layer identifier") @PathVariable String id) {

        log.debug("DELETE layer: worldId={}, id={}", worldId, id);

        WorldId.of(worldId).orElseThrow(
                () -> new IllegalStateException("Invalid worldId: " + worldId)
        );
        var validation = validateId(id, "id");
        if (validation != null) return validation;

        Optional<WLayer> opt = layerService.findById(id);
        if (opt.isEmpty()) {
            log.warn("Layer not found for deletion: id={}", id);
            return notFound("layer not found");
        }

        WLayer layer = opt.get();
        if (!layer.getWorldId().equals(worldId)) {
            log.warn("Layer worldId mismatch: expected={}, actual={}", worldId, layer.getWorldId());
            return notFound("layer not found");
        }

        layerService.delete(id);

        log.info("Deleted layer: id={}, name={}", id, layer.getName());
        return ResponseEntity.noContent().build();
    }

    // Helper methods

    private LayerDto toDto(WLayer layer) {
        return new LayerDto(
                layer.getId(),
                layer.getWorldId(),
                layer.getName(),
                layer.getLayerType(),
                layer.getLayerDataId(),
                layer.getMountX(),
                layer.getMountY(),
                layer.getMountZ(),
                layer.isGround(),
                layer.isAllChunks(),
                layer.getAffectedChunks(),
                layer.getOrder(),
                layer.isEnabled(),
                layer.getGroups(),
                layer.getCreatedAt(),
                layer.getUpdatedAt()
        );
    }
}
