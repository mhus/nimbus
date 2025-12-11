package de.mhus.nimbus.world.control.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.mhus.nimbus.generated.types.Block;
import de.mhus.nimbus.generated.types.EditAction;
import de.mhus.nimbus.generated.types.Vector3;
import de.mhus.nimbus.world.shared.client.WorldClientService;
import de.mhus.nimbus.world.shared.commands.CommandContext;
import de.mhus.nimbus.world.shared.layer.WLayer;
import de.mhus.nimbus.world.shared.layer.WLayerService;
import de.mhus.nimbus.world.shared.redis.WorldRedisService;
import de.mhus.nimbus.world.shared.session.WSession;
import de.mhus.nimbus.world.shared.session.WSessionService;
import de.mhus.nimbus.world.shared.world.BlockUtil;
import de.mhus.nimbus.world.shared.world.WWorldService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
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
    private final WorldClientService worldClient;
    private final WSessionService wSessionService;
    private final BlockInfoService blockInfoService;
    private final WWorldService worldService;
    private final ObjectMapper objectMapper;
    private final de.mhus.nimbus.world.shared.overlay.BlockOverlayService blockOverlayService;
    private final BlockUpdateService blockUpdateService;

    private static final Duration EDIT_STATE_TTL = Duration.ofHours(24);
    private static final String EDIT_STATE_PREFIX = "edit:";
    private static final Duration OVERLAY_TTL = Duration.ofHours(24);

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
                clientOpenConfigDialogAtClient(worldId, sessionId, playerUrl);
                break;
            case OPEN_EDITOR:
                // Open block editor dialog at client
                setSelectedBlock(worldId, sessionId, x, y, z);
                clientOpenBlockEditorDialogAtClient(worldId, sessionId, playerUrl, x, y, z);
                break;

            case MARK_BLOCK:
                // get block definition at position
                Map<String, Object> blockInfo = blockInfoService.loadBlockInfo(worldId, sessionId, x, y, z);
                // Store marked block and show in client
                doMarkBlock(worldId, sessionId, x, y, z);
                // store also blockInfo data in redis to use in copy
                storeBlockDataRegistry(worldId, sessionId, blockInfo);
                log.info("Block marked: session={} pos=({},{},{})", sessionId, x, y, z);
                break;

            case EditAction.PASTE_BLOCK:
                // Paste marked block to current position
                pasteMarkedBlock(worldId, sessionId, x, y, z);
                break;

            case DELETE_BLOCK:
                // Delete block at position
                deleteBlock(worldId, sessionId, x, y, z);
                break;

            case SMOOTH_BLOCKS:
                smoothBlocks(worldId, sessionId, x, y, z);
                break;
            case ROUGH_BLOCKS:
                roughBlocks(worldId, sessionId, x, y, z);
                break;
            default:
                log.warn("Unknown edit action: {}", action);
                setSelectedBlock(worldId, sessionId, x, y, z);
                break;
        }
    }

    private void roughBlocks(String worldId, String sessionId, int x, int y, int z) {
        EditState editState = getEditState(worldId, sessionId);

        RoughBlockOperation.builder()
                .editService(this)
                .editState(editState)
                .sessionId(sessionId)
                .centerX(x)
                .centerY(y)
                .centerZ(z)
                .build()
                .execute();
    }

    private void smoothBlocks(String worldId, String sessionId, int x, int y, int z) {
        EditState editState = getEditState(worldId, sessionId);

        SmoothBlockOperation.builder()
                .editService(this)
                .editState(editState)
                .sessionId(sessionId)
                .centerX(x)
                .centerY(y)
                .centerZ(z)
                .build()
                .execute();
    }

    private void clientSetSelectedEditBlock(String worldId, String sessionId, String origin, int x, int y, int z) {
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
                List.of("setSelectedEditBlock", String.valueOf(x),String.valueOf(y),String.valueOf(z) ),
                ctx);
    }

    private void clientOpenConfigDialogAtClient(String worldId, String sessionId, String origin) {
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

    private void clientOpenBlockEditorDialogAtClient(String worldId, String sessionId, String origin, int x, int y, int z) {
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
     * Store marked block info (complete block data with metadata) in Redis.
     * Used for copy/move operations.
     */
    private void storeBlockDataRegistry(String worldId, String sessionId, Map<String, Object> blockInfo) {
        try {
            String key = editStateKey(sessionId);
            if (blockInfo == null) {
                // Clear stored info
                redisService.deleteValue(worldId, key + "registerBlockInfo");
                log.debug("Register block info cleared: session={}", sessionId);
                return;
            }
            String blockInfoJson = objectMapper.writeValueAsString(blockInfo);
            redisService.putValue(worldId, key + "registerBlockInfo", blockInfoJson, EDIT_STATE_TTL);
            log.debug("Register block info stored: session={} size={} bytes", sessionId, blockInfoJson.length());
        } catch (Exception e) {
            log.error("Failed to store register block info: session={}", sessionId, e);
        }
    }

    /**
     * Get marked block info (complete block data with metadata) from Redis.
     * Returns empty if not found or parse error.
     */
    @Transactional(readOnly = true)
    public Optional<Map<String, Object>> getBlockDataRegister(String worldId, String sessionId) {
        try {
            String key = editStateKey(sessionId);
            Optional<String> blockInfoJsonOpt = redisService.getValue(worldId, key + "registerBlockInfo");

            if (blockInfoJsonOpt.isEmpty()) {
                return Optional.empty();
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> blockInfo = objectMapper.readValue(blockInfoJsonOpt.get(), Map.class);
            log.debug("Register block info loaded: session={}", sessionId);
            return Optional.of(blockInfo);

        } catch (Exception e) {
            log.warn("Failed to load register block info: session={}", sessionId, e);
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
     * Paste marked block to target position.
     * Uses stored block data from Redis to recreate the block at the new position.
     * Only the block content is used, not the original position.
     */
    @Transactional
    private void pasteMarkedBlock(String worldId, String sessionId, int x, int y, int z) {
        // Get marked block data from Redis
        Optional<Block> originalBlockOpt = getRegisterBlockData(worldId, sessionId);
        if (originalBlockOpt.isEmpty()) {
            log.warn("No marked block data for paste: session={}", sessionId);
            return;
        }

        Block originalBlock = originalBlockOpt.get();

        // Clone block with all properties (without position)
        Block pastedBlock = BlockUtil.cloneBlock(originalBlock);

        // Set new position
        pastedBlock.setPosition(Vector3.builder()
                .x((double) x)
                .y((double) y)
                .z((double) z)
                .build());

        // Save to Redis overlay using BlockOverlayService
        String blockJson = blockOverlayService.saveBlockOverlay(worldId, sessionId, pastedBlock);

        if (blockJson == null) {
            log.error("Failed to save pasted block: session={} to=({},{},{})",
                    sessionId, x, y, z);
            return;
        }

        boolean sent = blockUpdateService.sendBlockUpdate(worldId, sessionId, x, y, z, blockJson, null);
        if (!sent) {
            log.warn("Failed to send block update to client: session={}", sessionId);
        }

        log.info("Block pasted: session={} to=({},{},{}) type={}",
                sessionId, x, y, z, pastedBlock.getBlockTypeId());

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
     * Mark a block at the specified position.
     * Sends command to client to highlight/mark the block.
     */
    @Transactional
    public void doMarkBlock(String worldId, String sessionId, int x, int y, int z) {
        // Get playerUrl from WSession
        Optional<WSession> wSession = wSessionService.getWithPlayerUrl(sessionId);
        if (wSession.isEmpty() || Strings.isBlank(wSession.get().getPlayerUrl())) {
            log.warn("No player URL available for session {}, cannot mark block", sessionId);
            return;
        }

        String playerUrl = wSession.get().getPlayerUrl();

        setMarkedBlock(worldId, sessionId, x, y, z);

        // Send command to client to mark the block visually
        clientSetSelectedEditBlock(worldId, sessionId, playerUrl, x, y, z);

        log.info("Block marked: worldId={}, session={}, pos=({},{},{})", worldId, sessionId, x, y, z);
    }

    /**
     * Clear the marked block.
     * Removes marked block from Redis and sends empty setSelectedEditBlock to client.
     */
    @Transactional
    public void clearMarkedBlock(String worldId, String sessionId) {
        // Get playerUrl from WSession
        Optional<WSession> wSession = wSessionService.getWithPlayerUrl(sessionId);
        if (wSession.isEmpty() || Strings.isBlank(wSession.get().getPlayerUrl())) {
            log.warn("No player URL available for session {}, cannot clear mark", sessionId);
            return;
        }

        String playerUrl = wSession.get().getPlayerUrl();

        // Remove marked block from Redis
        String key = editStateKey(sessionId);
        redisService.deleteValue(worldId, key + "markedBlockX");
        redisService.deleteValue(worldId, key + "markedBlockY");
        redisService.deleteValue(worldId, key + "markedBlockZ");

        // Send empty command to client to clear the visual marker
        CommandContext ctx = CommandContext.builder()
                .worldId(worldId)
                .sessionId(sessionId)
                .originServer("world-control")
                .build();
        worldClient.sendPlayerCommand(
                worldId,
                sessionId,
                playerUrl,
                "client",
                List.of("setSelectedEditBlock"),
                ctx);

        log.info("Marked block cleared: worldId={}, session={}", worldId, sessionId);
    }

    /**
     * Get marked block as Block object from Redis.
     * Returns the complete Block that was stored when the block was marked.
     * Uses the stored markedBlockInfo, not the overlay.
     */
    @Transactional(readOnly = true)
    public Optional<Block> getRegisterBlockData(String worldId, String sessionId) {
        // Get marked block info from Redis (stored when block was marked)
        Optional<Map<String, Object>> blockInfoOpt = getBlockDataRegister(worldId, sessionId);
        if (blockInfoOpt.isEmpty()) {
            log.debug("No registered block data found: sessionId={}", sessionId);
            return Optional.empty();
        }

        Map<String, Object> blockInfo = blockInfoOpt.get();

        // Extract block from blockInfo
        Object blockObj = blockInfo.get("block");
        if (blockObj == null) {
            log.warn("No block data in marked block info: sessionId={}", sessionId);
            return Optional.empty();
        }

        try {
            // Convert block object to Block
            Block block = objectMapper.convertValue(blockObj, Block.class);
            log.debug("Retrieved marked block: blockTypeId={}", block.getBlockTypeId());
            return Optional.of(block);
        } catch (Exception e) {
            log.warn("Failed to convert marked block data: sessionId={}, error={}", sessionId, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Set paste block in Redis.
     * Stores a complete block definition for later paste operations.
     */
    @Transactional
    public void setPasteBlock(String worldId, String sessionId, String blockJson) {
        String key = editStateKey(sessionId);
        redisService.putValue(worldId, key + "pasteBlock", blockJson, EDIT_STATE_TTL);
        log.debug("Paste block set: session={}", sessionId);
    }

    /**
     * Get paste block from Redis.
     * Returns the stored block JSON for paste operations.
     */
    @Transactional(readOnly = true)
    public Optional<String> getPasteBlock(String worldId, String sessionId) {
        String key = editStateKey(sessionId);
        return redisService.getValue(worldId, key + "pasteBlock");
    }

    /**
     * Set marked block data (complete block JSON for palette selection).
     * Uses existing storeMarkedBlockInfo to store in Redis.
     *
     * @param worldId World identifier
     * @param sessionId Session identifier
     * @param blockJson Block data as JSON string
     */
    public void setBlockRegisterData(String worldId, String sessionId, String blockJson) {
        try {

            if (blockJson == null ) {
                storeBlockDataRegistry(worldId, sessionId, null);
                log.info("Cleared block register data: worldId={}, sessionId={}",
                        worldId, sessionId);
                return;
            }
            // Parse block JSON
            Block block = objectMapper.readValue(blockJson, Block.class);

            // Create blockInfo map (same format as when marking from world)
            Map<String, Object> blockInfo = new HashMap<>();
            blockInfo.put("block", block);

            // Use existing store method
            storeBlockDataRegistry(worldId, sessionId, blockInfo);

            log.info("Register block set from palette: worldId={}, sessionId={}, blockTypeId={}",
                    worldId, sessionId, block.getBlockTypeId());

        } catch (Exception e) {
            log.error("Failed to set register block data: worldId={}, sessionId={}",
                    worldId, sessionId, e);
            throw new RuntimeException("Failed to set marked block data", e);
        }
    }

    /**
     * Get block at position, reading from overlay, layer, and chunk (in that order).
     * Uses blockInfoService which handles the priority correctly:
     * 1. Overlay (if sessionId provided)
     * 2. Selected layer (if editState has selectedLayer)
     * 3. Chunk data
     *
     * @param editState Edit state containing layer selection
     * @param sessionId Session ID for overlay lookup
     * @param x X coordinate
     * @param y Y coordinate
     * @param z Z coordinate
     * @return Block at position, or null if not found
     */
    public Block getBlock(EditState editState, String sessionId, int x, int y, int z) {
        String worldId = editState.getWorldId();

        // Use blockInfoService which handles the priority correctly
        // It reads from: overlay -> layer -> chunk
        Map<String, Object> blockInfo = blockInfoService.loadBlockInfo(worldId, sessionId, x, y, z);

        if (blockInfo == null) {
            return null;
        }

        Object blockObj = blockInfo.get("block");
        if (blockObj == null) {
            return null;
        }

        try {
            return objectMapper.convertValue(blockObj, Block.class);
        } catch (Exception e) {
            log.warn("Failed to convert block at ({},{},{}): {}", x, y, z, e.getMessage());
            return null;
        }
    }

    /**
     * Update block at position.
     * Writes to overlay and sends update to client.
     * Always writes through the selected layer in edit state.
     *
     * @param editState Edit state containing session and layer info
     * @param sessionId Session ID for overlay and client updates
     * @param x X coordinate
     * @param y Y coordinate
     * @param z Z coordinate
     * @param block Block to write
     * @return true if successful
     */
    public boolean updateBlock(EditState editState, String sessionId, int x, int y, int z, Block block) {
        String worldId = editState.getWorldId();

        // Set position in block if not already set
        if (block.getPosition() == null) {
            block.setPosition(Vector3.builder()
                    .x((double) x)
                    .y((double) y)
                    .z((double) z)
                    .build());
        }

        // Save to overlay using BlockOverlayService
        String blockJson = blockOverlayService.saveBlockOverlay(worldId, sessionId, block);

        if (blockJson == null) {
            log.error("Failed to save block to overlay: worldId={}, pos=({},{},{})",
                    worldId, x, y, z);
            return false;
        }

        // Send update to client
        boolean sent = blockUpdateService.sendBlockUpdate(worldId, sessionId, x, y, z, blockJson, null);
        if (!sent) {
            log.warn("Failed to send block update to client: session={}, pos=({},{},{})",
                    sessionId, x, y, z);
        }

        return true;
    }

    /**
     * Block position record.
     */
    public record BlockPosition(int x, int y, int z) {}
}
