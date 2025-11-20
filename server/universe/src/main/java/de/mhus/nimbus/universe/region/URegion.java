package de.mhus.nimbus.universe.region;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * API-spezifische Projektion eines Regionen im Universe-Modul.
 * Speicherung der Maintainer analog zur Registry als CSV, mit Hilfsmethoden fr Set-Operationen.
 */
@Getter
@Setter
@NoArgsConstructor
@Document(collection = "uRegions")
public class URegion {

    @Id
    private String id; // ggf. von MongoDB gesetzt
    private String name;
    private String apiUrl;
    private String publicSignKey;

    // Komma-separierte Liste von User-IDs mit MAINTAINER-Rechten
    private String maintainers; // z.B. "u1,u2,u3" oder null

    public URegion(String name, String apiUrl, String publicSignKey) {
        this.name = name;
        this.apiUrl = apiUrl;
        this.publicSignKey = publicSignKey;
    }

    public URegion(String id, String name, String apiUrl, String publicSignKey, Set<String> maintainerSet) {
        this.id = id;
        this.name = name;
        this.apiUrl = apiUrl;
        this.publicSignKey = publicSignKey;
        setMaintainersFromSet(maintainerSet);
    }

    // Normalisiert CSV: Leer oder nur Whitespaces => null
    public void setMaintainers(String csv) {
        if (csv == null || csv.isBlank()) {
            this.maintainers = null;
        } else {
            var set = Arrays.stream(csv.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            this.maintainers = set.isEmpty() ? null : String.join(",", set);
        }
    }

    public void setMaintainersFromSet(Set<String> set) {
        if (set == null || set.isEmpty()) {
            this.maintainers = null;
        } else {
            var norm = set.stream().filter(s -> s != null && !s.isBlank()).map(String::trim)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            this.maintainers = norm.isEmpty() ? null : String.join(",", norm);
        }
    }

    public Set<String> getMaintainerSet() {
        if (maintainers == null || maintainers.isBlank()) return Collections.emptySet();
        return Arrays.stream(maintainers.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    public boolean hasMaintainer(String userId) {
        return userId != null && getMaintainerSet().contains(userId);
    }

    public void addMaintainer(String userId) {
        if (userId == null || userId.isBlank()) return;
        var set = new LinkedHashSet<>(getMaintainerSet());
        if (set.add(userId.trim())) {
            this.maintainers = String.join(",", set);
        }
    }

    public void removeMaintainer(String userId) {
        if (userId == null) return;
        var set = new LinkedHashSet<>(getMaintainerSet());
        if (set.remove(userId)) {
            this.maintainers = set.isEmpty() ? null : String.join(",", set);
        }
    }
}
