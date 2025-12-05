package de.mhus.nimbus.world.control.service;

import de.mhus.nimbus.world.shared.client.WorldClientService;
import de.mhus.nimbus.world.shared.commands.CommandContext;
import de.mhus.nimbus.world.shared.commands.CommandService;
import de.mhus.nimbus.world.shared.layer.EditAction;
import de.mhus.nimbus.world.shared.layer.WLayer;
import de.mhus.nimbus.world.shared.layer.WLayerService;
import de.mhus.nimbus.world.shared.redis.WorldRedisService;
import de.mhus.nimbus.world.shared.session.WSession;
import de.mhus.nimbus.world.shared.session.WSessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
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
    private final WorldClientService worldClient;
    private final WSessionService wSessionService;

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
     * Execute edit action at block position.
     * Reads current EditAction from state and performs corresponding operation.
     */
    @Transactional
    public void doAction(String worldId, String sessionId, int x, int y, int z) {
        // Get current edit state
        EditState state = getEditState(worldId, sessionId);
        EditAction action = state.getEditAction();

        // Get playerUrl from WSession (not from EditState)
        Optional<WSession> wSession = wSessionService.getWithPlayerUrl(sessionId);
        if (wSession.isEmpty() || Strings.isBlank(wSession.get().getPlayerUrl())) {
            log.warn("No player URL available for session {}, cannot perform edit action", sessionId);
            return;
        }

        String playerUrl = wSession.get().getPlayerUrl();

        if (playerUrl == null) {
            log.warn("No player URL available for session {}, cannot perform edit action", sessionId);
            return;
        }

        if (action == null) {
            action = EditAction.OPEN_CONFIG_DIALOG; // Default
        }

        log.debug("Executing edit action: session={} action={} pos=({},{},{})",
                sessionId, action, x, y, z);

        switch (action) {
            case OPEN_CONFIG_DIALOG:
                // Open config dialog at client
                openConfigDialogAtClient(worldId, sessionId, playerUrl);
                break;
            case OPEN_EDITOR:
                // Open block editor dialog at client
                setSelectedBlock(worldId, sessionId, x, y, z);
                openBlockEditorDialogAtClient(worldId, sessionId, playerUrl, x, y, z);
                break;

            case MARK_BLOCK:
                // Store marked block for copy/move operations
                setMarkedBlock(worldId, sessionId, x, y, z);
                log.info("Block marked: session={} pos=({},{},{})", sessionId, x, y, z);
                break;

            case COPY_BLOCK:
                // Copy marked block to current position
                copyMarkedBlock(worldId, sessionId, x, y, z);
                break;

            case DELETE_BLOCK:
                // Delete block at position
                deleteBlock(worldId, sessionId, x, y, z);
                break;

            case MOVE_BLOCK:
                // Move marked block to current position
                moveMarkedBlock(worldId, sessionId, x, y, z);
                break;

            default:
                log.warn("Unknown edit action: {}", action);
                setSelectedBlock(worldId, sessionId, x, y, z);
                break;
        }
    }

    private void openConfigDialogAtClient(String worldId, String sessionId, String origin) {
        CommandContext ctx = CommandContext.builder()
                .worldId(worldId)
                .sessionId(sessionId)
                .originServer("world-control")
                .build();
        worldClient.sendPlayerCommand(
                worldId,
                sessionId,
                origin,
                "client",
                List.of("openComponent", "edit_config" ),
                ctx);
    }

    private void openBlockEditorDialogAtClient(String worldId, String sessionId, String origin, int x, int y, int z) {
        CommandContext ctx = CommandContext.builder()
                .worldId(worldId)
                .sessionId(sessionId)
                .originServer("world-control")
                .build();
        worldClient.sendPlayerCommand(
                worldId,
                sessionId,
                origin,
                "client",
                List.of("openComponent", "block_editor", String.valueOf(x),String.valueOf(y),String.valueOf(z) ),
                ctx);
    }

    /**
     * Update selected block coordinates.
     */
    @Transactional
    private void setSelectedBlock(String worldId, String sessionId, int x, int y, int z) {
        String key = editStateKey(sessionId);
        redisService.putValue(worldId, key + "selectedBlockX", String.valueOf(x), EDIT_STATE_TTL);
        redisService.putValue(worldId, key + "selectedBlockY", String.valueOf(y), EDIT_STATE_TTL);
        redisService.putValue(worldId, key + "selectedBlockZ", String.valueOf(z), EDIT_STATE_TTL);
        log.debug("Selected block updated: session={} pos=({},{},{})", sessionId, x, y, z);

    }

    /**
     * Store marked block coordinates for copy/move operations.
     */
    @Transactional
    private void setMarkedBlock(String worldId, String sessionId, int x, int y, int z) {
        String key = editStateKey(sessionId);
        redisService.putValue(worldId, key + "markedBlockX", String.valueOf(x), EDIT_STATE_TTL);
        redisService.putValue(worldId, key + "markedBlockY", String.valueOf(y), EDIT_STATE_TTL);
        redisService.putValue(worldId, key + "markedBlockZ", String.valueOf(z), EDIT_STATE_TTL);
        log.debug("Marked block stored: session={} pos=({},{},{})", sessionId, x, y, z);
    }

    /**
     * Get marked block coordinates (if set).
     */
    @Transactional(readOnly = true)
    public Optional<BlockPosition> getMarkedBlock(String worldId, String sessionId) {
        String key = editStateKey(sessionId);
        String xStr = redisService.getValue(worldId, key + "markedBlockX").orElse(null);
        String yStr = redisService.getValue(worldId, key + "markedBlockY").orElse(null);
        String zStr = redisService.getValue(worldId, key + "markedBlockZ").orElse(null);

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
            log.warn("Invalid marked block position in Redis: session={}", sessionId);
            return Optional.empty();
        }
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
        redisService.deleteValue(worldId, key + "markedBlockX");
        redisService.deleteValue(worldId, key + "markedBlockY");
        redisService.deleteValue(worldId, key + "markedBlockZ");
        redisService.deleteValue(worldId, key + "playerIp");
        redisService.deleteValue(worldId, key + "playerPort");

        log.debug("Edit state deleted: session={}", sessionId);
    }

    /**
     * Copy marked block to target position.
     */
    @Transactional
    private void copyMarkedBlock(String worldId, String sessionId, int x, int y, int z) {
        Optional<BlockPosition> markedOpt = getMarkedBlock(worldId, sessionId);
        if (markedOpt.isEmpty()) {
            log.warn("No marked block for copy: session={}", sessionId);
            return;
        }

        BlockPosition marked = markedOpt.get();

        // TODO: Read block data from marked position (requires world-player coordination)
        // For now: Just log the operation
        log.info("Copy block: session={} from=({},{},{}) to=({},{},{})",
                sessionId, marked.x(), marked.y(), marked.z(), x, y, z);

        // Update selected block to target position
        setSelectedBlock(worldId, sessionId, x, y, z);
    }

    /**
     * Move marked block to target position.
     */
    @Transactional
    private void moveMarkedBlock(String worldId, String sessionId, int x, int y, int z) {
        Optional<BlockPosition> markedOpt = getMarkedBlock(worldId, sessionId);
        if (markedOpt.isEmpty()) {
            log.warn("No marked block for move: session={}", sessionId);
            return;
        }

        BlockPosition marked = markedOpt.get();

        // TODO: Read block from marked position, write to target, delete at marked
        // For now: Just log the operation
        log.info("Move block: session={} from=({},{},{}) to=({},{},{})",
                sessionId, marked.x(), marked.y(), marked.z(), x, y, z);

        // Clear marked block after move
        String key = editStateKey(sessionId);
        redisService.deleteValue(worldId, key + "markedBlockX");
        redisService.deleteValue(worldId, key + "markedBlockY");
        redisService.deleteValue(worldId, key + "markedBlockZ");

        // Update selected block to target position
        setSelectedBlock(worldId, sessionId, x, y, z);
    }

    /**
     * Delete block at position.
     */
    @Transactional
    private void deleteBlock(String worldId, String sessionId, int x, int y, int z) {
        // TODO: Write air block to position (requires block data access)
        // For now: Just log the operation
        log.info("Delete block: session={} pos=({},{},{})", sessionId, x, y, z);

        // Update selected block position
        setSelectedBlock(worldId, sessionId, x, y, z);
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

        log.trace("Edit state saved: session={} layer={}",
                sessionId, state.getSelectedLayer());
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
