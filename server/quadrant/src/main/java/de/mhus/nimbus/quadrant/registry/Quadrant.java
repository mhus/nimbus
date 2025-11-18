package de.mhus.nimbus.quadrant.registry;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "quadrants")
public class Quadrant {

    @Id
    private String id;

    @Indexed(unique = true)
    private String name;

    @Indexed(unique = true)
    private String apiUrl;

    private String publicSignKey;

    // Komma-separierte Liste von User-IDs mit MAINTAINER-Rechten für diesen Quadranten
    private String maintainers; // z.B. "u1,u2,u3"

    public Quadrant() {}

    public Quadrant(String name, String apiUrl, String publicSignKey) {
        this.name = name;
        this.apiUrl = apiUrl;
        this.publicSignKey = publicSignKey;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getApiUrl() { return apiUrl; }
    public void setApiUrl(String apiUrl) { this.apiUrl = apiUrl; }

    public String getPublicSignKey() { return publicSignKey; }
    public void setPublicSignKey(String publicSignKey) { this.publicSignKey = publicSignKey; }

    public String getMaintainers() { return maintainers; }
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
