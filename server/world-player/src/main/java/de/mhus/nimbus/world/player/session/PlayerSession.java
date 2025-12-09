package de.mhus.nimbus.world.player.session;

import de.mhus.nimbus.generated.network.ClientType;
import de.mhus.nimbus.shared.types.PlayerData;
import de.mhus.nimbus.shared.types.WorldId;
import lombok.Data;
import org.springframework.web.socket.WebSocketSession;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

/**
 * Stateful player session for WebSocket connection.
 * Maintains connection state, authentication, and chunk subscriptions.
 */
@Data
public class PlayerSession {

    private final WebSocketSession webSocketSession;

    private String sessionId;
    private PlayerData player;
    private String displayName;
    private WorldId worldId;
    private ClientType clientType;

    private SessionStatus status = SessionStatus.CONNECTED;

    private Instant connectedAt;
    private Instant lastPingAt;
    private Instant authenticatedAt;


    public boolean isAuthenticated() {
        return status == SessionStatus.AUTHENTICATED;
    }

    /**
     * Registered chunks (cx, cz coordinates as "cx:cz" format).
     * Client receives updates only for these chunks.
     */
    private final Set<String> registeredChunks = new HashSet<>();

    /**
     * Ping interval in seconds (from world settings).
     */
    private int pingInterval = 30;

    /**
     * Edit mode flag - when true, session sees overlay blocks from Redis.
     */
    private boolean editMode = false;

    public PlayerSession(WebSocketSession webSocketSession) {
        this.webSocketSession = webSocketSession;
        this.connectedAt = Instant.now();
        this.lastPingAt = Instant.now();
    }

    /**
     * Check if session is still alive based on ping timeout.
     * deadline = lastPingAt + pingInterval*1000 + 10000 (10s buffer)
     */
    public boolean isAlive() {
        if (lastPingAt == null) return false;
        long deadlineMs = lastPingAt.toEpochMilli() + (pingInterval * 1000L) + 10000L;
        return Instant.now().toEpochMilli() < deadlineMs;
    }

    /**
     * Update last ping timestamp.
     */
    public void touch() {
        this.lastPingAt = Instant.now();
    }

    /**
     * Register chunk for updates.
     * @param cx chunk x coordinate
     * @param cz chunk z coordinate
     */
    public void registerChunk(int cx, int cz) {
        registeredChunks.add(chunkKey(cx, cz));
    }

    /**
     * Unregister chunk.
     * @param cx chunk x coordinate
     * @param cz chunk z coordinate
     */
    public void unregisterChunk(int cx, int cz) {
        registeredChunks.remove(chunkKey(cx, cz));
    }

    /**
     * Clear all registered chunks.
     */
    public void clearChunks() {
        registeredChunks.clear();
    }

    /**
     * Check if chunk is registered.
     */
    public boolean isChunkRegistered(int cx, int cz) {
        return registeredChunks.contains(chunkKey(cx, cz));
    }

    private String chunkKey(int cx, int cz) {
        return cx + ":" + cz;
    }

    public enum SessionStatus {
        CONNECTED,      // Connection established, not yet authenticated
        AUTHENTICATED,  // Successfully authenticated
        DEPRECATED,     // Connection lost or closing
        CLOSED          // Connection closed
    }
}
