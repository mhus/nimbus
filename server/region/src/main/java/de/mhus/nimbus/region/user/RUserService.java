package de.mhus.nimbus.region.user;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import de.mhus.nimbus.shared.user.RegionRoles;

@Service
@Validated
public class RUserService {

    private final RUserRepository repository;
    private final RRegionRoleRepository regionRoleRepository;

    public RUserService(RUserRepository repository, RRegionRoleRepository regionRoleRepository) {
        this.repository = repository;
        this.regionRoleRepository = regionRoleRepository;
    }

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
        user.addServerRole(RegionRoles.PLAYER); // Standardrolle global
        return repository.save(user);
    }

    public Optional<RUser> getById(String id) { return repository.findById(id); }
    public Optional<RUser> getByUsername(String username) { return repository.findByUsername(username); }
    public List<RUser> listAll() { return repository.findAll(); }

    public RUser update(String id, String username, String email, String serverRolesRaw) {
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
        if (serverRolesRaw != null) {
            existing.setServerRolesRaw(serverRolesRaw);
        }
        return repository.save(existing);
    }

    public void deleteById(String id) {
        repository.deleteById(id);
        // RegionRole Dokumente bereinigen (Bulk)
        var regionRoles = regionRoleRepository.findByUserId(id);
        if (!regionRoles.isEmpty()) regionRoleRepository.deleteAll(regionRoles);
    }

    // Globale Server-Rollen
    public RUser addServerRole(String id, RegionRoles role) {
        RUser existing = repository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + id));
        if (existing.addServerRole(role)) existing = repository.save(existing);
        return existing;
    }

    public RUser removeServerRole(String id, RegionRoles role) {
        RUser existing = repository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + id));
        if (existing.removeServerRole(role)) existing = repository.save(existing);
        return existing;
    }

    // Region-spezifische Rollen
    public RRegionRole assignRolesToRegion(String userId, String regionId, List<RegionRoles> roles) {
        if (userId == null || regionId == null) throw new IllegalArgumentException("userId/regionId null");
        repository.findById(userId).orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        RRegionRole rr = regionRoleRepository.findByUserIdAndRegionId(userId, regionId).orElse(new RRegionRole(userId, regionId));
        rr.setRoles(roles);
        return regionRoleRepository.save(rr);
    }

    public RRegionRole addRoleToRegion(String userId, String regionId, RegionRoles role) {
        if (role == null) throw new IllegalArgumentException("role null");
        repository.findById(userId).orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        RRegionRole rr = regionRoleRepository.findByUserIdAndRegionId(userId, regionId).orElse(new RRegionRole(userId, regionId));
        if (rr.addRole(role)) rr = regionRoleRepository.save(rr);
        return rr;
    }

    public RRegionRole removeRoleFromRegion(String userId, String regionId, RegionRoles role) {
        if (role == null) throw new IllegalArgumentException("role null");
        RRegionRole rr = regionRoleRepository.findByUserIdAndRegionId(userId, regionId)
                .orElseThrow(() -> new IllegalArgumentException("Region role not found for user=" + userId + " region=" + regionId));
        if (rr.removeRole(role)) rr = regionRoleRepository.save(rr);
        return rr;
    }

    public List<RRegionRole> listRegionsForUser(String userId) {
        return regionRoleRepository.findByUserId(userId);
    }

    public List<RRegionRole> listUsersByRegion(String regionId) {
        return regionRoleRepository.findByRegionId(regionId);
    }

    public List<String> listUserIdsByRegionAndRole(String regionId, RegionRoles role) {
        return regionRoleRepository.findByRegionIdAndRolesIn(regionId, List.of(role))
                .stream().map(RRegionRole::getUserId).collect(Collectors.toList());
    }
}
