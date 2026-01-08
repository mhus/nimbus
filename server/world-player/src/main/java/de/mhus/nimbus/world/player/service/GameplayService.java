package de.mhus.nimbus.world.player.service;

import com.fasterxml.jackson.databind.JsonNode;
import de.mhus.nimbus.world.player.session.PlayerSession;
import de.mhus.nimbus.world.shared.world.WChunk;
import de.mhus.nimbus.world.shared.world.WChunkService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class GameplayService {

    private final WChunkService chunkService;
    private final PlayerService playerService;
    public void onPlayerEntityInteraction(PlayerSession session, String entityId, String action, Long timestamp, JsonNode params) {
        log.info("Player {} interacted with entity {}: action={}, timestamp={}, params={}",
                session.getPlayer(), entityId, action, timestamp, params);
    }

    public void onPlayerBlockInteraction(PlayerSession session, int x, int y, int z, String blockId, String groupId, String action, JsonNode params) {
        log.info("Player {} interacted with block at ({}, {}, {}): blockId={}, groupId={}, action={}, params={}",
                session.getPlayer(), x, y, z, blockId, groupId, action, params);

        // Check for teleportation in server metadata
        if (session.getWorldId() != null) {
            handleBlockTeleportation(session, x, y, z);
        }
    }

    /**
     * Handle block teleportation if server metadata contains "teleportation" entry.
     *
     * @param session PlayerSession
     * @param x       Block x coordinate
     * @param y       Block y coordinate
     * @param z       Block z coordinate
     */
    private void handleBlockTeleportation(PlayerSession session, int x, int y, int z) {
        // Calculate chunk coordinates
        int chunkSize = 16; // Default chunk size, could be loaded from world config
        int cx = Math.floorDiv(x, chunkSize);
        int cz = Math.floorDiv(z, chunkSize);
        String chunkKey = cx + ":" + cz;

        // Load chunk
        Optional<WChunk> chunkOpt = chunkService.find(session.getWorldId(), chunkKey);
        if (chunkOpt.isEmpty()) {
            log.trace("Chunk not found for block interaction: chunkKey={}, world={}",
                    chunkKey, session.getWorldId());
            return;
        }

        WChunk chunk = chunkOpt.get();

        // Get server metadata for block
        Map<String, String> serverInfo = chunk.getServerInfoForBlock(x, y, z);
        if (serverInfo == null || serverInfo.isEmpty()) {
            log.trace("No server metadata for block at ({}, {}, {})", x, y, z);
            return;
        }

        // Check for teleportation entry
        String teleportTarget = serverInfo.get("teleportation");
        if (teleportTarget == null || teleportTarget.isBlank()) {
            log.trace("No teleportation entry in server metadata for block at ({}, {}, {})", x, y, z);
            return;
        }

        // Trigger teleportation
        log.info("Teleportation triggered by block interaction at ({}, {}, {}): target={}",
                x, y, z, teleportTarget);

        boolean success = playerService.teleportPlayer(session, teleportTarget);
        if (success) {
            log.info("Teleportation set for player {}: target={}", session.getPlayer(), teleportTarget);
        } else {
            log.warn("Failed to set teleportation for player {}: target={}", session.getPlayer(), teleportTarget);
        }
    }
}
