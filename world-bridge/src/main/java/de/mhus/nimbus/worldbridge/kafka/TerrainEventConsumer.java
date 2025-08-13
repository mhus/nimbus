package de.mhus.nimbus.worldbridge.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.mhus.nimbus.shared.dto.websocket.WebSocketResponse;
import de.mhus.nimbus.worldbridge.model.WebSocketSession;
import de.mhus.nimbus.worldbridge.websocket.WorldBridgeWebSocketHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class TerrainEventConsumer {

    private final WorldBridgeWebSocketHandler webSocketHandler;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @KafkaListener(topics = "world-terrain-events", groupId = "world-bridge")
    public void handleTerrainEvent(String message) {
        try {
            Map<String, Object> event = objectMapper.readValue(message, Map.class);
            String eventType = (String) event.get("eventType");
            String worldId = (String) event.get("worldId");

            log.debug("Received terrain event: {} for world: {}", eventType, worldId);

            // Filter and send to registered clients
            webSocketHandler.getSessionData().forEach((sessionId, sessionInfo) -> {
                if (shouldReceiveEvent(sessionInfo, event, eventType, worldId)) {
                    WebSocketResponse response = WebSocketResponse.builder()
                            .service("terrain")
                            .command("event")
                            .data(event)
                            .status("success")
                            .build();

                    webSocketHandler.sendMessageToSession(sessionId, response);
                }
            });

        } catch (Exception e) {
            log.error("Error processing terrain event", e);
        }
    }

    private boolean shouldReceiveEvent(WebSocketSession sessionInfo, Map<String, Object> event,
                                     String eventType, String worldId) {
        // Check if session is logged in and has selected the correct world
        if (!sessionInfo.isLoggedIn() || !worldId.equals(sessionInfo.getWorldId())) {
            return false;
        }

        // Check if registered for this event type
        if (!sessionInfo.getRegisteredTerrainEvents().contains(eventType)) {
            return false;
        }

        // For cluster-specific events, check cluster registration
        if (isClusterSpecificEvent(eventType)) {
            return isRegisteredForCluster(sessionInfo, event);
        }

        return true;
    }

    private boolean isClusterSpecificEvent(String eventType) {
        return eventType.startsWith("cluster") || eventType.contains("tile") || eventType.contains("sprite");
    }

    private boolean isRegisteredForCluster(WebSocketSession sessionInfo, Map<String, Object> event) {
        Integer eventX = (Integer) event.get("x");
        Integer eventY = (Integer) event.get("y");
        Integer eventLevel = (Integer) event.get("level");

        if (eventX == null || eventY == null || eventLevel == null) {
            return false;
        }

        return sessionInfo.getRegisteredClusters().stream()
                .anyMatch(cluster -> cluster.getX() == eventX &&
                                   cluster.getY() == eventY &&
                                   cluster.getLevel() == eventLevel);
    }
}
