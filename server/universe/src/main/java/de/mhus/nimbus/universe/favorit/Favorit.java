package de.mhus.nimbus.universe.favorit;

import java.time.Instant;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "favorits")
public class Favorit {

    @Id
    private String id;

    @Indexed
    private String userId;

    @Indexed
    private String quadrantId;

    private String solarSystemId;
    private String worldId;
    private String entryPointId;
    private String title;

    @CreatedDate
    private Instant createdAt;
    private Instant lastAccessAt;

    private boolean favorit; // true if marked as favorite

    public Favorit() {}

    public Favorit(String userId, String quadrantId, String solarSystemId, String worldId, String entryPointId, String title, boolean favorit) {
        this.userId = userId;
        this.quadrantId = quadrantId;
        this.solarSystemId = solarSystemId;
        this.worldId = worldId;
        this.entryPointId = entryPointId;
        this.title = title;
        this.favorit = favorit;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getQuadrantId() { return quadrantId; }
    public void setQuadrantId(String quadrantId) { this.quadrantId = quadrantId; }

    public String getSolarSystemId() { return solarSystemId; }
    public void setSolarSystemId(String solarSystemId) { this.solarSystemId = solarSystemId; }

    public String getWorldId() { return worldId; }
    public void setWorldId(String worldId) { this.worldId = worldId; }

    public String getEntryPointId() { return entryPointId; }
    public void setEntryPointId(String entryPointId) { this.entryPointId = entryPointId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getLastAccessAt() { return lastAccessAt; }
    public void setLastAccessAt(Instant lastAccessAt) { this.lastAccessAt = lastAccessAt; }

    public boolean isFavorit() { return favorit; }
    public void setFavorit(boolean favorit) { this.favorit = favorit; }
}

