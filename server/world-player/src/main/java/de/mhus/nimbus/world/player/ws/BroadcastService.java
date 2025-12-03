package de.mhus.nimbus.world.player.ws;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;

/**
 * Service for broadcasting messages to relevant player sessions.
 * Handles session filtering logic (authentication, world, chunk registration).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BroadcastService {

    private final SessionManager sessionManager;
    private final ObjectMapper objectMapper;

    /**
     * Broadcast message to sessions in a specific world, optionally filtered by chunk and origin.
     *
     * @param worldId              Target world ID
     * @param messageType          Network message type (e.g., "u.m", "b.u")
     * @param data                 Message data
     * @param originatingSessionId Session that originated the message (will be skipped), null to send to all
     * @param cx                   Chunk X coordinate (null = no chunk filtering)
     * @param cz                   Chunk Z coordinate (null = no chunk filtering)
     * @return Number of sessions the message was sent to
     */
    public int broadcastToWorld(
            String worldId,
            String messageType,
            JsonNode data,
            String originatingSessionId,
            Integer cx,
            Integer cz) {

        try {
            NetworkMessage networkMessage = NetworkMessage.builder()
                    .t(messageType)
                    .d(data)
                    .build();

            String json = objectMapper.writeValueAsString(networkMessage);
            TextMessage textMessage = new TextMessage(json);

            int sentCount = 0;
            for (PlayerSession session : sessionManager.getAllSessions().values()) {
                // Skip if not authenticated
                if (!session.isAuthenticated()) continue;

                // Skip if different world
                if (!worldId.equals(session.getWorldId())) continue;

                // Skip if this is the originating session (ignore own events)
                if (originatingSessionId != null && originatingSessionId.equals(session.getSessionId())) {
                    log.trace("Skipping originating session: {}", session.getSessionId());
                    continue;
                }

                // Check if session has registered the chunk (if chunk coordinates provided)
                if (cx != null && cz != null) {
                    if (!session.isChunkRegistered(cx, cz)) {
                        log.trace("Session {} has not registered chunk ({}, {}), skipping",
                                session.getSessionId(), cx, cz);
                        continue;
                    }
                }

                // Send to session
                session.getWebSocketSession().sendMessage(textMessage);
                sentCount++;
            }

            log.trace("Broadcast {} to {} sessions (origin={}, chunk={},{})",
                    messageType, sentCount, originatingSessionId, cx, cz);

            return sentCount;

        } catch (Exception e) {
            log.error("Failed to broadcast message type: {}", messageType, e);
            return 0;
        }
    }

    /**
     * Broadcast to all authenticated sessions in a world (no chunk filtering).
     */
    public int broadcastToWorld(String worldId, String messageType, JsonNode data) {
        return broadcastToWorld(worldId, messageType, data, null, null, null);
    }

    /**
     * Broadcast to a specific chunk in a world (excludes originating session).
     */
    public int broadcastToChunk(String worldId, int cx, int cz, String messageType, JsonNode data, String originatingSessionId) {
        return broadcastToWorld(worldId, messageType, data, originatingSessionId, cx, cz);
    }
}
