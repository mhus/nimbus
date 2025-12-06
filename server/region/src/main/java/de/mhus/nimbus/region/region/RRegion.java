package de.mhus.nimbus.region.region;

import de.mhus.nimbus.shared.persistence.SchemaVersion;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Document(collection = "regions")
@SchemaVersion("1.0.0")
@Data
public class RRegion {

    @Id
    private String id;

    @Indexed(unique = true)
    private String name;

    private boolean enabled = true;

    // Liste von User-IDs mit MAINTAINER-Rechten für diesen Regionen
    private Set<String> maintainers = new HashSet<>(); // z.B. "u1,u2,u3"

    public RRegion() {}

    public RRegion(String name) {
        this.name = name;
    }

    public boolean hasMaintainer(String userId) {
        return userId != null && maintainers != null && maintainers.contains(userId);
    }

    public void addMaintainer(String userId) {
        if (userId == null || userId.isBlank()) return;
        if (maintainers == null) maintainers = new HashSet<>();
        maintainers.add(userId);
    }

    public void removeMaintainer(String userId) {
        if (userId == null) return;
        if (maintainers == null) maintainers = new HashSet<>();
        maintainers.remove(userId);
    }
}
