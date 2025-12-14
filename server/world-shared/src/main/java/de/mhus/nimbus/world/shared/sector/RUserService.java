package de.mhus.nimbus.world.shared.sector;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import de.mhus.nimbus.generated.configs.Settings;
import de.mhus.nimbus.shared.types.PlayerUser;
import de.mhus.nimbus.shared.user.SectorRoles;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import de.mhus.nimbus.shared.user.RegionRoles;

@Service
@RequiredArgsConstructor
public class RUserService {

    private final RUserRepository repository;

    public RUser createUser(PlayerUser publicData, String email) {
        if (email == null || email.isBlank()) throw new IllegalArgumentException("email is blank");
        if (repository.existsByUsername(publicData.getUserId())) {
            throw new IllegalArgumentException("Username already exists: " + publicData);
        }
        if (repository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email already exists: " + email);
        }
        RUser user = RUser.builder()
                .username(publicData.getUserId())
                .publicData(publicData)
                .email(email)
                .enabled(true)
                .build();
        user.addSectorRole(SectorRoles.USER); // Standardrolle global
        return repository.save(user);
    }

    public Optional<RUser> getByUsername(String username) { return repository.findByUsername(username); }
    public List<RUser> listAll() { return repository.findAll(); }

    public RUser update(String username, String email, String sectorRolesRaw) {
        RUser existing = repository.findByUsername(username)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));
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
    public RUser addSectorRoles(String username, SectorRoles role) {
        RUser existing = repository.findByUsername(username)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));
        if (existing.addSectorRole(role)) existing = repository.save(existing);
        return existing;
    }

    public RUser removeSectorRole(String username, SectorRoles role) {
        RUser existing = repository.findByUsername(username)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));
        if (existing.removeSectorRole(role)) existing = repository.save(existing);
        return existing;
    }

    // Legacy API methods (moved from deprecated RUser methods)
    public List<SectorRoles> getRoles(String username) {
        RUser user = repository.findByUsername(username)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));
        return user.getSectorRoles();
    }

    public boolean addRole(String username, SectorRoles role) {
        RUser user = repository.findByUsername(username)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));
        boolean changed = user.addSectorRole(role);
        if (changed) {
            repository.save(user);
        }
        return changed;
    }

    public boolean removeRole(String username, SectorRoles role) {
        RUser user = repository.findByUsername(username)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));
        boolean changed = user.removeSectorRole(role);
        if (changed) {
            repository.save(user);
        }
        return changed;
    }

    public boolean hasRole(String username, SectorRoles role) {
        RUser user = repository.findByUsername(username)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));
        return user.hasSectorRole(role);
    }

    public String getRolesRaw(String username) {
        RUser user = repository.findByUsername(username)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));
        return user.getSectorRolesRaw();
    }

    public void setRolesRaw(String username, String raw) {
        RUser user = repository.findByUsername(username)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));
        user.setSectorRolesRaw(raw);
        repository.save(user);
    }

    // Region-specific role management
    public Map<String, RegionRoles> getRegionRoles(String username) {
        RUser user = repository.findByUsername(username)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));
        return user.getRegionRoles();
    }

    public void setRegionRoles(String username, Map<String, RegionRoles> roles) {
        RUser user = repository.findByUsername(username)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));
        user.setRegionRoles(roles);
        repository.save(user);
    }

    public RegionRoles getRegionRole(String username, String regionId) {
        RUser user = repository.findByUsername(username)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));
        return user.getRegionRole(regionId);
    }

    public boolean setRegionRole(String username, String regionId, RegionRoles role) {
        RUser user = repository.findByUsername(username)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));
        boolean changed = user.setRegionRole(regionId, role);
        if (changed) {
            repository.save(user);
        }
        return changed;
    }

    public boolean hasRegionRole(String username, String regionId, RegionRoles role) {
        RUser user = repository.findByUsername(username)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));
        return user.hasRegionRole(regionId, role);
    }

    public boolean removeRegionRole(String username, String regionId) {
        RUser user = repository.findByUsername(username)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));
        boolean changed = user.removeRegionRole(regionId);
        if (changed) {
            repository.save(user);
        }
        return changed;
    }

    public List<String> getRegionIdsWithRole(String username, RegionRoles role) {
        RUser user = repository.findByUsername(username)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));
        return user.getRegionIdsWithRole(role);
    }

    public List<String> getUserIdsByRegionRole(String regionId, RegionRoles role) {
        return repository.findAll().stream()
            .filter(user -> user.hasRegionRole(regionId, role))
            .map(RUser::getId)
            .collect(Collectors.toList());
    }

    // User Settings management
    public Map<String, Settings> getUserSettings(String username) {
        RUser user = repository.findByUsername(username)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));
        return user.getUserSettings();
    }

    public Settings getSettingsForClientType(String username, String clientType) {
        RUser user = repository.findByUsername(username)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));
        return user.getSettingsForClientType(clientType);
    }

    public void setSettingsForClientType(String username, String clientType, Settings settings) {
        RUser user = repository.findByUsername(username)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));
        user.setSettingsForClientType(clientType, settings);
        repository.save(user);
    }

    public boolean hasSettingsForClientType(String username, String clientType) {
        RUser user = repository.findByUsername(username)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));
        return user.hasSettingsForClientType(clientType);
    }

    public void setUserSettings(String username, Map<String, Settings> settings) {
        RUser user = repository.findByUsername(username)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));
        user.setUserSettings(settings);
        repository.save(user);
    }

}
