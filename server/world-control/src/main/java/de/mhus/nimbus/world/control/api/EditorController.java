package de.mhus.nimbus.world.control.api;

import de.mhus.nimbus.world.control.commands.CommitLayerCommand;
import de.mhus.nimbus.world.control.service.EditService;
import de.mhus.nimbus.world.control.service.EditState;
import de.mhus.nimbus.world.shared.client.WorldClientService;
import de.mhus.nimbus.world.shared.commands.CommandContext;
import de.mhus.nimbus.world.shared.layer.EditAction;
import de.mhus.nimbus.world.shared.layer.WLayer;
import de.mhus.nimbus.world.shared.layer.WLayerService;
import de.mhus.nimbus.world.shared.redis.WorldRedisService;
import de.mhus.nimbus.world.shared.session.WSession;
import de.mhus.nimbus.world.shared.session.WSessionService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Editor REST API controller.
 * Manages edit mode configuration and layer selection.
 */
@RestController
@RequestMapping("/api/editor")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Editor", description = "Edit mode and layer editing")
public class EditorController extends BaseEditorController {

    private final EditService editService;
    private final WLayerService layerService;
    private final de.mhus.nimbus.world.control.service.BlockUpdateService blockUpdateService;
    private final de.mhus.nimbus.world.control.service.BlockOverlayService blockOverlayService;
    private final de.mhus.nimbus.world.shared.layer.WDirtyChunkService dirtyChunkService;
    private final WorldRedisService redisService;
    private final WSessionService wSessionService;
    private final WorldClientService worldClientService;
    private final CommitLayerCommand commitLayerCommand;

    // ===== EDIT STATE =====

    /**
     * GET /api/editor/{worldId}/session/{sessionId}/edit
     * Returns full edit state including selected block.
     */
    @GetMapping("/{worldId}/session/{sessionId}/edit")
    public ResponseEntity<?> getEditState(
            @PathVariable String worldId,
            @PathVariable String sessionId) {

        ResponseEntity<?> validation = validateWorldId(worldId);
        if (validation != null) return validation;

        validation = validateId(sessionId, "sessionId");
        if (validation != null) return validation;

        EditState state = editService.getEditState(worldId, sessionId);
        Optional<EditService.BlockPosition> selectedBlock = editService.getSelectedBlock(worldId, sessionId);

        Map<String, Object> response = new HashMap<>();
        response.put("editMode", state.isEditMode());
        response.put("editAction", state.getEditAction() != null ? state.getEditAction().name() : "OPEN_CONFIG_DIALOG");
        response.put("selectedLayer", state.getSelectedLayer());
        response.put("mountX", state.getMountX() != null ? state.getMountX() : 0);
        response.put("mountY", state.getMountY() != null ? state.getMountY() : 0);
        response.put("mountZ", state.getMountZ() != null ? state.getMountZ() : 0);
        response.put("selectedGroup", state.getSelectedGroup());

        // Add selected block coordinates
        if (selectedBlock.isPresent()) {
            Map<String, Integer> blockPos = Map.of(
                    "x", selectedBlock.get().x(),
                    "y", selectedBlock.get().y(),
                    "z", selectedBlock.get().z()
            );
            response.put("selectedBlock", blockPos);
        } else {
            response.put("selectedBlock", null);
        }

        // Add marked block coordinates (for copy/move operations)
        Optional<EditService.BlockPosition> markedBlock = editService.getMarkedBlock(worldId, sessionId);
        if (markedBlock.isPresent()) {
            Map<String, Integer> markedPos = Map.of(
                    "x", markedBlock.get().x(),
                    "y", markedBlock.get().y(),
                    "z", markedBlock.get().z()
            );
            response.put("markedBlock", markedPos);
        } else {
            response.put("markedBlock", null);
        }

        return ResponseEntity.ok(response);
    }

