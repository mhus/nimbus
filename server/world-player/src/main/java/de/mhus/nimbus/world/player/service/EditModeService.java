package de.mhus.nimbus.world.player.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.mhus.nimbus.generated.types.Block;
import de.mhus.nimbus.generated.types.ChunkData;
import de.mhus.nimbus.world.player.session.PlayerSession;
import de.mhus.nimbus.world.shared.client.WorldClientService;
import de.mhus.nimbus.world.shared.commands.CommandContext;
import de.mhus.nimbus.world.shared.redis.WorldRedisService;
import de.mhus.nimbus.world.shared.world.BlockUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;

/**
 * Service for managing edit mode operations.
 * Handles overlay storage, retrieval, application, and cleanup.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EditModeService {

    private final WorldRedisService redisService;
    private final WorldClientService worldClientService;
    private final ObjectMapper objectMapper;

    private static final java.time.Duration OVERLAY_TTL = java.time.Duration.ofHours(24);

    /**
     * Enable edit mode for session.
     */
    public void enableEditMode(PlayerSession session) {
        session.setEditMode(true);
        log.info("Edit mode enabled: session={}, worldId={}",
                session.getSessionId(), session.getWorldId());
    }

    /**
     * Disable edit mode and cleanup overlays.
     */
    public void disableEditMode(PlayerSession session) {
        session.setEditMode(false);

        // Trigger async cleanup via world-control
        cleanupOverlaysAsync(session);

        log.info("Edit mode disabled: session={}, worldId={}",
                session.getSessionId(), session.getWorldId());
    }

    /**
     * Check if session is in edit mode.
     */
    public boolean isEditMode(PlayerSession session) {
        return session.isEditMode();
    }

    /**
     * Apply overlays to chunk data for edit mode sessions.
     * Modifies the chunk data in-place by:
     * 1. Overlaying new/modified blocks from Redis
     * 2. Removing blocks marked as AIR in overlay
     *
     * @param session PlayerSession (must be in edit mode)
     * @param chunkData ChunkData to modify
     */
    public void applyOverlays(PlayerSession session, ChunkData chunkData) {
        if (!session.isEditMode()) {
            return;
        }

        try {
            // Get overlays from Redis
            Map<Object, Object> overlays = redisService.getOverlayBlocks(
                    session.getWorldId().getId(),
                    session.getSessionId(),
                    chunkData.getCx(),
                    chunkData.getCz()
            );

            if (overlays.isEmpty()) {
                log.trace("No overlays for chunk: cx={}, cz={}, session={}",
                        chunkData.getCx(), chunkData.getCz(), session.getSessionId());
                return;
            }

            log.debug("Applying {} overlays to chunk: cx={}, cz={}, session={}",
                    overlays.size(), chunkData.getCx(), chunkData.getCz(), session.getSessionId());

            // Parse overlay blocks
            Map<String, Block> overlayBlocks = parseOverlayBlocks(overlays);

            // Build position index of existing blocks
            List<Block> blocks = chunkData.getBlocks();
            if (blocks == null) {
                blocks = new ArrayList<>();
                chunkData.setBlocks(blocks);
            }

            Map<String, Block> blockIndex = new HashMap<>();
            for (Block block : blocks) {
                String posKey = BlockUtil.positionKey(block);
                blockIndex.put(posKey, block);
            }

            // Apply overlays
            for (Map.Entry<String, Block> entry : overlayBlocks.entrySet()) {
                String posKey = entry.getKey();
                Block overlayBlock = entry.getValue();

                if (BlockUtil.isAirType(overlayBlock.getBlockTypeId())) {
                    // AIR overlay = remove block
                    blockIndex.remove(posKey);
                    log.trace("Removed block at {}", posKey);
                } else {
                    // Non-AIR overlay = add or replace block
                    blockIndex.put(posKey, overlayBlock);
                    log.trace("Overlayed block at {} with type {}",
                            posKey, overlayBlock.getBlockTypeId());
                }
            }

            // Rebuild block list
            chunkData.setBlocks(new ArrayList<>(blockIndex.values()));

            log.debug("Applied overlays: chunk={}:{}, original={}, overlay={}, final={}",
                    chunkData.getCx(), chunkData.getCz(),
                    blocks.size(), overlays.size(), chunkData.getBlocks().size());

        } catch (Exception e) {
            log.error("Failed to apply overlays: chunk={}:{}, session={}",
                    chunkData.getCx(), chunkData.getCz(), session.getSessionId(), e);
        }
    }

    /**
     * Parse overlay blocks from Redis hash entries.
     */
    private Map<String, Block> parseOverlayBlocks(Map<Object, Object> overlays) {
        Map<String, Block> result = new HashMap<>();

        for (Map.Entry<Object, Object> entry : overlays.entrySet()) {
            try {
                String posKey = entry.getKey().toString();
                String blockJson = entry.getValue().toString();

                Block block = objectMapper.readValue(blockJson, Block.class);
                result.put(posKey, block);

            } catch (Exception e) {
                log.warn("Failed to parse overlay block: {}", entry.getKey(), e);
            }
        }

        return result;
    }

    /**
     * Cleanup overlays asynchronously via world-control.
     */
    public void cleanupOverlaysAsync(PlayerSession session) {
        if (session.getSessionId() == null) {
            log.warn("Cannot cleanup overlays: session has no sessionId");
            return;
        }

        CommandContext context = CommandContext.builder()
                .worldId(session.getWorldId().getId())
                .sessionId(session.getSessionId())
                .originServer("world-player")
                .requestTime(Instant.now())
                .build();

        // Send cleanup command to world-control (async, fire-and-forget)
        worldClientService.sendControlCommand(
                session.getWorldId().getId(),
                "EditModeClosed",
                List.of(),
                context
        ).exceptionally(throwable -> {
            log.error("Failed to send cleanup command for session: {}",
                    session.getSessionId(), throwable);
            return null;
        });

        log.debug("Cleanup command sent for session: {}", session.getSessionId());
    }
}
