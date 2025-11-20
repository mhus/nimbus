package de.mhus.nimbus.region.user;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import de.mhus.nimbus.shared.user.UniverseRoles;

@Document(collection = "users")
public class RUser {

    @Id
    private String id;

    @Indexed(unique = true)
    private String username;

    @Indexed(unique = true)
    private String email;

    @CreatedDate
    private Instant createdAt;

    // Komma-separierte Liste von Rollen-Namen (Enum.name())
    private String roles; // z.B. "USER,ADMIN"

    public RUser() {}

    public RUser(final String username, final String email) {
        this.username = username;
        this.email = email;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    // Rollen-Hilfsmethoden
    public Set<UniverseRoles> getRoles() {
        if (roles == null || roles.isBlank()) return Collections.emptySet();
        return Arrays.stream(roles.split(","))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .map(UniverseRoles::valueOf)
            .collect(Collectors.toCollection(LinkedHashSet::new));
    }
    public void setRoles(Set<UniverseRoles> rolesSet) {
        if (rolesSet == null || rolesSet.isEmpty()) this.roles = null; else this.roles = rolesSet.stream().map(Enum::name).distinct().collect(Collectors.joining(","));
    }
    public void setRoles(UniverseRoles... rolesArray) {
        if (rolesArray == null || rolesArray.length == 0) { this.roles = null; } else { Set<UniverseRoles> set = Arrays.stream(rolesArray).collect(Collectors.toCollection(LinkedHashSet::new)); setRoles(set); }
    }
    public boolean addRole(UniverseRoles role) {
        if (role == null) return false; Set<UniverseRoles> current = new LinkedHashSet<>(getRoles()); boolean added = current.add(role); if (added) setRoles(current); return added;
    }
    public boolean removeRole(UniverseRoles role) {
        if (role == null) return false; Set<UniverseRoles> current = new LinkedHashSet<>(getRoles()); boolean removed = current.remove(role); if (removed) setRoles(current); return removed;
    }
    public boolean hasRole(UniverseRoles role) { return role != null && getRoles().contains(role); }
    public String getRolesRaw() { return roles; }
    public void setRolesRaw(String rolesRaw) { this.roles = (rolesRaw == null || rolesRaw.isBlank()) ? null : rolesRaw.trim(); }
}
