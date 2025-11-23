package de.mhus.nimbus.region.registry;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "regions")
@Data
public class RRegion {

    @Id
    private String id;

    @Indexed(unique = true)
    private String name;

    private String apiUrl;

    private boolean enabled = true;

    // Komma-separierte Liste von User-IDs mit MAINTAINER-Rechten für diesen Regionen
    private String maintainers; // z.B. "u1,u2,u3"

    public RRegion() {}

    public RRegion(String name, String apiUrl) {
        this.name = name;
        this.apiUrl = apiUrl;
    }

    public void setMaintainers(String maintainers) { this.maintainers = (maintainers==null||maintainers.isBlank())?null:maintainers.trim(); }
    public java.util.Set<String> getMaintainerSet() {
        if (maintainers == null || maintainers.isBlank()) return java.util.Collections.emptySet();
        return java.util.Arrays.stream(maintainers.split(",")).map(String::trim).filter(s->!s.isEmpty()).collect(java.util.stream.Collectors.toCollection(java.util.LinkedHashSet::new));
    }
    public boolean hasMaintainer(String userId) { return userId != null && getMaintainerSet().contains(userId); }
    public void addMaintainer(String userId) {
        if (userId == null || userId.isBlank()) return;
        java.util.LinkedHashSet<String> set = new java.util.LinkedHashSet<>(getMaintainerSet());
        if (set.add(userId.trim())) maintainers = String.join(",", set);
    }
    public void removeMaintainer(String userId) {
        if (userId == null) return;
        java.util.LinkedHashSet<String> set = new java.util.LinkedHashSet<>(getMaintainerSet());
        if (set.remove(userId)) maintainers = set.isEmpty()?null:String.join(",", set);
    }
}
