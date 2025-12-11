package de.mhus.nimbus.world.control.api;

import de.mhus.nimbus.generated.types.Block;
import de.mhus.nimbus.generated.types.EditAction;
import de.mhus.nimbus.shared.engine.EngineMapper;
import de.mhus.nimbus.world.control.commands.CommitLayerCommand;
import de.mhus.nimbus.world.control.service.EditService;
import de.mhus.nimbus.world.control.service.EditState;
import de.mhus.nimbus.world.shared.client.WorldClientService;
import de.mhus.nimbus.world.shared.commands.CommandContext;
import de.mhus.nimbus.world.shared.layer.WLayer;
import de.mhus.nimbus.world.shared.layer.WLayerService;
import de.mhus.nimbus.world.shared.redis.WorldRedisService;
import de.mhus.nimbus.world.shared.session.WSession;
import de.mhus.nimbus.world.shared.session.WSessionService;
import de.mhus.nimbus.world.shared.world.WWorld;
import de.mhus.nimbus.world.shared.world.WWorldService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
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
    private final de.mhus.nimbus.world.shared.overlay.BlockOverlayService blockOverlayService;
    private final de.mhus.nimbus.world.shared.layer.WDirtyChunkService dirtyChunkService;
    private final WorldRedisService redisService;
    private final WSessionService wSessionService;
    private final WorldClientService worldClientService;
    private final CommitLayerCommand commitLayerCommand;
    private final EngineMapper engineMapper;
    private final WWorldService worldService;

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

        // Add marked block coordinates (for copy/move operations) - extracted from block data
        Optional<Block> markedBlock = editService.getMarkedBlockData(worldId, sessionId);
        if (markedBlock.isPresent() && markedBlock.get().getPosition() != null) {
            Map<String, Integer> markedPos = Map.of(
                    "x", (int) markedBlock.get().getPosition().getX(),
                    "y", (int) markedBlock.get().getPosition().getY(),
                    "z", (int) markedBlock.get().getPosition().getZ()
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
            @RequestBody String request) {

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

        try {
            // Validate blockJson
            if (Strings.isEmpty(request)) {
                return bad("blockJson is required");
            }

            // Deserialize block using EngineMapper
            Block block;
            try {
                block = engineMapper.readValue(request, Block.class);
            } catch (Exception e) {
                log.error("Failed to parse blockJson: {}", request, e);
                return bad("Invalid blockJson: " + e.getMessage());
            }

            // Validate block has position
            if (block.getPosition() == null) {
                return bad("Block must have position");
            }

            // Save complete block to Redis overlay
            String blockJson = blockOverlayService.saveBlockOverlay(worldId, sessionId, block);

            if (blockJson == null) {
                return bad("Failed to save block overlay");
            }

            // Send block update to client
            int x = (int) block.getPosition().getX();
            int y = (int) block.getPosition().getY();
            int z = (int) block.getPosition().getZ();

            boolean sent = blockUpdateService.sendBlockUpdate(worldId, sessionId, x, y, z, blockJson, null);
            if (!sent) {
                log.warn("Failed to send block update to client: session={}", sessionId);
            }

            // Mark chunk as dirty for later commit
            WWorld world = worldService.getByWorldId(worldId).orElse(null);
            if (world != null) {
                String chunkKey = world.getChunkKey(x, z);
                dirtyChunkService.markChunkDirty(worldId, chunkKey, "block_overlay_edit");
            } else {
                log.warn("Could not mark chunk dirty - world not found: {}", worldId);
            }

            log.info("Block saved and update sent: session={} layer={} pos=({},{},{}) blockTypeId={}",
                    sessionId, state.getSelectedLayer(), x, y, z, block.getBlockTypeId());

            Map<String, Object> response = new HashMap<>();
            response.put("blockTypeId", block.getBlockTypeId());
            response.put("layer", state.getSelectedLayer());
            response.put("saved", true);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to update block: session={}", sessionId, e);
            return bad("Failed to update block: " + e.getMessage());
        }
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
     * Accepts complete block definition as JSON string from block-editor.
     */
    @Data
    public static class UpdateBlockRequest {
        private String blockJson;  // Complete block definition as JSON string
        private String meta;  // Optional additional metadata
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

    // ===== BLOCK PALETTE SUPPORT =====

    /**
     * GET /api/editor/{worldId}/session/{sessionId}/markedBlock
     * Returns the complete block data for the currently marked block.
     * Reads from Redis overlay where the marked block is stored.
     * Used when adding a marked block to the palette.
     * Returns 200 with null/empty response if no marked block exists.
     */
    @GetMapping("/{worldId}/session/{sessionId}/markedBlock")
    public ResponseEntity<?> getMarkedBlockData(
            @PathVariable String worldId,
            @PathVariable String sessionId) {

        ResponseEntity<?> validation = validateWorldId(worldId);
        if (validation != null) return validation;

        validation = validateId(sessionId, "sessionId");
        if (validation != null) return validation;

        // Get marked block from EditService
        Optional<Block> blockOpt = editService.getMarkedBlockData(worldId, sessionId);
        if (blockOpt.isEmpty()) {
            log.debug("No marked block found: worldId={}, sessionId={}", worldId, sessionId);
            // Return 200 with null to indicate no marked block
            return ResponseEntity.ok()
                    .header("Content-Type", "application/json")
                    .body("null");
        }

        try {
            Block block = blockOpt.get();

            // Serialize to JSON
            String blockJson = engineMapper.writeValueAsString(block);

            log.info("Retrieved marked block data: worldId={}, sessionId={}, blockTypeId={}",
                    worldId, sessionId, block.getBlockTypeId());

            // Return as JSON
            return ResponseEntity.ok()
                    .header("Content-Type", "application/json")
                    .body(blockJson);

        } catch (Exception e) {
            log.error("Failed to serialize marked block: worldId={}, sessionId={}",
                    worldId, sessionId, e);
            return bad("Failed to serialize marked block: " + e.getMessage());
        }
    }

    /**
     * POST /api/editor/{worldId}/session/{sessionId}/markedBlock
     * Sets a block as the current marked block (for palette selection and paste).
     * Stores complete block data in Redis overlay.
     * Position in block.position is optional/ignored - only block content matters.
     */
    @PostMapping("/{worldId}/session/{sessionId}/markedBlock")
    public ResponseEntity<?> setMarkedBlock(
            @PathVariable String worldId,
            @PathVariable String sessionId,
            @RequestBody String blockJson) {

        ResponseEntity<?> validation = validateWorldId(worldId);
        if (validation != null) return validation;

        validation = validateId(sessionId, "sessionId");
        if (validation != null) return validation;

        if (Strings.isEmpty(blockJson)) {
            return bad("Block data is required");
        }

        try {
            // Validate by parsing the JSON
            Block block = engineMapper.readValue(blockJson, Block.class);

            if (block == null || Strings.isBlank(block.getBlockTypeId())) {
                return bad("Invalid block data: blockTypeId is required");
            }

            // Store via EditService
            editService.setMarkedBlockData(worldId, sessionId, blockJson);

            log.info("Marked block set from palette: worldId={}, sessionId={}, blockTypeId={}",
                    worldId, sessionId, block.getBlockTypeId());

            return ResponseEntity.ok(Map.of(
                    "message", "Marked block set successfully",
                    "blockTypeId", block.getBlockTypeId()
            ));

        } catch (Exception e) {
            log.error("Failed to set marked block: worldId={}, sessionId={}", worldId, sessionId, e);
            return bad("Failed to set marked block: " + e.getMessage());
        }
    }
}
