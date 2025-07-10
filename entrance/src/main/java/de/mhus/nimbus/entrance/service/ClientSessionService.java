package de.mhus.nimbus.entrance.service;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.WebSocketSession;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * Service zur Verwaltung von Client-Sessions
 */
@Service
@Slf4j
public class ClientSessionService {

    private final Map<String, ClientSession> sessions = new ConcurrentHashMap<>();

    /**
     * Fügt eine neue Session hinzu
     */
    public void addSession(WebSocketSession session) {
        ClientSession clientSession = new ClientSession();
        clientSession.setSession(session);
        clientSession.setConnectedAt(Instant.now());
        clientSession.setAuthenticated(false);

        sessions.put(session.getId(), clientSession);
        log.info("Session {} hinzugefügt. Aktive Sessions: {}", session.getId(), sessions.size());
    }

    /**
     * Entfernt eine Session
     */
    public void removeSession(String sessionId) {
        ClientSession removed = sessions.remove(sessionId);
        if (removed != null) {
            log.info("Session {} entfernt. Aktive Sessions: {}", sessionId, sessions.size());
        }
    }

    /**
     * Markiert eine Session als authentifiziert
     */
    public void authenticateSession(String sessionId, String token, String username) {
        ClientSession session = sessions.get(sessionId);
        if (session != null) {
            session.setAuthenticated(true);
            session.setToken(token);
            session.setUsername(username);
            session.setAuthenticatedAt(Instant.now());
            log.info("Session {} authentifiziert für Benutzer {}", sessionId, username);
        }
    }

    /**
     * Prüft, ob eine Session authentifiziert ist
     */
    public boolean isAuthenticated(String sessionId) {
        ClientSession session = sessions.get(sessionId);
        return session != null && session.isAuthenticated();
    }

    /**
     * Gibt die WebSocket-Session zurück
     */
    public WebSocketSession getWebSocketSession(String sessionId) {
        ClientSession session = sessions.get(sessionId);
        return session != null ? session.getSession() : null;
    }

    /**
     * Gibt die Client-Session zurück
     */
    public ClientSession getClientSession(String sessionId) {
        return sessions.get(sessionId);
    }

    /**
     * Gibt alle aktiven Sessions zurück
     */
    public Map<String, ClientSession> getAllSessions() {
        return new ConcurrentHashMap<>(sessions);
    }

    /**
     * Interne Klasse für Session-Daten
     */
    @Data
    public static class ClientSession {
        private WebSocketSession session;
        private boolean authenticated;
        private String token;
        private String username;
        private Instant connectedAt;
        private Instant authenticatedAt;
        private Instant lastActivity;

        public void updateLastActivity() {
            this.lastActivity = Instant.now();
        }
    }
}
