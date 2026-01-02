package de.mhus.nimbus.world.control.service;

import de.mhus.nimbus.generated.types.Block;
import de.mhus.nimbus.shared.engine.EngineMapper;
import de.mhus.nimbus.world.shared.client.WorldClientService;
import de.mhus.nimbus.world.shared.commands.CommandContext;
import de.mhus.nimbus.world.shared.session.WSession;
import de.mhus.nimbus.world.shared.session.WSessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Service for triggering block updates on world-player via WorldClientService.
 * Used by block editor and copy/move operations to send "b.u" updates to clients.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BlockUpdateService {

    private final WSessionService wSessionService;
    private final WorldClientService worldClientService;
    private final EngineMapper objectMapper;

    /**
     * Send block update with optional source information.
     * Source is typically set to "layerDataId:layerName" for editor operations.
     *
     * @param worldId   World identifier
     * @param sessionId Session identifier
     * @param x         Block X coordinate
     * @param y         Block Y coordinate
     * @param z         Block Z coordinate
     * @param block     Block data
     * @param source    Source information (e.g., "layerDataId:layerName", optional)
     * @param meta      Block metadata (optional)
     * @return true if command was sent successfully
     */
    public boolean sendBlockUpdateWithSource(String worldId, String sessionId, int x, int y, int z, Block block, String source, String meta) {
        // Set source field if provided
        if (source != null && !source.isBlank()) {
            block.setSource(source);
        }

        return sendBlockUpdate(worldId, sessionId, x, y, z, block, meta);
    }

    /**
     * Send block update without source information.
     * Kept for backward compatibility.
     */
    public boolean sendBlockUpdate(String worldId, String sessionId, int x, int y, int z, Block block, String meta) {
        // Serialize block to JSON
        try {
            String blockJson = objectMapper.writeValueAsString(block);
            return sendBlockUpdate(worldId, sessionId, x, y, z, blockJson, meta);
        } catch (Exception e) {
            log.error("Failed to serialize block for update: session={} pos=({},{},{})", sessionId, x, y, z, e);
            return false;
        }
    }

    /**
     * Send block update to world-player to trigger "b.u" message on WebSocket.
     *
     * @param worldId   World identifier
     * @param sessionId Session identifier
     * @param x         Block X coordinate
     * @param y         Block Y coordinate
     * @param z         Block Z coordinate
     * @param blockJson   Block identifier
     * @param meta      Block metadata (optional)
     * @return true if command was sent successfully
     */
    public boolean sendBlockUpdate(String worldId, String sessionId, int x, int y, int z, String blockJson, String meta) {
        // Get playerUrl from WSession
        Optional<WSession> wSession = wSessionService.getWithPlayerUrl(sessionId);
        if (wSession.isEmpty() || Strings.isBlank(wSession.get().getPlayerUrl())) {
            log.warn("No player URL available for session {}, cannot send block update", sessionId);
            return false;
        }

        String playerUrl = wSession.get().getPlayerUrl();

        try {
            // Build command context
            CommandContext ctx = CommandContext.builder()
                    .worldId(worldId)
                    .sessionId(sessionId)
                    .originServer("world-control")
                    .build();

            // Send BlockUpdate command to world-player via WorldClientService
            worldClientService.sendPlayerCommand(
                    worldId,
                    sessionId,
                    playerUrl,
                    "BlockUpdate",
                    List.of(blockJson),
                    ctx
            );

            log.debug("Sent block update to world-player: session={} pos=({},{},{}) blockId={}",
                    sessionId, x, y, z, blockJson);

            return true;

        } catch (Exception e) {
            log.error("Failed to send block update to world-player: session={} playerUrl={}", sessionId, playerUrl, e);
            return false;
        }
    }
}
