package de.mhus.nimbus.world.player.ws;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.mhus.nimbus.world.player.ws.handlers.MessageHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Routes incoming WebSocket messages to appropriate handlers.
 */
@Service
@Slf4j
public class MessageRouter {

    private final ObjectMapper objectMapper;
    private final Map<String, MessageHandler> handlers;

    public MessageRouter(ObjectMapper objectMapper, List<MessageHandler> handlerList) {
        this.objectMapper = objectMapper;
        this.handlers = handlerList.stream()
                .collect(Collectors.toMap(MessageHandler::getMessageType, Function.identity()));
        log.info("Registered {} message handlers: {}", handlers.size(), handlers.keySet());
    }

    /**
     * Route incoming text message to appropriate handler.
     *
     * @param session  Player session
     * @param message  Raw WebSocket message
     */
    public void route(PlayerSession session, TextMessage message) {
        try {
            String payload = message.getPayload();
            log.debug("Received message from {}: {}", session.getWebSocketSession().getId(),
                     payload.length() > 200 ? payload.substring(0, 200) + "..." : payload);

            // Parse network message
            NetworkMessage networkMessage = objectMapper.readValue(payload, NetworkMessage.class);

            if (networkMessage.getT() == null) {
                log.warn("Message without type from {}", session.getWebSocketSession().getId());
                sendError(session, networkMessage.getI(), "Missing message type");
                return;
            }

            // Find handler
            MessageHandler handler = handlers.get(networkMessage.getT());
            if (handler == null) {
                log.warn("No handler for message type: {}", networkMessage.getT());
                sendError(session, networkMessage.getI(), "Unknown message type: " + networkMessage.getT());
                return;
            }

            // Handle message
            handler.handle(session, networkMessage);

        } catch (Exception e) {
            log.error("Error routing message from {}", session.getWebSocketSession().getId(), e);
            try {
                sendError(session, null, "Internal server error: " + e.getMessage());
            } catch (Exception ex) {
                log.error("Failed to send error response", ex);
            }
        }
    }

    /**
     * Send error response to client.
     */
    private void sendError(PlayerSession session, String requestId, String errorMessage) throws Exception {
        NetworkMessage response = NetworkMessage.builder()
                .r(requestId)
                .t("error")
                .d(objectMapper.createObjectNode()
                        .put("error", errorMessage))
                .build();
        String json = objectMapper.writeValueAsString(response);
        session.getWebSocketSession().sendMessage(new TextMessage(json));
    }
}
