package de.mhus.nimbus.world.control.api;

import de.mhus.nimbus.shared.types.WorldId;
import de.mhus.nimbus.world.shared.dto.CreateLayerModelRequest;
import de.mhus.nimbus.world.shared.dto.LayerModelDto;
import de.mhus.nimbus.world.shared.dto.UpdateLayerModelRequest;
import de.mhus.nimbus.world.shared.layer.LayerBlock;
import de.mhus.nimbus.world.shared.layer.WLayer;
import de.mhus.nimbus.world.shared.layer.WLayerModel;
import de.mhus.nimbus.world.shared.layer.WLayerModelRepository;
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

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * REST Controller for Layer Model CRUD operations.
 * Base path: /control/worlds/{worldId}/layers/{layerId}/models
 * <p>
 * Layer models are entity-oriented storage for MODEL type layers.
 * Multiple models can reference the same layerDataId.
 */
@RestController
@RequestMapping("/control/worlds/{worldId}/layers/{layerId}/models")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Layer Models", description = "Layer model management for MODEL type layers")
public class ELayerModelController extends BaseEditorController {

    private final WLayerService layerService;
    private final WLayerModelRepository modelRepository;

    /**
     * Get single Layer Model by ID.
     * GET /control/worlds/{worldId}/layers/{layerId}/models/{id}
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get Layer Model by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Model found"),
            @ApiResponse(responseCode = "400", description = "Invalid parameters"),
            @ApiResponse(responseCode = "404", description = "Model not found")
    })
    public ResponseEntity<?> get(
            @Parameter(description = "World identifier") @PathVariable String worldId,
            @Parameter(description = "Layer identifier") @PathVariable String layerId,
            @Parameter(description = "Model identifier") @PathVariable String id) {

        log.debug("GET layer model: worldId={}, layerId={}, id={}", worldId, layerId, id);

        WorldId.of(worldId).orElseThrow(
                () -> new IllegalStateException("Invalid worldId: " + worldId)
        );
        var validation = validateId(layerId, "layerId");
        if (validation != null) return validation;
        validation = validateId(id, "id");
        if (validation != null) return validation;

        // Verify layer exists and belongs to world
        Optional<WLayer> layerOpt = layerService.findById(layerId);
        if (layerOpt.isEmpty() || !layerOpt.get().getWorldId().equals(worldId)) {
            log.warn("Layer not found: layerId={}", layerId);
            return notFound("layer not found");
        }

        Optional<WLayerModel> opt = modelRepository.findById(id);
        if (opt.isEmpty()) {
            log.warn("Model not found: id={}", id);
            return notFound("model not found");
        }

        WLayerModel model = opt.get();
        if (!model.getWorldId().equals(worldId)) {
            log.warn("Model worldId mismatch: expected={}, actual={}", worldId, model.getWorldId());
            return notFound("model not found");
        }

        log.debug("Returning model: id={}", id);
        return ResponseEntity.ok(toDto(model));
    }

    /**
     * List all Layer Models for a layer.
     * GET /control/worlds/{worldId}/layers/{layerId}/models
     */
    @GetMapping
    @Operation(summary = "List all Layer Models for a layer")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Success"),
            @ApiResponse(responseCode = "400", description = "Invalid parameters"),
            @ApiResponse(responseCode = "404", description = "Layer not found")
    })
    public ResponseEntity<?> list(
            @Parameter(description = "World identifier") @PathVariable String worldId,
            @Parameter(description = "Layer identifier") @PathVariable String layerId) {

        log.debug("LIST layer models: worldId={}, layerId={}", worldId, layerId);

        var wid = WorldId.of(worldId).orElseThrow(
                () -> new IllegalStateException("Invalid worldId: " + worldId)
        );
        var validation = validateId(layerId, "layerId");
        if (validation != null) return validation;

        // Verify layer exists and belongs to world
        Optional<WLayer> layerOpt = layerService.findById(layerId);
        if (layerOpt.isEmpty()) {
            log.warn("Layer not found: layerId={}", layerId);
            return notFound("layer not found");
        }

        WLayer layer = layerOpt.get();
        String lookupWorldId = wid.withoutInstance().getId();
        if (!layer.getWorldId().equals(lookupWorldId)) {
            log.warn("Layer worldId mismatch: expected={}, actual={}", lookupWorldId, layer.getWorldId());
            return notFound("layer not found");
        }

        // Get layerDataId from layer
        String layerDataId = layer.getLayerDataId();
        if (layerDataId == null) {
            log.warn("Layer has no layerDataId: layerId={}", layerId);
            return ResponseEntity.ok(Map.of(
                    "models", List.of(),
                    "count", 0
            ));
        }

        // Get all models for this layerDataId (sorted by order)
        List<WLayerModel> allModels = modelRepository.findByLayerDataIdOrderByOrder(layerDataId);
        log.debug("Found {} models for layerDataId={}", allModels.size(), layerDataId);

        // Convert to DTOs
        List<LayerModelDto> models = allModels.stream()
                .map(this::toDto)
                .collect(Collectors.toList());

        log.debug("Returning {} models for layerDataId={}", models.size(), layerDataId);

        return ResponseEntity.ok(Map.of(
                "models", models,
                "count", models.size()
        ));
    }

    /**
     * Create new Layer Model.
     * POST /control/worlds/{worldId}/layers/{layerId}/models
     */
    @PostMapping
    @Operation(summary = "Create new Layer Model")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Model created"),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "404", description = "Layer not found")
    })
    public ResponseEntity<?> create(
            @Parameter(description = "World identifier") @PathVariable String worldId,
            @Parameter(description = "Layer identifier") @PathVariable String layerId,
            @RequestBody CreateLayerModelRequest request) {

        log.debug("CREATE layer model: worldId={}, layerId={}", worldId, layerId);

        var wid = WorldId.of(worldId).orElseThrow(
                () -> new IllegalStateException("Invalid worldId: " + worldId)
        );
        var validation = validateId(layerId, "layerId");
        if (validation != null) return validation;

        // Verify layer exists and belongs to world
        Optional<WLayer> layerOpt = layerService.findById(layerId);
        if (layerOpt.isEmpty()) {
            log.warn("Layer not found: layerId={}", layerId);
            return notFound("layer not found");
        }

        WLayer layer = layerOpt.get();
        String lookupWorldId = wid.withoutInstance().getId();
        if (!layer.getWorldId().equals(lookupWorldId)) {
            log.warn("Layer worldId mismatch: expected={}, actual={}", lookupWorldId, layer.getWorldId());
            return notFound("layer not found");
        }

        // Get layerDataId from layer
        String layerDataId = layer.getLayerDataId();
        if (layerDataId == null) {
            log.error("Layer has no layerDataId: layerId={}", layerId);
            return bad("layer has no layerDataId");
        }

        try {
            WLayerModel model = WLayerModel.builder()
                    .worldId(lookupWorldId)
                    .name(request.name())
                    .title(request.title())
                    .layerDataId(layerDataId)
                    .mountX(request.mountX() != null ? request.mountX() : 0)
                    .mountY(request.mountY() != null ? request.mountY() : 0)
                    .mountZ(request.mountZ() != null ? request.mountZ() : 0)
                    .rotation(request.rotation() != null ? request.rotation() : 0)
                    .referenceModelId(request.referenceModelId())
                    .order(request.order() != null ? request.order() : 100)
                    .content(List.of())
                    .groups(request.groups() != null ? request.groups() : Map.of())
                    .build();

            model.touchCreate();

            WLayerModel saved = modelRepository.save(model);

            // Transfer model to terrain if layer type is MODEL
            if (layer.getLayerType() == de.mhus.nimbus.world.shared.layer.LayerType.MODEL) {
                int chunksAffected = layerService.transferModelToTerrain(saved.getId(), true);
                log.info("Transferred model to terrain: modelId={}, chunks={}", saved.getId(), chunksAffected);
            }

            log.info("Created layer model: id={}, layerDataId={}, worldId={}, mountX={}, mountY={}, mountZ={}",
                    saved.getId(), saved.getLayerDataId(), saved.getWorldId(),
                    saved.getMountX(), saved.getMountY(), saved.getMountZ());
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("id", saved.getId()));
        } catch (IllegalArgumentException e) {
            log.warn("Validation error creating model: {}", e.getMessage());
            return bad(e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error creating model", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Internal server error"));
        }
    }

    /**
     * Update existing Layer Model.
     * PUT /control/worlds/{worldId}/layers/{layerId}/models/{id}
     */
    @PutMapping("/{id}")
    @Operation(summary = "Update Layer Model")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Model updated"),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "404", description = "Model not found")
    })
    public ResponseEntity<?> update(
            @Parameter(description = "World identifier") @PathVariable String worldId,
            @Parameter(description = "Layer identifier") @PathVariable String layerId,
            @Parameter(description = "Model identifier") @PathVariable String id,
            @RequestBody UpdateLayerModelRequest request) {

        log.debug("UPDATE layer model: worldId={}, layerId={}, id={}", worldId, layerId, id);

        WorldId.of(worldId).orElseThrow(
                () -> new IllegalStateException("Invalid worldId: " + worldId)
        );
        var validation = validateId(layerId, "layerId");
        if (validation != null) return validation;
        validation = validateId(id, "id");
        if (validation != null) return validation;

        // Verify layer exists
        Optional<WLayer> layerOpt = layerService.findById(layerId);
        if (layerOpt.isEmpty() || !layerOpt.get().getWorldId().equals(worldId)) {
            log.warn("Layer not found: layerId={}", layerId);
            return notFound("layer not found");
        }

        Optional<WLayerModel> opt = modelRepository.findById(id);
        if (opt.isEmpty()) {
            log.warn("Model not found for update: id={}", id);
            return notFound("model not found");
        }

        WLayerModel model = opt.get();
        if (!model.getWorldId().equals(worldId)) {
            log.warn("Model worldId mismatch: expected={}, actual={}", worldId, model.getWorldId());
            return notFound("model not found");
        }

        // Apply updates
        boolean changed = false;
        if (request.name() != null) {
            model.setName(request.name());
            changed = true;
        }
        if (request.title() != null) {
            model.setTitle(request.title());
            changed = true;
        }
        if (request.mountX() != null) {
            model.setMountX(request.mountX());
            changed = true;
        }
        if (request.mountY() != null) {
            model.setMountY(request.mountY());
            changed = true;
        }
        if (request.mountZ() != null) {
            model.setMountZ(request.mountZ());
            changed = true;
        }
        if (request.rotation() != null) {
            model.setRotation(request.rotation());
            changed = true;
        }
        if (request.referenceModelId() != null) {
            model.setReferenceModelId(request.referenceModelId());
            changed = true;
        }
        if (request.order() != null) {
            model.setOrder(request.order());
            changed = true;
        }
        if (request.groups() != null) {
            model.setGroups(request.groups());
            changed = true;
        }

        if (!changed) {
            return bad("at least one field required for update");
        }

        model.touchUpdate();
        WLayerModel updated = modelRepository.save(model);

        // Transfer model to terrain if layer type is MODEL
        WLayer layer = layerOpt.get();
        if (layer.getLayerType() == de.mhus.nimbus.world.shared.layer.LayerType.MODEL) {
            int chunksAffected = layerService.transferModelToTerrain(updated.getId(), true);
            log.info("Transferred updated model to terrain: modelId={}, chunks={}", updated.getId(), chunksAffected);
        }

        log.info("Updated layer model: id={}", id);
        return ResponseEntity.ok(toDto(updated));
    }

    /**
     * Sync Layer Model to Terrain.
     * Manually triggers transfer of model data to terrain layer and marks chunks as dirty.
     * POST /control/worlds/{worldId}/layers/{layerId}/models/{id}/sync
     */
    @PostMapping("/{id}/sync")
    @Operation(summary = "Sync Layer Model to Terrain")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Model synced successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid parameters"),
            @ApiResponse(responseCode = "404", description = "Model not found")
    })
    public ResponseEntity<?> syncToTerrain(
            @Parameter(description = "World identifier") @PathVariable String worldId,
            @Parameter(description = "Layer identifier") @PathVariable String layerId,
            @Parameter(description = "Model identifier") @PathVariable String id) {

        log.debug("SYNC layer model to terrain: worldId={}, layerId={}, id={}", worldId, layerId, id);

        WorldId.of(worldId).orElseThrow(
                () -> new IllegalStateException("Invalid worldId: " + worldId)
        );
        var validation = validateId(layerId, "layerId");
        if (validation != null) return validation;
        validation = validateId(id, "id");
        if (validation != null) return validation;

        // Verify layer exists and belongs to world
        Optional<WLayer> layerOpt = layerService.findById(layerId);
        if (layerOpt.isEmpty()) {
            log.warn("Layer not found: layerId={}", layerId);
            return notFound("layer not found");
        }

        WLayer layer = layerOpt.get();
        String lookupWorldId = WorldId.of(worldId).orElseThrow().withoutInstance().getId();
        if (!layer.getWorldId().equals(lookupWorldId)) {
            log.warn("Layer worldId mismatch: expected={}, actual={}", lookupWorldId, layer.getWorldId());
            return notFound("layer not found");
        }

        // Verify model exists
        Optional<WLayerModel> modelOpt = modelRepository.findById(id);
        if (modelOpt.isEmpty()) {
            log.warn("Model not found for sync: id={}", id);
            return notFound("model not found");
        }

        WLayerModel model = modelOpt.get();
        if (!model.getWorldId().equals(lookupWorldId)) {
            log.warn("Model worldId mismatch: expected={}, actual={}", lookupWorldId, model.getWorldId());
            return notFound("model not found");
        }

        // Verify layer type is MODEL
        if (layer.getLayerType() != de.mhus.nimbus.world.shared.layer.LayerType.MODEL) {
            log.warn("Layer is not MODEL type: layerId={} type={}", layerId, layer.getLayerType());
            return bad("layer is not MODEL type");
        }

        try {
            // Transfer model to terrain with dirty chunk marking
            int chunksAffected = layerService.transferModelToTerrain(id, true);
            log.info("Manually synced model to terrain: modelId={}, chunks={}", id, chunksAffected);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "chunksAffected", chunksAffected,
                    "message", "Model synced to terrain successfully"
            ));
        } catch (Exception e) {
            log.error("Failed to sync model to terrain: modelId={}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to sync model: " + e.getMessage()));
        }
    }

    /**
     * Delete Layer Model.
     * DELETE /control/worlds/{worldId}/layers/{layerId}/models/{id}
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete Layer Model")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Model deleted"),
            @ApiResponse(responseCode = "400", description = "Invalid parameters"),
            @ApiResponse(responseCode = "404", description = "Model not found")
    })
    public ResponseEntity<?> delete(
            @Parameter(description = "World identifier") @PathVariable String worldId,
            @Parameter(description = "Layer identifier") @PathVariable String layerId,
            @Parameter(description = "Model identifier") @PathVariable String id) {

        log.debug("DELETE layer model: worldId={}, layerId={}, id={}", worldId, layerId, id);

        WorldId.of(worldId).orElseThrow(
                () -> new IllegalStateException("Invalid worldId: " + worldId)
        );
        var validation = validateId(layerId, "layerId");
        if (validation != null) return validation;
        validation = validateId(id, "id");
        if (validation != null) return validation;

        // Verify layer exists
        Optional<WLayer> layerOpt = layerService.findById(layerId);
        if (layerOpt.isEmpty() || !layerOpt.get().getWorldId().equals(worldId)) {
            log.warn("Layer not found: layerId={}", layerId);
            return notFound("layer not found");
        }

        Optional<WLayerModel> opt = modelRepository.findById(id);
        if (opt.isEmpty()) {
            log.warn("Model not found for deletion: id={}", id);
            return notFound("model not found");
        }

        WLayerModel model = opt.get();
        if (!model.getWorldId().equals(worldId)) {
            log.warn("Model worldId mismatch: expected={}, actual={}", worldId, model.getWorldId());
            return notFound("model not found");
        }

        modelRepository.delete(model);

        log.info("Deleted layer model: id={}", id);
        return ResponseEntity.noContent().build();
    }

    // Helper methods

    private LayerModelDto toDto(WLayerModel model) {
        return new LayerModelDto(
                model.getId(),
                model.getWorldId(),
                model.getName(),
                model.getTitle(),
                model.getLayerDataId(),
                model.getMountX(),
                model.getMountY(),
                model.getMountZ(),
                model.getRotation(),
                model.getReferenceModelId(),
                model.getOrder(),
                model.getGroups(),
                model.getCreatedAt(),
                model.getUpdatedAt()
        );
    }
}
