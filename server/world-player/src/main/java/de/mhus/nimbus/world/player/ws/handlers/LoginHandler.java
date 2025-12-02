package de.mhus.nimbus.world.player.ws.handlers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import de.mhus.nimbus.generated.types.WorldInfo;
import de.mhus.nimbus.world.player.ws.NetworkMessage;
import de.mhus.nimbus.world.player.ws.PlayerSession;
import de.mhus.nimbus.world.player.ws.SessionManager;
import de.mhus.nimbus.world.shared.world.WWorld;
import de.mhus.nimbus.world.shared.world.WWorldService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;

import java.time.Instant;
import java.util.UUID;

/**
 * Handles login messages from clients.
 * Message type: "login"
 *
 * Supports:
 * - username/password authentication
 * - token authentication
 * - session resumption with sessionId
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class LoginHandler implements MessageHandler {

    private final ObjectMapper objectMapper;
    private final SessionManager sessionManager;
    private final WWorldService worldService;

    @Override
    public String getMessageType() {
        return "login";
    }

    @Override
    public void handle(PlayerSession session, NetworkMessage message) throws Exception {
        JsonNode data = message.getD();

        // Extract login data
        String username = data.has("username") ? data.get("username").asText() : null;
        String password = data.has("password") ? data.get("password").asText() : null;
        String token = data.has("token") ? data.get("token").asText() : null;
        String worldId = data.has("worldId") ? data.get("worldId").asText() : null;
        String clientType = data.has("clientType") ? data.get("clientType").asText() : "web";
        String existingSessionId = data.has("sessionId") ? data.get("sessionId").asText() : null;

        log.info("Login attempt: username={}, token={}, worldId={}, existingSessionId={}",
                username, token != null ? "***" : null, worldId, existingSessionId);

        // Check if world exists
        String targetWorldId = worldId != null ? worldId : session.getWorldId();
        WWorld world = worldService.getByWorldId(targetWorldId).orElse(null);
        if (world == null || world.getPublicData() == null) {
            log.warn("World not found in database: {}, login failed", targetWorldId);
            sendLoginResponse(session, message.getI(), false, "World not found: " + targetWorldId, null, null);
            return;
        }

        // TODO: Implement real authentication
        // For now, accept all logins
        boolean authenticated = true;
        String userId = username != null ? "user_" + username : "user_" + UUID.randomUUID().toString().substring(0, 8);
        String displayName = username != null ? username : "Guest";

        if (!authenticated) {
            sendLoginResponse(session, message.getI(), false, "Invalid credentials", null, null);
            return;
        }

        // Generate or reuse session ID
        String sessionId = existingSessionId != null ? existingSessionId : UUID.randomUUID().toString();

        // Update session
        session.setAuthenticated(true);
        session.setAuthenticatedAt(Instant.now());
        session.setUserId(userId);
        session.setDisplayName(displayName);
        session.setWorldId(targetWorldId);
        session.setStatus(PlayerSession.SessionStatus.AUTHENTICATED);

        // Register session ID
        sessionManager.setSessionId(session, sessionId);

        // Send success response with world data
        sendLoginResponse(session, message.getI(), true, null, sessionId, world);

        log.info("Login successful: user={}, sessionId={}, worldId={}",
                displayName, sessionId, session.getWorldId());
    }

    private void sendLoginResponse(PlayerSession session, String requestId, boolean success,
                                   String errorMessage, String sessionId, WWorld world) throws Exception {
        ObjectNode data = objectMapper.createObjectNode();
        data.put("success", success);

        if (success) {
            data.put("userId", session.getUserId());
            data.put("displayName", session.getDisplayName());
            data.put("sessionId", sessionId);

            // Use world data passed from caller
            if (world != null && world.getPublicData() != null) {
                WorldInfo worldInfo = world.getPublicData();

                // Convert WorldInfo to JSON and add settings
                ObjectNode worldInfoNode = objectMapper.valueToTree(worldInfo);

                // Add settings object with pingInterval (not part of WorldInfo)
                ObjectNode settings = objectMapper.createObjectNode();
                settings.put("pingInterval", session.getPingInterval());
                settings.put("maxPlayers", 100);
                settings.put("allowGuests", true);
                worldInfoNode.set("settings", settings);

                data.set("worldInfo", worldInfoNode);
            }
        } else {
            data.put("errorCode", 401);
            data.put("errorMessage", errorMessage != null ? errorMessage : "Authentication failed");
        }

        NetworkMessage response = NetworkMessage.builder()
                .r(requestId)
                .t("loginResponse")
                .d(data)
                .build();

        String json = objectMapper.writeValueAsString(response);
        session.getWebSocketSession().sendMessage(new TextMessage(json));
    }
}
