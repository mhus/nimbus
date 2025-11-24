package de.mhus.nimbus.region.user;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.stream.Collectors;

import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import de.mhus.nimbus.shared.user.RegionRoles;

@Document(collection = "users")
@Data
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

    // Globale Rollen für den Region-Server (nicht pro einzelne Region)
    @Field("roles") // liest alte Property 'roles' weiterhin ein
    private List<RegionRoles> serverRoles; // null oder leer -> keine Rollen

    public RUser() { this.enabled = true; }
    public RUser(final String username, final String email) { this.username = username; this.email = email; this.enabled = true; }

    public boolean isEnabled() { return enabled == null || enabled; }
    public Boolean getEnabledRaw() { return enabled; }
    public void enable() { this.enabled = true; }
    public void disable() { this.enabled = false; }

    // ServerRoles API
    public List<RegionRoles> getServerRoles() { return serverRoles == null ? Collections.emptyList() : Collections.unmodifiableList(serverRoles); }
    public void setServerRoles(List<RegionRoles> roles) { this.serverRoles = (roles == null || roles.isEmpty()) ? null : new ArrayList<>(new LinkedHashSet<>(roles)); }
    public boolean addServerRole(RegionRoles role) {
        if (role == null) return false; if (serverRoles == null) serverRoles = new ArrayList<>(); if (serverRoles.contains(role)) return false; serverRoles.add(role); return true;
    }
    public boolean removeServerRole(RegionRoles role) {
        if (role == null || serverRoles == null) return false; return serverRoles.remove(role);
    }
    public boolean hasServerRole(RegionRoles role) { return role != null && serverRoles != null && serverRoles.contains(role); }

    public String getServerRolesRaw() { return serverRoles == null ? "" : serverRoles.stream().map(Enum::name).collect(Collectors.joining(",")); }
    public void setServerRolesRaw(String raw) {
        if (raw == null || raw.isBlank()) { serverRoles = null; return; }
        List<RegionRoles> list = new ArrayList<>();
        for (String part : raw.split(",")) {
            String p = part.trim(); if (p.isEmpty()) continue;
            try { list.add(RegionRoles.valueOf(p)); } catch (IllegalArgumentException ignored) { }
        }
        setServerRoles(list);
    }

    @Deprecated public List<RegionRoles> getRoles() { return getServerRoles(); }
    @Deprecated public boolean addRole(RegionRoles role) { return addServerRole(role); }
    @Deprecated public boolean removeRole(RegionRoles role) { return removeServerRole(role); }
    @Deprecated public boolean hasRole(RegionRoles role) { return hasServerRole(role); }
    @Deprecated public String getRolesRaw() { return getServerRolesRaw(); }
    @Deprecated public void setRolesRaw(String raw) { setServerRolesRaw(raw); }
}
