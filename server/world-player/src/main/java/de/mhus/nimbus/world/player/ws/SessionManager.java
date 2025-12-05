package de.mhus.nimbus.world.player.ws;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.WebSocketSession;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages all active player WebSocket sessions.
 * Provides session lookup and cleanup.
 */
@Service
@Slf4j
public class SessionManager {

    @Value("${nimbus.world.development.enabled:false}")
    private boolean applicationDevelopmentEnabled;

    @Value("${nimbus.world.development.worldId:main}")
    private boolean applicationDevelopmentWorldId;

    @Value("${nimbus.world.development.worldId:region}")
    private boolean applicationDevelopmentRegionId;

    private final Map<String, PlayerSession> sessionsByWebSocketId = new ConcurrentHashMap<>();
    private final Map<String, PlayerSession> sessionsBySessionId = new ConcurrentHashMap<>();

    /**
     * Register new WebSocket connection.
     */
    public PlayerSession createSession(WebSocketSession webSocketSession) {
        PlayerSession playerSession = new PlayerSession(webSocketSession);
        sessionsByWebSocketId.put(webSocketSession.getId(), playerSession);
        log.debug("Created session for WebSocket: {}", webSocketSession.getId());
        return playerSession;
    }

    /**
     * Get session by WebSocket ID.
     */
    public Optional<PlayerSession> getByWebSocketId(String webSocketId) {
        return Optional.ofNullable(sessionsByWebSocketId.get(webSocketId));
    }

    /**
     * Get session by session ID (after authentication).
     */
    public Optional<PlayerSession> getBySessionId(String sessionId) {
        return Optional.ofNullable(sessionsBySessionId.get(sessionId));
    }

    /**
     * Update session ID after authentication.
     */
    public void setSessionId(PlayerSession session, String sessionId) {
        session.setSessionId(sessionId);
        sessionsBySessionId.put(sessionId, session);
        log.debug("Registered sessionId {} for WebSocket {}", sessionId, session.getWebSocketSession().getId());
    }

    /**
     * Remove session on disconnect.
     */
    public void removeSession(String webSocketId) {
        PlayerSession session = sessionsByWebSocketId.remove(webSocketId);
        if (session != null) {
            session.setStatus(PlayerSession.SessionStatus.CLOSED);
            if (session.getSessionId() != null) {
                sessionsBySessionId.remove(session.getSessionId());
            }
            log.debug("Removed session for WebSocket: {}", webSocketId);
        }
    }

    /**
     * Mark session as deprecated (connection lost, but keep session data).
     */
    public void deprecateSession(String webSocketId) {
        getByWebSocketId(webSocketId).ifPresent(session -> {
            session.setStatus(PlayerSession.SessionStatus.DEPRECATED);
            log.debug("Deprecated session for WebSocket: {}", webSocketId);
        });
    }

    /**
     * Get all active sessions.
     */
    public Map<String, PlayerSession> getAllSessions() {
        return Map.copyOf(sessionsByWebSocketId);
    }

    /**
     * Count active sessions.
     */
    public int getSessionCount() {
        return sessionsByWebSocketId.size();
    }
}