    /**
     * PUT /api/editor/{worldId}/session/{sessionId}/edit
     * Update edit state (partial updates supported).
     */
    @PutMapping("/{worldId}/session/{sessionId}/edit")
    public ResponseEntity<?> updateEditState(
            @PathVariable String worldId,
            @PathVariable String sessionId,
            @RequestBody EditStateUpdateRequest request) {

        ResponseEntity<?> validation = validateWorldId(worldId);
        if (validation != null) return validation;

        validation = validateId(sessionId, "sessionId");
        if (validation != null) return validation;

        EditState updated = editService.updateEditState(worldId, sessionId, state -> {
            if (request.editMode != null) {
                state.setEditMode(request.editMode);
            }
            if (request.editAction != null) {
                try {
                    EditAction action = EditAction.valueOf(request.editAction);
                    state.setEditAction(action);
                } catch (IllegalArgumentException e) {
                    log.warn("Invalid edit action: {}", request.editAction);
                }
            }
            if (request.selectedLayer != null) {
                state.setSelectedLayer(request.selectedLayer);
            }
            if (request.mountX != null) {
                state.setMountX(request.mountX);
            }
            if (request.mountY != null) {
                state.setMountY(request.mountY);
            }
            if (request.mountZ != null) {
                state.setMountZ(request.mountZ);
            }
            if (request.selectedGroup != null) {
                state.setSelectedGroup(request.selectedGroup);
            }
        });

        Map<String, Object> response = new HashMap<>();
        response.put("editMode", updated.isEditMode());
        response.put("editAction", updated.getEditAction() != null ? updated.getEditAction().name() : "OPEN_CONFIG_DIALOG");
        response.put("selectedLayer", updated.getSelectedLayer());
        response.put("mountX", updated.getMountX() != null ? updated.getMountX() : 0);
        response.put("mountY", updated.getMountY() != null ? updated.getMountY() : 0);
        response.put("mountZ", updated.getMountZ() != null ? updated.getMountZ() : 0);
        response.put("selectedGroup", updated.getSelectedGroup());

        return ResponseEntity.ok(response);
    }

    // ===== LAYERS =====

    /**
     * GET /api/editor/{worldId}/layers
     * List all layers for selection dropdown.
     */
    @GetMapping("/{worldId}/layers")
    public ResponseEntity<?> listLayers(@PathVariable String worldId) {

        ResponseEntity<?> validation = validateWorldId(worldId);
        if (validation != null) return validation;

        List<WLayer> layers = layerService.findLayersByWorld(worldId);

        List<Map<String, Object>> dtos = layers.stream()
                .map(layer -> {
                    Map<String, Object> dto = new HashMap<>();
                    dto.put("name", layer.getName());
                    dto.put("layerType", layer.getLayerType().name());
                    dto.put("enabled", layer.isEnabled());
                    dto.put("order", layer.getOrder());
                    dto.put("mountX", layer.getMountX() != null ? layer.getMountX() : 0);
                    dto.put("mountY", layer.getMountY() != null ? layer.getMountY() : 0);
                    dto.put("mountZ", layer.getMountZ() != null ? layer.getMountZ() : 0);
                    dto.put("groups", layer.getGroups());
                    return dto;
                })
                .toList();

        return ResponseEntity.ok(Map.of("layers", dtos));
    }

