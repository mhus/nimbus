package de.mhus.nimbus.universe.user;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import de.mhus.nimbus.shared.persistence.ActualSchemaVersion;
import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import de.mhus.nimbus.shared.user.UniverseRoles; // geändert

@Document(collection = "users")
@ActualSchemaVersion("1.0.0")
@Data
public class UUser {

    @Id
    private String id;

    @Indexed(unique = true)
    private String username;

    @Indexed(unique = true)
    private String email;

    @CreatedDate
    private Instant createdAt;

    private String passwordHash; // stored as algorithm[:saltBase64]:hashBase64

    // Kommaseparierte Liste von Rollen-Namen (Enum.name())
    private List<String> roles; // z.B. "USER,ADMIN"

    private boolean enabled = true;

    public UUser() {}

    public UUser(final String username, final String email) {
        this.username = username;
        this.email = email;
    }

    // Rollen-Hilfsmethoden
    public Set<UniverseRoles> getRoles() {
        if (roles == null || roles.isEmpty()) return Collections.emptySet();
        return roles.stream()
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .map(UniverseRoles::valueOf)
            .collect(Collectors.toCollection(LinkedHashSet::new));
    }
    public void setRoles(Set<UniverseRoles> rolesSet) {
        if (rolesSet == null || rolesSet.isEmpty()) this.roles = null;
        else this.roles = rolesSet.stream().map(Enum::name).distinct().collect(Collectors.toList());
    }
    public void setRoles(UniverseRoles... rolesArray) {
        if (rolesArray == null || rolesArray.length == 0) { this.roles = null; }
        else { Set<UniverseRoles> set = Arrays.stream(rolesArray).collect(Collectors.toCollection(LinkedHashSet::new)); setRoles(set); }
    }
    public boolean addRole(UniverseRoles role) {
        if (role == null) return false;
        Set<UniverseRoles> current = new LinkedHashSet<>(getRoles());
        boolean added = current.add(role);
        if (added) setRoles(current);
        return added;
    }
    public boolean removeRole(UniverseRoles role) {
        if (role == null) return false;
        Set<UniverseRoles> current = new LinkedHashSet<>(getRoles());
        boolean removed = current.remove(role);
        if (removed) setRoles(current);
        return removed;
    }
    public boolean hasRole(UniverseRoles role) { return role != null && getRoles().contains(role); }
    public String getRolesAsString() {
        if (roles == null || roles.isEmpty()) return "";
        return String.join(",", roles);
    }
    public void setRolesStringList(String rolesRaw) {
        if (rolesRaw == null || rolesRaw.trim().isEmpty()) {
            this.roles = null;
        } else {
            this.roles = Arrays.stream(rolesRaw.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
        }
    }

}
