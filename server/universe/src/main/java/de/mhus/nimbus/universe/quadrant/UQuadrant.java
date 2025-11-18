package de.mhus.nimbus.universe.quadrant;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * API-spezifische Projektion eines Quadranten im Universe-Modul.
 * Dient als entkoppelte Darstellung gegenueber dem internen Registry-Quadrant.
 */
@Data
public class UQuadrant {

    private final String id;
    private final String name;
    private final String apiUrl;
    private final String publicSignKey;
    private final Set<String> maintainers; // unveraenderliche Menge

    public UQuadrant(String id, String name, String apiUrl, String publicSignKey, Set<String> maintainers) {
        this.id = id;
        this.name = name;
        this.apiUrl = apiUrl;
        this.publicSignKey = publicSignKey;
        this.maintainers = maintainers == null ? Collections.emptySet() : Collections.unmodifiableSet(new LinkedHashSet<>(maintainers));
    }

    public UQuadrant(String name, String apiUrl, String publicSignKey) {
        this(null, name, apiUrl, publicSignKey, null);
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getApiUrl() { return apiUrl; }
    public String getPublicSignKey() { return publicSignKey; }
    public Set<String> getMaintainers() { return maintainers; }
    public boolean hasMaintainer(String userId) { return userId != null && maintainers.contains(userId); }

    public void setMaintainers(String maintainers) {
        if (maintainers == null || maintainers.isBlank()) {
            this.maintainers.clear();
        } else {
            this.maintainers.clear();
            for (String maintainer : maintainers.split(",")) {
                this.maintainers.add(maintainer.trim());
            }
        }
    }

}