    /**
     * POST /api/editor/{worldId}/layers
     * Create a new layer.
     */
    @PostMapping("/{worldId}/layers")
    public ResponseEntity<?> createLayer(
            @PathVariable String worldId,
            @RequestBody CreateLayerRequest request) {

        ResponseEntity<?> validation = validateWorldId(worldId);
        if (validation != null) return validation;

        validation = validateId(request.name, "name");
        if (validation != null) return validation;

        // Check if layer already exists
        Optional<WLayer> existing = layerService.findLayer(worldId, request.name);
        if (existing.isPresent()) {
            return conflict("Layer with name '" + request.name + "' already exists");
        }

        // Create new layer
        de.mhus.nimbus.world.shared.layer.LayerType layerType = request.layerType != null
            ? request.layerType
            : de.mhus.nimbus.world.shared.layer.LayerType.TERRAIN;
        int order = request.order != null ? request.order : 10;
        boolean allChunks = request.allChunks != null ? request.allChunks : false;

        WLayer saved = layerService.createLayer(worldId, request.name, layerType, order, allChunks, List.of());

        // Set mount points if MODEL layer
        if (layerType == de.mhus.nimbus.world.shared.layer.LayerType.MODEL &&
            (request.mountX != null || request.mountY != null || request.mountZ != null)) {

            layerService.updateLayer(worldId, request.name, layer -> {
                if (request.mountX != null) layer.setMountX(request.mountX);
                if (request.mountY != null) layer.setMountY(request.mountY);
                if (request.mountZ != null) layer.setMountZ(request.mountZ);
            });
        }

        Map<String, Object> response = new HashMap<>();
        response.put("name", saved.getName());
        response.put("layerType", saved.getLayerType().name());
        response.put("enabled", saved.isEnabled());
        response.put("order", saved.getOrder());

        return ResponseEntity.ok(response);
    }

    /**
     * DELETE /api/editor/{worldId}/layers/{layerName}
     * Delete a layer and all its data.
     */
    @DeleteMapping("/{worldId}/layers/{layerName}")
    public ResponseEntity<?> deleteLayer(
            @PathVariable String worldId,
            @PathVariable String layerName) {

        ResponseEntity<?> validation = validateWorldId(worldId);
        if (validation != null) return validation;

        validation = validateId(layerName, "layerName");
        if (validation != null) return validation;

        // Check if layer exists
        Optional<WLayer> layer = layerService.findLayer(worldId, layerName);
        if (layer.isEmpty()) {
            return notFound("Layer not found: " + layerName);
        }

        // Delete layer and associated data
        layerService.deleteLayer(worldId, layerName);

        return ResponseEntity.ok(Map.of("message", "Layer deleted successfully"));
    }

    // ===== BLOCK EDITOR =====

    /**
     * PUT /api/editor/{worldId}/session/{sessionId}/block
     * Update a block at the selected position and trigger "b.u" to client.
     */
    @PutMapping("/{worldId}/session/{sessionId}/block")
    public ResponseEntity<?> updateBlock(
            @PathVariable String worldId,
            @PathVariable String sessionId,
            @RequestBody UpdateBlockRequest request) {

        ResponseEntity<?> validation = validateWorldId(worldId);
        if (validation != null) return validation;

        validation = validateId(sessionId, "sessionId");
        if (validation != null) return validation;

        // Get edit state
        EditState state = editService.getEditState(worldId, sessionId);

        // Validate edit mode and layer selection
        if (!state.isEditMode()) {
            return bad("Edit mode not enabled for this session");
        }

        if (state.getSelectedLayer() == null || state.getSelectedLayer().isBlank()) {
            return bad("No layer selected - cannot save block without layer selection");
        }

        // Get selected block position
        Optional<EditService.BlockPosition> positionOpt = editService.getSelectedBlock(worldId, sessionId);
        if (positionOpt.isEmpty()) {
            return bad("No block selected");
        }

        EditService.BlockPosition position = positionOpt.get();

        try {
            // Save block to Redis overlay
            boolean saved = blockOverlayService.saveBlockOverlay(
                    worldId,
                    sessionId,
                    position.x(),
                    position.y(),
                    position.z(),
                    request.blockId,
                    request.meta
            );

            if (!saved) {
                return error("Failed to save block overlay");
            }

            // Mark chunk as dirty for later commit
            String chunkKey = calculateChunkKey(position.x(), position.z());
            dirtyChunkService.markChunkDirty(worldId, chunkKey, "block_overlay_edit");

            log.info("Block saved to overlay: session={} layer={} pos=({},{},{}) blockId={}",
                    sessionId, state.getSelectedLayer(), position.x(), position.y(), position.z(), request.blockId);

            Map<String, Object> response = new HashMap<>();
            response.put("x", position.x());
            response.put("y", position.y());
            response.put("z", position.z());
            response.put("blockId", request.blockId);
            response.put("meta", request.meta);
            response.put("layer", state.getSelectedLayer());
            response.put("saved", true);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to update block: session={} pos=({},{},{})", sessionId, position.x(), position.y(), position.z(), e);
            return error("Failed to update block: " + e.getMessage());
        }
    }

