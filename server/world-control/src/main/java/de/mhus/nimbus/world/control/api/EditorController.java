package de.mhus.nimbus.world.control.api;

import de.mhus.nimbus.world.control.service.EditService;
import de.mhus.nimbus.world.control.service.EditState;
import de.mhus.nimbus.world.shared.layer.EditAction;
import de.mhus.nimbus.world.shared.layer.WLayer;
import de.mhus.nimbus.world.shared.layer.WLayerService;
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
}
