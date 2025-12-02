package de.mhus.nimbus.world.player.ws.handlers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import de.mhus.nimbus.world.player.ws.NetworkMessage;
import de.mhus.nimbus.world.player.ws.PlayerSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;

/**
 * Handles ping messages from clients.
 * Message type: "p"
 *
 * Clients send regular pings to keep connection alive and measure latency.
 * Server responds with pong including both client and server timestamps.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PingHandler implements MessageHandler {

    private final ObjectMapper objectMapper;

    @Override
    public String getMessageType() {
        return "p";
    }

    @Override
    public void handle(PlayerSession session, NetworkMessage message) throws Exception {
        JsonNode data = message.getD();

        // Extract client timestamp
        long clientTimestamp = data.has("cTs") ? data.get("cTs").asLong() : System.currentTimeMillis();

        // Update session last ping time
        session.touch();

        // Send pong response
        ObjectNode responseData = objectMapper.createObjectNode();
        responseData.put("cTs", clientTimestamp);  // Echo client timestamp
        responseData.put("sTs", System.currentTimeMillis());  // Add server timestamp

        NetworkMessage response = NetworkMessage.builder()
                .r(message.getI())
                .t("p")
                .d(responseData)
                .build();

        String json = objectMapper.writeValueAsString(response);
        session.getWebSocketSession().sendMessage(new TextMessage(json));

        log.trace("Ping/pong: session={}, clientTs={}, latency={}ms",
                session.getWebSocketSession().getId(),
                clientTimestamp,
                System.currentTimeMillis() - clientTimestamp);
    }
}