    /**
     * Calculate chunk key from world coordinates.
     */
    private String calculateChunkKey(int x, int z) {
        int cx = x >> 4;
        int cz = z >> 4;
        return cx + ":" + cz;
    }

    private ResponseEntity<?> error(String message) {
        return ResponseEntity.status(500).body(Map.of("error", message));
    }

    // ===== DTOs =====

    /**
     * Request DTO for edit state updates.
     * All fields are optional (partial updates).
     */
    @Data
    public static class EditStateUpdateRequest {
        private Boolean editMode;
        private String editAction;
        private String selectedLayer;
        private Integer mountX;
        private Integer mountY;
        private Integer mountZ;
        private Integer selectedGroup;
    }

    /**
     * Request DTO for creating a new layer.
     */
    @Data
    public static class CreateLayerRequest {
        private String name;
        private de.mhus.nimbus.world.shared.layer.LayerType layerType;
        private Integer order;
        private Integer mountX;
        private Integer mountY;
        private Integer mountZ;
        private Boolean enabled;
        private Boolean allChunks;
    }

    /**
     * Request DTO for block updates.
     */
    @Data
    public static class UpdateBlockRequest {
        private String blockId;
        private String meta;
    }

    // ===== EDIT MODE CONTROL =====

    /**
     * POST /api/editor/{worldId}/session/{sessionId}/activate
     * Activates edit mode for the session.
     */
    @PostMapping("/{worldId}/session/{sessionId}/activate")
    public ResponseEntity<?> activateEditMode(
            @PathVariable String worldId,
            @PathVariable String sessionId) {

        ResponseEntity<?> validation = validateWorldId(worldId);
        if (validation != null) return validation;

        try {
            // 1. Validate: selectedLayer must be set
            EditState state = editService.getEditState(worldId, sessionId);
            if (state.getSelectedLayer() == null) {
                return bad("No layer selected. Please select a layer before activating edit mode.");
            }

            // 2. Update Redis: editMode=true
            editService.setEditMode(worldId, sessionId, true);

            // 3. Get playerUrl from WSessionService
            Optional<WSession> session = wSessionService.getWithPlayerUrl(sessionId);
            if (session.isEmpty() || session.get().getPlayerUrl() == null) {
                return bad("Player URL not available. Session may not be connected.");
            }

            String playerUrl = session.get().getPlayerUrl();

            // 4. Send "edit" command to world-player
            CommandContext ctx = CommandContext.builder()
                    .worldId(worldId)
                    .sessionId(sessionId)
                    .originServer("world-control")
                    .build();

            try {
                worldClientService.sendPlayerCommand(
                        worldId,
                        sessionId,
                        playerUrl,
                        "edit",
                        List.of("true"),
                        ctx
                );
            } catch (Exception e) {
                log.warn("Failed to send edit command to player: {}", e.getMessage());
                // Continue anyway - Redis state is updated
            }

            log.info("Edit mode activated: worldId={}, sessionId={}, layer={}",
                    worldId, sessionId, state.getSelectedLayer());

            // 5. Return success
            return ResponseEntity.ok().body(Map.of(
                    "editMode", true,
                    "layer", state.getSelectedLayer(),
                    "message", "Edit mode activated"
            ));

        } catch (Exception e) {
            log.error("Failed to activate edit mode: worldId={}, sessionId={}", worldId, sessionId, e);
            return bad("Failed to activate edit mode: " + e.getMessage());
        }
    }

