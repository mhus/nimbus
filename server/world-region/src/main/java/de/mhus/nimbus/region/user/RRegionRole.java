package de.mhus.nimbus.region.user;

import de.mhus.nimbus.shared.persistence.ActualSchemaVersion;
import de.mhus.nimbus.shared.user.RegionRoles;
import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.stream.Collectors;

@Document(collection = "region_roles")
@CompoundIndex(def = "{userId:1, regionId:1}", unique = true)
@ActualSchemaVersion("1.0.0")
@Data
public class RRegionRole {

    @Id
    private String id;

    @Indexed
    private String userId;

    @Indexed
    private String regionId;

    @CreatedDate
    private Instant createdAt;

    // Region-spezifische Rollen für diesen User in dieser Region
    private List<RegionRoles> roles; // null oder leer => keine Rollen

    public RRegionRole() { }
    public RRegionRole(String userId, String regionId) {
        this.userId = userId;
        this.regionId = regionId;
    }

    // Zugriffsmethoden (trotz @Data für zusätzliche Logik / Sicherheit)
    public List<RegionRoles> getRoles() {
        return roles == null ? List.of() : List.copyOf(roles);
    }

    public void setRoles(List<RegionRoles> roles) {
        this.roles = (roles == null || roles.isEmpty()) ? null : new ArrayList<>(new LinkedHashSet<>(roles));
    }

    public boolean addRole(RegionRoles role) {
        if (role == null) return false;
        if (roles == null) roles = new ArrayList<>();
        if (roles.contains(role)) return false;
        roles.add(role);
        return true;
    }

    public boolean removeRole(RegionRoles role) {
        if (role == null || roles == null) return false;
        return roles.remove(role);
    }

    public boolean hasRole(RegionRoles role) {
        return role != null && roles != null && roles.contains(role);
    }

    public String getRolesRaw() {
        return roles == null ? "" : roles.stream().map(Enum::name).collect(Collectors.joining(","));
    }

    public void setRolesRaw(String raw) {
        if (raw == null || raw.isBlank()) { roles = null; return; }
        List<RegionRoles> list = new ArrayList<>();
        for (String part : raw.split(",")) {
            String p = part.trim();
            if (p.isEmpty()) continue;
            try { list.add(RegionRoles.valueOf(p)); } catch (IllegalArgumentException ignored) { }
        }
        setRoles(list);
    }
}
