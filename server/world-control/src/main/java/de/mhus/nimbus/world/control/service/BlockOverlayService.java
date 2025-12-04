package de.mhus.nimbus.world.control.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.mhus.nimbus.generated.types.Block;
import de.mhus.nimbus.generated.types.Vector3;
import de.mhus.nimbus.world.shared.redis.WorldRedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Service for storing block edits in Redis overlay.
 * Blocks are stored per chunk in Redis hash: world:{worldId}:overlay:{sessionId}:{cx}:{cz}
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BlockOverlayService {

    private final WorldRedisService redisService;
    private final BlockUpdateService blockUpdateService;
    private final ObjectMapper objectMapper;

    private static final Duration OVERLAY_TTL = Duration.ofHours(24);

    /**
     * Save block to Redis overlay and trigger client update.
     *
     * @param worldId   World identifier
     * @param sessionId Session identifier
     * @param x         Block X coordinate
     * @param y         Block Y coordinate
     * @param z         Block Z coordinate
     * @param blockTypeId Block type ID (use "air" or "0" for delete)
     * @param metadata  Block metadata JSON (can be null)
     * @return true if saved successfully
     */
    public boolean saveBlockOverlay(String worldId, String sessionId,
                                    int x, int y, int z,
                                    String blockTypeId, String metadata) {
        try {
            // Calculate chunk coordinates
            int cx = x >> 4;
            int cz = z >> 4;

            // Create position key
            String positionKey = x + ":" + y + ":" + z;

            // Build block object
            Vector3 position = Vector3.builder()
                    .x((double) x)
                    .y((double) y)
                    .z((double) z)
                    .build();

            Block block = Block.builder()
                    .blockTypeId(blockTypeId)
                    .position(position)
                    .build();

            // Serialize block to JSON
            String blockJson = objectMapper.writeValueAsString(block);

            // Save to Redis overlay
            redisService.putOverlayBlock(worldId, sessionId, cx, cz, positionKey, blockJson, OVERLAY_TTL);

            log.info("Saved block overlay: session={} pos=({},{},{}) blockTypeId={}",
                    sessionId, x, y, z, blockTypeId);

            // Send block update to client
            boolean sent = blockUpdateService.sendBlockUpdate(worldId, sessionId, x, y, z, blockTypeId, metadata);

            if (!sent) {
                log.warn("Failed to send block update to client: session={}", sessionId);
            }

            return true;

        } catch (Exception e) {
            log.error("Failed to save block overlay: session={} pos=({},{},{})",
                    sessionId, x, y, z, e);
            return false;
        }
    }

    /**
     * Delete block from overlay (set to AIR).
     */
    public boolean deleteBlockOverlay(String worldId, String sessionId, int x, int y, int z) {
        return saveBlockOverlay(worldId, sessionId, x, y, z, "air", null);
    }
}