    /**
     * POST /api/editor/{worldId}/session/{sessionId}/discard
     * Discards all overlays and deactivates edit mode.
     */
    @PostMapping("/{worldId}/session/{sessionId}/discard")
    public ResponseEntity<?> discardOverlays(
            @PathVariable String worldId,
            @PathVariable String sessionId) {

        ResponseEntity<?> validation = validateWorldId(worldId);
        if (validation != null) return validation;

        try {
            // 1. Validate: editMode must be active
            EditState state = editService.getEditState(worldId, sessionId);
            if (!state.isEditMode()) {
                return bad("Edit mode not active");
            }

            // 2. Delete all overlays from Redis
            long deleted = redisService.deleteAllOverlays(worldId, sessionId);

            log.info("Deleted {} overlay keys for session {}", deleted, sessionId);

            // 3. Disable edit mode in Redis
            editService.setEditMode(worldId, sessionId, false);

            // 4. Send "edit false" command to world-player
            Optional<WSession> session = wSessionService.getWithPlayerUrl(sessionId);
            if (session.isPresent() && session.get().getPlayerUrl() != null) {
                CommandContext ctx = CommandContext.builder()
                        .worldId(worldId)
                        .sessionId(sessionId)
                        .originServer("world-control")
                        .build();

                try {
                    worldClientService.sendPlayerCommand(
                            worldId,
                            sessionId,
                            session.get().getPlayerUrl(),
                            "edit",
                            List.of("false"),
                            ctx
                    );
                } catch (Exception e) {
                    log.warn("Failed to send edit disable command to player: {}", e.getMessage());
                }
            }

            log.info("Edit mode discarded: worldId={}, sessionId={}, deleted={}",
                    worldId, sessionId, deleted);

            // 5. Return count
            return ResponseEntity.ok().body(Map.of(
                    "deleted", deleted,
                    "editMode", false,
                    "message", "Discarded " + deleted + " overlay blocks"
            ));

        } catch (Exception e) {
            log.error("Failed to discard overlays: worldId={}, sessionId={}", worldId, sessionId, e);
            return bad("Failed to discard overlays: " + e.getMessage());
        }
    }

    /**
     * POST /api/editor/{worldId}/session/{sessionId}/save
     * Saves overlays to the selected layer (fire-and-forget).
     */
    @PostMapping("/{worldId}/session/{sessionId}/save")
    public ResponseEntity<?> saveOverlays(
            @PathVariable String worldId,
            @PathVariable String sessionId) {

        ResponseEntity<?> validation = validateWorldId(worldId);
        if (validation != null) return validation;

        try {
            // 1. Validate
            EditState state = editService.getEditState(worldId, sessionId);
            if (!state.isEditMode()) {
                return bad("Edit mode not active");
            }
            if (state.getSelectedLayer() == null) {
                return bad("No layer selected");
            }

            // 2. Fire-and-forget: Async commit
            CompletableFuture.runAsync(() -> {
                try {
                    CommandContext ctx = CommandContext.builder()
                            .worldId(worldId)
                            .sessionId(sessionId)
                            .originServer("world-control")
                            .build();

                    commitLayerCommand.execute(ctx, List.of());
                    log.info("Layer commit completed: worldId={}, sessionId={}", worldId, sessionId);
                } catch (Exception e) {
                    log.error("Layer commit failed: worldId={}, sessionId={}", worldId, sessionId, e);
                }
            });

            log.info("Layer save started: worldId={}, sessionId={}, layer={}",
                    worldId, sessionId, state.getSelectedLayer());

            // 3. Return 202 Accepted immediately
            return ResponseEntity.accepted()
                    .body(Map.of(
                            "message", "Save operation started",
                            "layer", state.getSelectedLayer(),
                            "editMode", true
                    ));

        } catch (Exception e) {
            log.error("Failed to start save operation: worldId={}, sessionId={}", worldId, sessionId, e);
            return bad("Failed to start save operation: " + e.getMessage());
        }
    }
}
