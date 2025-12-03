package de.mhus.nimbus.world.player.ws.redis;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import de.mhus.nimbus.world.player.ws.NetworkMessage;
import de.mhus.nimbus.world.player.ws.PlayerSession;
import de.mhus.nimbus.world.player.ws.SessionManager;
import de.mhus.nimbus.world.shared.redis.WorldRedisMessagingService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;

/**
 * Redis listener for user movement updates.
 * Receives movement events from Redis and distributes to relevant sessions.
 *
 * Distribution rules:
 * - Session must be authenticated
 * - Session must be in same world
 * - Session must NOT be the originating session (ignore own events)
 * - Session must have the player's chunk registered (cx, cz)
 *
 * Redis message format:
 * {
 *   "sessionId": "abc123",
 *   "userId": "user123",
 *   "displayName": "Player",
 *   "p": {"x": 100.5, "y": 65.0, "z": -200.5},
 *   "r": {"y": 90.0, "p": 0.0},
 *   "cx": 6,  // chunk x coordinate
 *   "cz": -13 // chunk z coordinate
 * }
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MovementBroadcastListener {

    private final WorldRedisMessagingService redisMessaging;
    private final SessionManager sessionManager;
    private final ObjectMapper objectMapper;

    /**
     * Subscribe to all active worlds on startup.
     * TODO: Dynamically subscribe when new worlds become active
     */
    @PostConstruct
    public void subscribeToWorlds() {
        // Subscribe to "main" world for now
        // In production, subscribe to all active worlds dynamically
        subscribeToWorld("main");
    }

    /**
     * Subscribe to movement updates for a specific world.
     */
    public void subscribeToWorld(String worldId) {
        redisMessaging.subscribe(worldId, "u.m", (topic, message) -> {
            handleMovementUpdate(worldId, message);
        });
        log.info("Subscribed to movement updates for world: {}", worldId);
    }

    /**
     * Handle incoming movement update from Redis.
     */
    private void handleMovementUpdate(String worldId, String message) {
        try {
            JsonNode data = objectMapper.readTree(message);

            // Extract metadata
            String originatingSessionId = data.has("sessionId") ? data.get("sessionId").asText() : null;
            Integer cx = data.has("cx") ? data.get("cx").asInt() : null;
            Integer cz = data.has("cz") ? data.get("cz").asInt() : null;

            if (originatingSessionId == null) {
                log.warn("Movement update without sessionId, ignoring");
                return;
            }

            // Build client message (without internal metadata)
            ObjectNode clientData = objectMapper.createObjectNode();
            if (data.has("userId")) clientData.put("userId", data.get("userId").asText());
            if (data.has("displayName")) clientData.put("displayName", data.get("displayName").asText());
            if (data.has("p")) clientData.set("p", data.get("p"));
            if (data.has("r")) clientData.set("r", data.get("r"));

            NetworkMessage networkMessage = NetworkMessage.builder()
                    .t("u.m")
                    .d(clientData)
                    .build();

            String json = objectMapper.writeValueAsString(networkMessage);
            TextMessage textMessage = new TextMessage(json);

            // Distribute to relevant sessions
            int sentCount = 0;
            for (PlayerSession session : sessionManager.getAllSessions().values()) {
                // Skip if not authenticated
                if (!session.isAuthenticated()) continue;

                // Skip if different world
                if (!worldId.equals(session.getWorldId())) continue;

                // Skip if this is the originating session (ignore own events)
                if (originatingSessionId.equals(session.getSessionId())) {
                    log.trace("Skipping originating session: {}", session.getSessionId());
                    continue;
                }

                // Check if session has registered the chunk where player is located
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

            log.trace("Distributed movement update to {} sessions (origin={}, chunk={},{})",
                    sentCount, originatingSessionId, cx, cz);

        } catch (Exception e) {
            log.error("Failed to handle movement update from Redis: {}", message, e);
        }
    }

    /**
     * Unsubscribe from world (e.g., when shutting down).
     */
    public void unsubscribeFromWorld(String worldId) {
        redisMessaging.unsubscribe(worldId, "u.m");
        log.info("Unsubscribed from movement updates for world: {}", worldId);
    }
}
