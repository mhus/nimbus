package de.mhus.nimbus.world.player.ws;

import de.mhus.nimbus.world.player.readiness.WebSocketSessionTracker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

/**
 * Main WebSocket handler for world-player.
 * Manages connections and routes messages to appropriate handlers.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class WorldWebSocketHandler extends TextWebSocketHandler {

    private final WebSocketSessionTracker tracker;
    private final SessionManager sessionManager;
    private final MessageRouter messageRouter;
    private final de.mhus.nimbus.world.player.service.EditModeService editModeService;

    @Override
    public void afterConnectionEstablished(WebSocketSession webSocketSession) throws Exception {
        tracker.increment();

        String worldId = extractWorldId(webSocketSession);
        log.info("WebSocket connection established: session={}, worldId={}",
                webSocketSession.getId(), worldId);

        // Create player session
        PlayerSession playerSession = sessionManager.createSession(webSocketSession);
        playerSession.setWorldId(worldId);

        // Send connection confirmation (legacy format for compatibility)
        webSocketSession.sendMessage(new TextMessage(
                String.format("{\"t\":\"connected\",\"d\":{\"worldId\":\"%s\"}}", worldId)));
    }

    @Override
    protected void handleTextMessage(WebSocketSession webSocketSession, TextMessage message) throws Exception {
        // Get player session
        PlayerSession playerSession = sessionManager.getByWebSocketId(webSocketSession.getId())
                .orElse(null);

        if (playerSession == null) {
            log.warn("No session found for WebSocket: {}", webSocketSession.getId());
            webSocketSession.close(CloseStatus.SERVER_ERROR);
            return;
        }

        // Route message to appropriate handler
        messageRouter.route(playerSession, message);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession webSocketSession, CloseStatus status) throws Exception {
        tracker.decrement();
        log.info("WebSocket connection closed: session={}, status={}",
                webSocketSession.getId(), status);

        // Get player session BEFORE removal
        PlayerSession playerSession = sessionManager.getByWebSocketId(webSocketSession.getId())
                .orElse(null);

        // Cleanup overlays if session was in edit mode
        if (playerSession != null && playerSession.isEditMode()) {
            editModeService.cleanupOverlaysAsync(playerSession);
        }

        // Mark session as deprecated first (allows reconnect with same sessionId)
        sessionManager.deprecateSession(webSocketSession.getId());

        // Remove session after grace period (handled by cleanup job)
        // For now, remove immediately
        sessionManager.removeSession(webSocketSession.getId());
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.error("WebSocket transport error: session={}", session.getId(), exception);
        sessionManager.deprecateSession(session.getId());
    }

    private String extractWorldId(WebSocketSession session) {
        // Expected path: /ws/world/{worldId}
        if (session.getUri() == null) return "";
        String path = session.getUri().getPath();
        if (path == null) return "";
        String[] parts = path.split("/");
        if (parts.length >= 4) return parts[3];
        return "";
    }
}
