package de.mhus.nimbus.world.player.ws.handlers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import de.mhus.nimbus.generated.network.ClientType;
import de.mhus.nimbus.generated.types.WorldInfo;
import de.mhus.nimbus.shared.types.PlayerId;
import de.mhus.nimbus.shared.types.WorldId;
import de.mhus.nimbus.world.player.service.PlayerService;
import de.mhus.nimbus.world.player.ws.NetworkMessage;
import de.mhus.nimbus.world.player.session.PlayerSession;
import de.mhus.nimbus.world.player.ws.SessionManager;
import de.mhus.nimbus.world.shared.session.WSessionService;
import de.mhus.nimbus.world.shared.world.WWorld;
import de.mhus.nimbus.world.shared.world.WWorldService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.Strings;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;

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
    private final PlayerService playerService;
    private final WSessionService wSessionService;

    @Value("${world.development.enabled:false}")
    private boolean applicationDevelopmentEnabled;

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
        String worldIdStr = data.has("worldId") ? data.get("worldId").asText() : null;
        String clientTypeStr = data.has("clientType") ? data.get("clientType").asText() : "web";
        String existingSessionId = data.has("sessionId") ? data.get("sessionId").asText() : null;

        log.info("Login attempt: player={}, worldId={}, existingSessionId={}",
                username, worldIdStr, existingSessionId);

        // Check if world exists
        var worldId = WorldId.of(worldIdStr);
        if (worldIdStr != null && worldId.isEmpty()) {
            log.warn("Invalid world ID format: {}, login failed", worldIdStr);
            sendLoginResponse(session, message.getI(), false, "Invalid world id", null, null);
            return;
        }

        var clientType = ClientType.valueOf(clientTypeStr.trim().toUpperCase());

        WWorld world = worldService.getByWorldId(worldId.get()).orElse(null);
        if (world == null || world.getPublicData() == null) {
            log.warn("World not found in database: {}, login failed", worldId);
            sendLoginResponse(session, message.getI(), false, "World not found: " + worldId, null, null);
            return;
        }

        var playerId = PlayerId.of(username);
        if (playerId.isEmpty()) { // TODO if sessionId is given, get player from existing session !
            log.warn("Invalid player ID format: {}, login failed", username);
            sendLoginResponse(session, message.getI(), false, "Invalid player", null, null);
            return;
        }
        var player = playerService.getPlayer(playerId.get(), clientType, worldId.get().getRegionId());

        if (applicationDevelopmentEnabled && username != null && password != null) {
            if (Strings.CS.equals(
                    password,
                    player.get().user().getDevelopmentPassword())
            ) {
                var newSessionId = wSessionService.create(
                        worldId.get(),
                        PlayerId.of(player.get().character().getPublicData().getPlayerId()).get()
                ).getId();
                sessionManager.authenticateSession(session, newSessionId, worldId.get(), player.get(), clientType);
            }
        } else {
            sessionManager.authenticateSession(session, existingSessionId, worldId.get(), player.get(), clientType);
        }

        if (!session.isAuthenticated()) {
            sendLoginResponse(session, message.getI(), false, "Invalid credentials", null, null);
            return;
        }

        // Use the actual session ID (may have changed for username/password login)
        String actualSessionId = session.getSessionId();

        // Send success response with world data
        sendLoginResponse(session, message.getI(), true, null, actualSessionId, world);

        log.info("Login successful: user={}, sessionId={}, worldId={}",
                playerId.get(), actualSessionId, worldId.get());
    }

    private void sendLoginResponse(PlayerSession session, String requestId, boolean success,
                                   String errorMessage, String sessionId, WWorld world) throws Exception {
        ObjectNode data = objectMapper.createObjectNode();
        data.put("success", success);

        if (success) {
            data.put("userId", session.getPlayer().user().getUserId());
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
