package de.mhus.nimbus.world.player.ws;

import de.mhus.nimbus.shared.utils.LocationService;
import de.mhus.nimbus.world.player.session.PlayerSession;
import de.mhus.nimbus.world.shared.session.WSession;
import de.mhus.nimbus.world.shared.session.WSessionService;
import de.mhus.nimbus.world.shared.session.WSessionStatus;
import lombok.RequiredArgsConstructor;
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
 * Synchronizes sessions with Redis via WSessionService.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SessionManager {

    private final WSessionService wSessionService;
    private final LocationService locationService;

    @Value("${world.development.enabled:false}")
    private boolean applicationDevelopmentEnabled;

    @Value("${world.development.worldId:main}")
    private String applicationDevelopmentWorldId;

    @Value("${world.development.regionId:region}")
    private String applicationDevelopmentRegionId;

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
     * Creates or updates WSession in Redis based on authentication type.
     */
    public void setSessionId(PlayerSession session, String sessionId, boolean isUsernamePasswordLogin,
                              String worldId, String regionId, String userId, String characterId) {
        session.setSessionId(sessionId);
        sessionsBySessionId.put(sessionId, session);

        String playerUrl = locationService.getInternalServerUrl();

        if (isUsernamePasswordLogin && applicationDevelopmentEnabled) {
            // Username/password login: Create new WSession in Redis
            WSession wSession = wSessionService.create(
                worldId != null ? worldId : applicationDevelopmentWorldId,
                regionId != null ? regionId : applicationDevelopmentRegionId,
                userId,
                characterId,
                null // use default TTL from WorldProperties
            );
            session.setSessionId(wSession.getId()); // Update to WSession ID
            sessionsBySessionId.remove(sessionId); // Remove temporary ID
            sessionsBySessionId.put(wSession.getId(), session); // Add with WSession ID

            // Update WSession to RUNNING and store player URL
            wSessionService.updateStatus(wSession.getId(), WSessionStatus.RUNNING);
            wSessionService.updatePlayerUrl(wSession.getId(), playerUrl);

            log.info("Created WSession for username/password login: sessionId={}, worldId={}, regionId={}, userId={}, playerUrl={}",
                wSession.getId(), worldId, regionId, userId, playerUrl);
        } else if (isUsernamePasswordLogin) {
            // deny login if not in development mode
            log.error("Username/password login is only allowed in development mode.");
            throw new RuntimeException("Username/password login not allowed");
        } else {
            // Token login: Lookup existing WSession in Redis
            Optional<WSession> wSession = wSessionService.get(sessionId);
            if (wSession.isPresent()) {
                // Always update playerUrl (even if already RUNNING - for reconnects or pod changes)
                wSessionService.updatePlayerUrl(sessionId, playerUrl);

                if (wSession.get().getStatus() == WSessionStatus.WAITING) {
                    // Update WSession to RUNNING
                    wSessionService.updateStatus(sessionId, WSessionStatus.RUNNING);

                    log.info("Updated WSession to RUNNING for token login: sessionId={}, worldId={}, userId={}, playerUrl={}",
                        sessionId, wSession.get().getWorldId(), wSession.get().getUserId(), playerUrl);
                } else {
                    log.debug("WSession already in {} state, updated playerUrl: sessionId={}, playerUrl={}",
                        wSession.get().getStatus(), sessionId, playerUrl);
                }
            } else {
                log.warn("WSession not found for token login: sessionId={}", sessionId);
            }
        }

        log.debug("Registered sessionId {} for WebSocket {}", sessionId, session.getWebSocketSession().getId());
    }

    /**
     * Remove session on disconnect.
     * Updates WSession in Redis to DEPRECATED.
     */
    public void removeSession(String webSocketId) {
        PlayerSession session = sessionsByWebSocketId.remove(webSocketId);
        if (session != null) {
            session.setStatus(PlayerSession.SessionStatus.CLOSED);
            String sessionId = session.getSessionId();
            if (sessionId != null) {
                sessionsBySessionId.remove(sessionId);

                // Update WSession to DEPRECATED in Redis
                wSessionService.updateStatus(sessionId, WSessionStatus.DEPRECATED);
                log.info("Updated WSession to DEPRECATED on disconnect: sessionId={}", sessionId);
            }
            log.debug("Removed session for WebSocket: {}", webSocketId);
        }
    }

    /**
     * Mark session as deprecated (connection lost, but keep session data).
     * Updates WSession in Redis to DEPRECATED.
     */
    public void deprecateSession(String webSocketId) {
        getByWebSocketId(webSocketId).ifPresent(session -> {
            session.setStatus(PlayerSession.SessionStatus.DEPRECATED);

            // Update WSession to DEPRECATED in Redis
            String sessionId = session.getSessionId();
            if (sessionId != null) {
                wSessionService.updateStatus(sessionId, WSessionStatus.DEPRECATED);
                log.info("Updated WSession to DEPRECATED: sessionId={}", sessionId);
            }

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
