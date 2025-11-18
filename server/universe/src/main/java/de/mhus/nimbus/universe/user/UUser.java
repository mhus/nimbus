package de.mhus.nimbus.universe.user;

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

import de.mhus.nimbus.shared.security.Roles;

@Document(collection = "users")
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
    private String roles; // z.B. "USER,ADMIN"

    public UUser() {}

    public UUser(final String username, final String email) {
        this.username = username;
        this.email = email;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    // ----------------------------------------------------------------------------
    // Rollen-Hilfsmethoden
    // ----------------------------------------------------------------------------

    /**
     * Liefert die Rollen als Set. Leeres Set falls keine Rollen gesetzt.
     */
    public Set<Roles> getRoles() {
        if (roles == null || roles.isBlank()) return Collections.emptySet();
        return Arrays.stream(roles.split(","))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .map(Roles::valueOf)
            .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /**
     * Setzt die Rollen basierend auf einem Set. Bei leer/null wird das Feld gelöscht (null).
     */
    public void setRoles(Set<Roles> rolesSet) {
        if (rolesSet == null || rolesSet.isEmpty()) {
            this.roles = null;
        } else {
            this.roles = rolesSet.stream().map(Enum::name).distinct().collect(Collectors.joining(","));
        }
    }

    /**
     * Komfort-Methode zum Setzen per Varargs.
     */
    public void setRoles(Roles... rolesArray) {
        if (rolesArray == null || rolesArray.length == 0) {
            this.roles = null;
        } else {
            Set<Roles> set = Arrays.stream(rolesArray).collect(Collectors.toCollection(LinkedHashSet::new));
            setRoles(set);
        }
    }

    /**
     * Fügt eine Rolle hinzu. Gibt true zurück falls neu hinzugefügt.
     */
    public boolean addRole(Roles role) {
        if (role == null) return false;
        Set<Roles> current = new LinkedHashSet<>(getRoles());
        boolean added = current.add(role);
        if (added) setRoles(current);
        return added;
    }

    /**
     * Entfernt eine Rolle. Gibt true zurück falls vorhanden war.
     */
    public boolean removeRole(Roles role) {
        if (role == null) return false;
        Set<Roles> current = new LinkedHashSet<>(getRoles());
        boolean removed = current.remove(role);
        if (removed) setRoles(current);
        return removed;
    }

    /**
     * Prüft ob die Rolle gesetzt ist.
     */
    public boolean hasRole(Roles role) {
        if (role == null) return false;
        return getRoles().contains(role);
    }

    /**
     * Rohformat der Rollen (kommasepariert) für Persistenz oder Logging.
     */
    public String getRolesRaw() {
        return roles;
    }

    /**
     * Setzt direkt die kommaseparierte Darstellung. Erwartet korrekte Enum-Namen.
     */
    public void setRolesRaw(String rolesRaw) {
        this.roles = (rolesRaw == null || rolesRaw.isBlank()) ? null : rolesRaw.trim();
    }
}
