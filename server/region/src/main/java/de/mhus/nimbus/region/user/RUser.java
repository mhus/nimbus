package de.mhus.nimbus.region.user;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import de.mhus.nimbus.shared.user.RegionRoles; // neuer Import

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

    private Boolean enabled; // wenn null -> enabled

    // Neue Rollenstruktur: Liste von RegionRoles
    private List<RegionRoles> roles; // null oder leer -> keine Rollen

    public RUser() { this.enabled = true; }
    public RUser(final String username, final String email) { this.username = username; this.email = email; this.enabled = true; }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public boolean isEnabled() { return enabled == null || enabled; }
    public Boolean getEnabledRaw() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
    public void enable() { this.enabled = true; }
    public void disable() { this.enabled = false; }

    // RegionRoles API
    public List<RegionRoles> getRoles() { return roles == null ? Collections.emptyList() : Collections.unmodifiableList(roles); }
    public void setRoles(List<RegionRoles> roles) { this.roles = (roles == null || roles.isEmpty()) ? null : new ArrayList<>(new LinkedHashSet<>(roles)); }
    public boolean addRole(RegionRoles role) {
        if (role == null) return false; if (roles == null) roles = new ArrayList<>(); if (roles.contains(role)) return false; roles.add(role); return true;
    }
    public boolean removeRole(RegionRoles role) {
        if (role == null || roles == null) return false; return roles.remove(role);
    }
    public boolean hasRole(RegionRoles role) { return role != null && roles != null && roles.contains(role); }

    public String getRolesRaw() { return roles == null ? "" : roles.stream().map(Enum::name).collect(Collectors.joining(",")); }
    public void setRolesRaw(String raw) {
        if (raw == null || raw.isBlank()) { roles = null; return; }
        List<RegionRoles> list = new ArrayList<>();
        for (String part : raw.split(",")) {
            String p = part.trim(); if (p.isEmpty()) continue;
            try { list.add(RegionRoles.valueOf(p)); } catch (IllegalArgumentException ignored) { /* unbekannte Rolle ignorieren */ }
        }
        setRoles(list);
    }
}
