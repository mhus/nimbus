package de.mhus.nimbus.world.control.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for triggering block updates on world-player via REST commands.
 * Used by block editor and copy/move operations to send "b.u" updates to clients.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BlockUpdateService {

    private final EditService editService;
    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Send block update to world-player to trigger "b.u" message on WebSocket.
     *
     * @param worldId   World identifier
     * @param sessionId Session identifier
     * @param x         Block X coordinate
     * @param y         Block Y coordinate
     * @param z         Block Z coordinate
     * @param blockId   Block identifier
     * @param meta      Block metadata (optional)
     * @return true if command was sent successfully
     */
    public boolean sendBlockUpdate(String worldId, String sessionId, int x, int y, int z, String blockId, String meta) {
        // Get edit state to retrieve player IP and port
        EditState state = editService.getEditState(worldId, sessionId);
        String playerUrl = state.getPlayerUrl();

        if (Strings.isEmpty(playerUrl)) {
            log.warn("Cannot send block update: no player IP stored for session={}", sessionId);
            return false;
        }

        try {
            // Build command request
            Map<String, Object> request = new HashMap<>();
            request.put("worldId", worldId);
            request.put("sessionId", sessionId);
            request.put("commandName", "BlockUpdate");
            request.put("args", List.of(
                    String.valueOf(x),
                    String.valueOf(y),
                    String.valueOf(z),
                    blockId != null ? blockId : "air",
                    meta != null ? meta : ""
            ));

            // Send POST request
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

            restTemplate.postForObject(playerUrl, entity, Map.class);

            log.debug("Sent block update to world-player: session={} pos=({},{},{}) playerUrl={}",
                    sessionId, x, y, z, playerUrl);

            return true;

        } catch (Exception e) {
            log.error("Failed to send block update to world-player: session={} playerUrl={}", sessionId, playerUrl, e);
            return false;
        }
    }

    // ==================== PRIVATE HELPERS ====================

    private String buildWorldPlayerUrl(String playerIp, int playerPort) {
        return "http://" + playerIp + ":" + playerPort + "/world/world/command";
    }
}
