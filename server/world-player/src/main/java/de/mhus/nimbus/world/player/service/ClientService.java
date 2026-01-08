package de.mhus.nimbus.world.player.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import de.mhus.nimbus.world.player.session.PlayerSession;
import de.mhus.nimbus.world.player.ws.NetworkMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;

/**
 * Client communication service for world-player.
 * Handles sending messages and commands to connected clients via WebSocket.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ClientService {

    private final ObjectMapper objectMapper;

    /**
     * Send server command to client via WebSocket.
     * Server Command Message: type="scmd", cmd=commandName, d=commandData.
     *
     * @param session PlayerSession
     * @param commandName Command name (e.g., "redirect")
     * @param commandData Command data (JsonNode with cmd, args, oneway, etc.)
     */
    public void sendCommand(PlayerSession session, String commandName, JsonNode commandData) {
        try {
            // Build network message
            NetworkMessage message = NetworkMessage.builder()
                    .t("scmd")
                    .d(commandData)
                    .build();

            // Send message
            String json = objectMapper.writeValueAsString(message);
            session.getWebSocketSession().sendMessage(new TextMessage(json));

            log.info("Sent server command to player: cmd={}, sessionId={}",
                    commandName, session.getWebSocketSession().getId());

        } catch (Exception e) {
            log.error("Failed to send server command to player: cmd={}", commandName, e);
        }
    }

    /**
     * Send server command with arguments to client via WebSocket.
     * Convenience method for sending commands with a list of string arguments.
     *
     * @param session PlayerSession
     * @param commandName Command name
     * @param args Command arguments
     */
    public void sendCommand(PlayerSession session, String commandName, java.util.List<String> args) {
        ObjectNode commandData = objectMapper.createObjectNode();
        commandData.put("cmd", commandName);
        commandData.set("args", objectMapper.valueToTree(args));

        sendCommand(session, commandName, commandData);
    }

    /**
     * Send redirect command to client via WebSocket.
     * Convenience method for sending redirect commands.
     *
     * @param session PlayerSession
     * @param url Redirect URL
     */
    public void sendRedirectCommand(PlayerSession session, String url) {
        ObjectNode commandData = objectMapper.createObjectNode();
        commandData.put("cmd", "redirect");
        commandData.set("args", objectMapper.createArrayNode().add(url));
        commandData.put("oneway", true); // No response expected

        sendCommand(session, "redirect", commandData);
    }
}
