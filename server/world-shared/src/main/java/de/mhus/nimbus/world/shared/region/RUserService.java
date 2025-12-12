package de.mhus.nimbus.world.shared.region;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import de.mhus.nimbus.shared.user.SectorRoles;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import de.mhus.nimbus.shared.user.RegionRoles;

@Service
@RequiredArgsConstructor
public class RUserService {

    private final RUserRepository repository;

    public RUser createUser(String username, String email) {
        if (username == null || username.isBlank()) throw new IllegalArgumentException("username is blank");
        if (email == null || email.isBlank()) throw new IllegalArgumentException("email is blank");
        if (repository.existsByUsername(username)) {
            throw new IllegalArgumentException("Username already exists: " + username);
        }
        if (repository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email already exists: " + email);
        }
        RUser user = new RUser(username, email);
        user.addSectorRole(SectorRoles.PLAYER); // Standardrolle global
        return repository.save(user);
    }

    public Optional<RUser> getById(String id) { return repository.findById(id); }
    public Optional<RUser> getByUsername(String username) { return repository.findByUsername(username); }
    public List<RUser> listAll() { return repository.findAll(); }

    public RUser update(String id, String username, String email, String sectorRolesRaw) {
        RUser existing = repository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + id));
        if (username != null && !username.equals(existing.getUsername())) {
            if (repository.existsByUsername(username)) throw new IllegalArgumentException("Username already exists: " + username);
            existing.setUsername(username);
        }
        if (email != null && !email.equals(existing.getEmail())) {
            if (repository.existsByEmail(email)) throw new IllegalArgumentException("Email already exists: " + email);
            existing.setEmail(email);
        }
        if (sectorRolesRaw != null) {
            existing.setSectorRolesRaw(sectorRolesRaw);
        }
        return repository.save(existing);
    }

    // Globale Server-Rollen
    public RUser addSectorRoles(String id, SectorRoles role) {
        RUser existing = repository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + id));
        if (existing.addSectorRole(role)) existing = repository.save(existing);
        return existing;
    }

    public RUser removeSectorRole(String id, SectorRoles role) {
        RUser existing = repository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + id));
        if (existing.removeSectorRole(role)) existing = repository.save(existing);
        return existing;
    }

    // Legacy API methods (moved from deprecated RUser methods)
    public List<SectorRoles> getRoles(String userId) {
        RUser user = repository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        return user.getSectorRoles();
    }

    public boolean addRole(String userId, SectorRoles role) {
        RUser user = repository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        boolean changed = user.addSectorRole(role);
        if (changed) {
            repository.save(user);
        }
        return changed;
    }

    public boolean removeRole(String userId, SectorRoles role) {
        RUser user = repository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        boolean changed = user.removeSectorRole(role);
        if (changed) {
            repository.save(user);
        }
        return changed;
    }

    public boolean hasRole(String userId, SectorRoles role) {
        RUser user = repository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        return user.hasSectorRole(role);
    }

    public String getRolesRaw(String userId) {
        RUser user = repository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        return user.getSectorRolesRaw();
    }

    public void setRolesRaw(String userId, String raw) {
        RUser user = repository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        user.setSectorRolesRaw(raw);
        repository.save(user);
    }

    // Region-specific role management
    public Map<String, RegionRoles> getRegionRoles(String userId) {
        RUser user = repository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        return user.getRegionRoles();
    }

    public void setRegionRoles(String userId, Map<String, RegionRoles> roles) {
        RUser user = repository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        user.setRegionRoles(roles);
        repository.save(user);
    }

    public RegionRoles getRegionRole(String userId, String regionId) {
        RUser user = repository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        return user.getRegionRole(regionId);
    }

    public boolean setRegionRole(String userId, String regionId, RegionRoles role) {
        RUser user = repository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        boolean changed = user.setRegionRole(regionId, role);
        if (changed) {
            repository.save(user);
        }
        return changed;
    }

    public boolean hasRegionRole(String userId, String regionId, RegionRoles role) {
        RUser user = repository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        return user.hasRegionRole(regionId, role);
    }

    public boolean removeRegionRole(String userId, String regionId) {
        RUser user = repository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        boolean changed = user.removeRegionRole(regionId);
        if (changed) {
            repository.save(user);
        }
        return changed;
    }

    public List<String> getRegionIdsWithRole(String userId, RegionRoles role) {
        RUser user = repository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        return user.getRegionIdsWithRole(role);
    }

    public List<String> getUserIdsByRegionRole(String regionId, RegionRoles role) {
        return repository.findAll().stream()
            .filter(user -> user.hasRegionRole(regionId, role))
            .map(RUser::getId)
            .collect(Collectors.toList());
    }
}
