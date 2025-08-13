package de.mhus.nimbus.world.bridge.model;

import lombok.Data;
import lombok.Builder;
import de.mhus.nimbus.shared.dto.worldwebsocket.RegisterClusterCommandData.ClusterCoordinate;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ConcurrentHashMap;

@Data
@Builder
public class WebSocketSession {
    private String sessionId;
    private String userId;
    private String worldId;
    private Set<String> roles;
    private List<ClusterCoordinate> registeredClusters;
    private Set<String> registeredTerrainEvents;
    private StringBuilder messageBuffer; // Buffer für Multi-Line JSON-Kommandos

    public WebSocketSession() {
        this.registeredClusters = new CopyOnWriteArrayList<>();
        this.registeredTerrainEvents = ConcurrentHashMap.newKeySet();
        this.roles = ConcurrentHashMap.newKeySet();
        this.messageBuffer = new StringBuilder();
    }

    public WebSocketSession(String sessionId, String userId, String worldId, Set<String> roles,
                           List<ClusterCoordinate> registeredClusters, Set<String> registeredTerrainEvents) {
        this.sessionId = sessionId;
        this.userId = userId;
        this.worldId = worldId;
        this.roles = roles != null ? roles : ConcurrentHashMap.newKeySet();
        this.registeredClusters = registeredClusters != null ? registeredClusters : new CopyOnWriteArrayList<>();
        this.registeredTerrainEvents = registeredTerrainEvents != null ? registeredTerrainEvents : ConcurrentHashMap.newKeySet();
        this.messageBuffer = new StringBuilder();
    }

    public boolean isLoggedIn() {
        return userId != null;
    }

    public boolean hasWorld() {
        return worldId != null;
    }

    public void clearRegistrations() {
        registeredClusters.clear();
        registeredTerrainEvents.clear();
    }

    // Hilfsmethoden für messageBuffer
    public void appendToMessageBuffer(String line) {
        if (messageBuffer.length() > 0) {
            messageBuffer.append("\n");
        }
        messageBuffer.append(line);
    }

    public String getMessageBuffer() {
        return messageBuffer.toString();
    }

    public void clearMessageBuffer() {
        messageBuffer.setLength(0);
    }

    public boolean hasMessageBuffer() {
        return messageBuffer.length() > 0;
    }
}
