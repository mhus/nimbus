package de.mhus.nimbus.world.control.service;

import de.mhus.nimbus.world.shared.layer.EditAction;
import de.mhus.nimbus.world.shared.layer.WLayer;
import de.mhus.nimbus.world.shared.layer.WLayerService;
import de.mhus.nimbus.world.shared.redis.WorldRedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Edit state management service.
 * Stores edit configuration in Redis for cross-pod sharing.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EditService {

    private final WorldRedisService redisService;
    private final WLayerService layerService;

    private static final Duration EDIT_STATE_TTL = Duration.ofHours(24);
    private static final String EDIT_STATE_PREFIX = "edit:";

    /**
     * Get edit state for session.
     * Returns default state if not found in Redis.
     */
    @Transactional(readOnly = true)
    public EditState getEditState(String worldId, String sessionId) {
        String key = editStateKey(sessionId);

        // Load all fields from Redis
        String editModeStr = redisService.getValue(worldId, key + "editMode").orElse(null);
        String editActionStr = redisService.getValue(worldId, key + "editAction").orElse(null);
        String selectedLayer = redisService.getValue(worldId, key + "selectedLayer").orElse(null);
        String mountXStr = redisService.getValue(worldId, key + "mountX").orElse(null);
        String mountYStr = redisService.getValue(worldId, key + "mountY").orElse(null);
        String mountZStr = redisService.getValue(worldId, key + "mountZ").orElse(null);
        String selectedGroupStr = redisService.getValue(worldId, key + "selectedGroup").orElse(null);
        String selectedBlockX = redisService.getValue(worldId, key + "selectedBlockX").orElse(null);
        String selectedBlockY = redisService.getValue(worldId, key + "selectedBlockY").orElse(null);
        String selectedBlockZ = redisService.getValue(worldId, key + "selectedBlockZ").orElse(null);
        String playerIp = redisService.getValue(worldId, key + "playerIp").orElse(null);

        // Build state from Redis values
        EditState.EditStateBuilder builder = EditState.builder()
                .worldId(worldId)
                .editMode(parseBoolean(editModeStr, false))
                .editAction(parseEditAction(editActionStr))
                .selectedLayer(selectedLayer)
                .mountX(parseInt(mountXStr))
                .mountY(parseInt(mountYStr))
                .mountZ(parseInt(mountZStr))
                .selectedGroup(parseInt(selectedGroupStr) != null ? parseInt(selectedGroupStr) : 0)
                .playerIp(playerIp)
                .lastUpdated(Instant.now());

        return builder.build();
    }

    /**
     * Update edit state using Consumer pattern.
     */
    @Transactional
    public EditState updateEditState(String worldId, String sessionId, Consumer<EditState> updater) {
        EditState state = getEditState(worldId, sessionId);
        state.setWorldId(worldId);
        updater.accept(state);
        state.setLastUpdated(Instant.now());

        saveEditState(worldId, sessionId, state);
        return state;
    }

    /**
     * Enable/disable edit mode.
     */
    @Transactional
    public void setEditMode(String worldId, String sessionId, boolean enabled) {
        String key = editStateKey(sessionId);
        redisService.putValue(worldId, key + "editMode", String.valueOf(enabled), EDIT_STATE_TTL);
        log.debug("Edit mode {}: session={}", enabled ? "enabled" : "disabled", sessionId);
    }

    /**
     * Update selected block coordinates.
     */
    @Transactional
    public void setSelectedBlock(String worldId, String sessionId, int x, int y, int z) {
        String key = editStateKey(sessionId);
        redisService.putValue(worldId, key + "selectedBlockX", String.valueOf(x), EDIT_STATE_TTL);
        redisService.putValue(worldId, key + "selectedBlockY", String.valueOf(y), EDIT_STATE_TTL);
        redisService.putValue(worldId, key + "selectedBlockZ", String.valueOf(z), EDIT_STATE_TTL);
        log.debug("Selected block updated: session={} pos=({},{},{})", sessionId, x, y, z);
    }

    /**
     * Get selected block coordinates (if set).
     */
    @Transactional(readOnly = true)
    public Optional<BlockPosition> getSelectedBlock(String worldId, String sessionId) {
        String key = editStateKey(sessionId);
        String xStr = redisService.getValue(worldId, key + "selectedBlockX").orElse(null);
        String yStr = redisService.getValue(worldId, key + "selectedBlockY").orElse(null);
        String zStr = redisService.getValue(worldId, key + "selectedBlockZ").orElse(null);

        if (xStr == null || yStr == null || zStr == null) {
            return Optional.empty();
        }

        try {
            return Optional.of(new BlockPosition(
                    Integer.parseInt(xStr),
                    Integer.parseInt(yStr),
                    Integer.parseInt(zStr)
            ));
        } catch (NumberFormatException e) {
            log.warn("Invalid block position in Redis: session={}", sessionId);
            return Optional.empty();
        }
    }

    /**
     * Delete edit state (on session close).
     */
    @Transactional
    public void deleteEditState(String worldId, String sessionId) {
        String key = editStateKey(sessionId);

        // Delete all related keys
        redisService.deleteValue(worldId, key + "editMode");
        redisService.deleteValue(worldId, key + "editAction");
        redisService.deleteValue(worldId, key + "selectedLayer");
        redisService.deleteValue(worldId, key + "mountX");
        redisService.deleteValue(worldId, key + "mountY");
        redisService.deleteValue(worldId, key + "mountZ");
        redisService.deleteValue(worldId, key + "selectedGroup");
        redisService.deleteValue(worldId, key + "selectedBlockX");
        redisService.deleteValue(worldId, key + "selectedBlockY");
        redisService.deleteValue(worldId, key + "selectedBlockZ");
        redisService.deleteValue(worldId, key + "playerIp");

        log.debug("Edit state deleted: session={}", sessionId);
    }

    /**
     * Validate that selected layer exists and is enabled.
     */
    public Optional<WLayer> validateSelectedLayer(EditState state) {
        if (state.getSelectedLayer() == null) {
            return Optional.empty();
        }
        return layerService.findLayer(state.getWorldId(), state.getSelectedLayer())
                .filter(WLayer::isEnabled);
    }

    // ===== PRIVATE HELPERS =====

    private String editStateKey(String sessionId) {
        return EDIT_STATE_PREFIX + sessionId + ":";
    }

    private void saveEditState(String worldId, String sessionId, EditState state) {
        String key = editStateKey(sessionId);

        redisService.putValue(worldId, key + "editMode", String.valueOf(state.isEditMode()), EDIT_STATE_TTL);

        if (state.getEditAction() != null) {
            redisService.putValue(worldId, key + "editAction", state.getEditAction().name(), EDIT_STATE_TTL);
        }

        if (state.getSelectedLayer() != null) {
            redisService.putValue(worldId, key + "selectedLayer", state.getSelectedLayer(), EDIT_STATE_TTL);
        } else {
            redisService.deleteValue(worldId, key + "selectedLayer");
        }

        if (state.getMountX() != null) {
            redisService.putValue(worldId, key + "mountX", String.valueOf(state.getMountX()), EDIT_STATE_TTL);
        }
        if (state.getMountY() != null) {
            redisService.putValue(worldId, key + "mountY", String.valueOf(state.getMountY()), EDIT_STATE_TTL);
        }
        if (state.getMountZ() != null) {
            redisService.putValue(worldId, key + "mountZ", String.valueOf(state.getMountZ()), EDIT_STATE_TTL);
        }

        redisService.putValue(worldId, key + "selectedGroup", String.valueOf(state.getSelectedGroup()), EDIT_STATE_TTL);

        if (state.getPlayerIp() != null) {
            redisService.putValue(worldId, key + "playerIp", state.getPlayerIp(), EDIT_STATE_TTL);
        } else {
            redisService.deleteValue(worldId, key + "playerIp");
        }

        log.trace("Edit state saved: session={} layer={} playerIp={}", sessionId, state.getSelectedLayer(), state.getPlayerIp());
    }

    private boolean parseBoolean(String value, boolean defaultValue) {
        if (value == null) return defaultValue;
        return "true".equalsIgnoreCase(value);
    }

    private EditAction parseEditAction(String value) {
        if (value == null) return EditAction.OPEN_CONFIG_DIALOG;
        try {
            return EditAction.valueOf(value);
        } catch (IllegalArgumentException e) {
            return EditAction.OPEN_CONFIG_DIALOG;
        }
    }

    private Integer parseInt(String value) {
        if (value == null) return null;
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Block position record.
     */
    public record BlockPosition(int x, int y, int z) {}
}
