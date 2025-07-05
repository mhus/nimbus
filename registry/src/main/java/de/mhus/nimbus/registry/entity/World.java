package de.mhus.nimbus.registry.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * JPA-Entität für Welten auf Planeten
 */
@Entity
@Table(name = "worlds", indexes = {
    @Index(name = "idx_world_name", columnList = "name"),
    @Index(name = "idx_world_planet_name", columnList = "planet_id, name"),
    @Index(name = "idx_world_status", columnList = "status")
})
public class World {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "world_id", nullable = false, unique = true, length = 100)
    private String worldId;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "planet_id", nullable = false)
    private Planet planet;

    @Column(name = "management_url", nullable = false, length = 500)
    private String managementUrl;

    @Column(name = "api_url", length = 500)
    private String apiUrl;

    @Column(name = "web_url", length = 500)
    private String webUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private de.mhus.nimbus.shared.avro.WorldStatus status;

    @Column(name = "description", length = 1000)
    private String description;

    @Column(name = "world_type", length = 50)
    private String worldType;

    @Column(name = "access_level", length = 50)
    private String accessLevel;

    @Column(name = "last_health_check")
    private Instant lastHealthCheck;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @ElementCollection
    @CollectionTable(name = "world_metadata", joinColumns = @JoinColumn(name = "world_id"))
    @MapKeyColumn(name = "metadata_key")
    @Column(name = "metadata_value")
    private Map<String, String> metadata = new HashMap<>();

    // Constructors
    public World() {}

    public World(String worldId, String name, String managementUrl) {
        this.worldId = worldId;
        this.name = name;
        this.managementUrl = managementUrl;
        this.status = de.mhus.nimbus.shared.avro.WorldStatus.ACTIVE;
        this.lastHealthCheck = Instant.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getWorldId() { return worldId; }
    public void setWorldId(String worldId) { this.worldId = worldId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Planet getPlanet() { return planet; }
    public void setPlanet(Planet planet) { this.planet = planet; }

    public String getManagementUrl() { return managementUrl; }
    public void setManagementUrl(String managementUrl) { this.managementUrl = managementUrl; }

    public String getApiUrl() { return apiUrl; }
    public void setApiUrl(String apiUrl) { this.apiUrl = apiUrl; }

    public String getWebUrl() { return webUrl; }
    public void setWebUrl(String webUrl) { this.webUrl = webUrl; }

    public de.mhus.nimbus.shared.avro.WorldStatus getStatus() { return status; }
    public void setStatus(de.mhus.nimbus.shared.avro.WorldStatus status) { this.status = status; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getWorldType() { return worldType; }
    public void setWorldType(String worldType) { this.worldType = worldType; }

    public String getAccessLevel() { return accessLevel; }
    public void setAccessLevel(String accessLevel) { this.accessLevel = accessLevel; }

    public Instant getLastHealthCheck() { return lastHealthCheck; }
    public void setLastHealthCheck(Instant lastHealthCheck) { this.lastHealthCheck = lastHealthCheck; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public Map<String, String> getMetadata() { return metadata; }
    public void setMetadata(Map<String, String> metadata) { this.metadata = metadata; }

    public void addMetadata(String key, String value) {
        this.metadata.put(key, value);
    }

    public void removeMetadata(String key) {
        this.metadata.remove(key);
    }
}
