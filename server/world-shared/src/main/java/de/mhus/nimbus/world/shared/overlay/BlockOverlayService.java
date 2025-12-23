package de.mhus.nimbus.world.shared.overlay;

import de.mhus.nimbus.generated.types.Block;
import de.mhus.nimbus.shared.engine.EngineMapper;
import de.mhus.nimbus.world.shared.redis.WorldRedisService;
import de.mhus.nimbus.world.shared.world.WWorld;
import de.mhus.nimbus.world.shared.world.WWorldService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Service for storing block edits in Redis overlay.
 * Blocks are stored per chunk in Redis hash: world:{worldId}:overlay:{sessionId}:{cx}:{cz}
 *
 * Shared service used by:
 * - world-control: To save block edits from block-editor
 * - world-player: To apply overlays when rendering chunks
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BlockOverlayService {

    private final WorldRedisService redisService;
    private final EngineMapper engineMapper;
    private final WWorldService worldService;

    private static final Duration OVERLAY_TTL = Duration.ofHours(24);

    /**
     * Save complete block to Redis overlay.
     *
     * @param worldId   World identifier
     * @param sessionId Session identifier
     * @param block     Complete block object with all properties
     * @return blockJson if saved successfully, null otherwise
     */
    public String saveBlockOverlay(String worldId, String sessionId, Block block) {
        try {
            if (block.getPosition() == null) {
                log.error("Block has no position, cannot save to overlay");
                return null;
            }

            // cleanup block data
            if (block.getOffsets() != null && block.getOffsets().size() == 0) {
                block.setOffsets(null);
            }
            if (block.getModifiers() != null && block.getModifiers().size() == 0) {
                block.setModifiers(null);
            }
            if (block.getModifiers() != null) {
                block.getModifiers().values().forEach((modifier) -> {
                    if (modifier.getAudio() != null && modifier.getAudio().size() == 0) {
                        modifier.setAudio(null);
                    }
                    // TODO more cleanup if needed
                });
            }

            int x = (int) block.getPosition().getX();
            int y = (int) block.getPosition().getY();
            int z = (int) block.getPosition().getZ();

            // Get world and calculate chunk coordinates
            WWorld world = worldService.getByWorldId(worldId).orElse(null);
            if (world == null || world.getPublicData() == null) {
                log.error("World not found or has no publicData: {}", worldId);
                return null;
            }

            // Calculate chunk coordinates using WWorld methods
            int cx = world.getChunkX(x);
            int cz = world.getChunkZ(z);

            // Create position key
            String positionKey = x + ":" + y + ":" + z;

            // Serialize complete block to JSON using EngineMapper
            String blockJson = engineMapper.writeValueAsString(block);

            // Save to Redis overlay
            redisService.putOverlayBlock(worldId, sessionId, cx, cz, positionKey, blockJson, OVERLAY_TTL);

            log.info("Saved block overlay to Redis: session={} pos=({},{},{}) blockTypeId={}",
                    sessionId, x, y, z, block.getBlockTypeId());

            return blockJson;

        } catch (Exception e) {
            log.error("Failed to save block overlay: session={} block={}",
                    sessionId, block, e);
            return null;
        }
    }

    /**
     * Delete block from overlay (set to AIR).
     */
    public String deleteBlockOverlay(String worldId, String sessionId, int x, int y, int z) {
        // Create AIR block
        de.mhus.nimbus.generated.types.Vector3Int position = de.mhus.nimbus.generated.types.Vector3Int.builder()
                .x(x)
                .y(y)
                .z(z)
                .build();

        Block airBlock = Block.builder()
                .blockTypeId("air")
                .position(position)
                .build();

        return saveBlockOverlay(worldId, sessionId, airBlock);
    }
}
