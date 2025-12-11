package de.mhus.nimbus.region.world;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class RWorldService {

    public static class WorldAccess {
        private final RWorld world;
        private final java.util.Set<String> roles;
        public WorldAccess(RWorld world) { this.world = world; this.roles = new java.util.LinkedHashSet<>(); }
        public RWorld getWorld() { return world; }
        public java.util.Set<String> getRoles() { return roles; }
        public void addRole(String role) { if (role!=null) roles.add(role); }
    }

    private final RWorldRepository repository;

    public RWorldService(RWorldRepository repository) {
        this.repository = repository;
    }

    // CRUD
    public RWorld create(String worldId, String name) {
        if (!StringUtils.hasText(worldId)) throw new IllegalArgumentException("worldId is blank");
        if (!StringUtils.hasText(name)) throw new IllegalArgumentException("name is blank");
        if (repository.existsByWorldId(worldId)) throw new IllegalArgumentException("worldId already exists: " + worldId);
        RWorld w = new RWorld(worldId, name);
        return repository.save(w);
    }

    public Optional<RWorld> getById(String id) {
        return repository.findById(id);
    }

    public Optional<RWorld> getByWorldId(String worldId) {
        return repository.findByWorldId(worldId);
    }

    public List<RWorld> listAll() { return repository.findAll(); }

    public void delete(String id) { repository.deleteById(id); }

    /**
     * Returns worlds a user can access and the role(s) they have there.
     * Roles: owner, editor, moderator (mapped from maintainer), player.
     */
    public List<WorldAccess> getAccessForUser(String userId) {
        if (!org.springframework.util.StringUtils.hasText(userId)) return java.util.Collections.emptyList();
        java.util.Map<String, WorldAccess> map = new java.util.LinkedHashMap<>();
        java.util.function.BiConsumer<List<RWorld>, String> addAll = (list, role) -> {
            for (RWorld w : list) {
                WorldAccess wa = map.computeIfAbsent(w.getId(), k -> new WorldAccess(w));
                wa.addRole(role);
            }
        };
        addAll.accept(repository.findByOwnersContaining(userId), "owner");
        addAll.accept(repository.findByEditorsContaining(userId), "editor");
        addAll.accept(repository.findByMaintainersContaining(userId), "moderator");
        addAll.accept(repository.findByPlayersContaining(userId), "player");
        return new java.util.ArrayList<>(map.values());
    }

    public RWorld update(String id,
                         String name,
                         String description,
                         String apiUrl,
                         String websocketUrl,
                         String controlsUrl,
                         RWorld.Visibility visibility,
                         String branch,
                         RWorld.State state,
                         String ownersCsv,
                         String editorsCsv,
                         String maintainersCsv,
                         String playersCsv,
                         List<RWorld.EntryPoint> entryPoints
    ) {
        RWorld existing = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("World not found: " + id));
        if (name != null) existing.setName(name);
        if (description != null) existing.setDescription(description);
        if (apiUrl != null) existing.setApiUrl(apiUrl);
        if (websocketUrl != null) existing.setWebsocketUrl(websocketUrl);
        if (controlsUrl != null) existing.setControlsUrl(controlsUrl);
        if (visibility != null) existing.setVisibility(visibility);
        if (branch != null) existing.setBranch(branch);
        if (state != null) existing.setState(state);
        if (ownersCsv != null) existing.setOwners(csvToList(ownersCsv));
        if (editorsCsv != null) existing.setEditors(csvToList(editorsCsv));
        if (maintainersCsv != null) existing.setMaintainers(csvToList(maintainersCsv));
        if (playersCsv != null) existing.setPlayers(csvToList(playersCsv));
        if (entryPoints != null) existing.setEntryPoints(entryPoints);
        return repository.save(existing);
    }

    // membership helpers
    public RWorld addOwner(String id, String userId) { return updateListMember(id, userId, ListType.OWNERS, true); }
    public RWorld removeOwner(String id, String userId) { return updateListMember(id, userId, ListType.OWNERS, false); }
    public RWorld addEditor(String id, String userId) { return updateListMember(id, userId, ListType.EDITORS, true); }
    public RWorld removeEditor(String id, String userId) { return updateListMember(id, userId, ListType.EDITORS, false); }
    public RWorld addMaintainer(String id, String userId) { return updateListMember(id, userId, ListType.MAINTAINERS, true); }
    public RWorld removeMaintainer(String id, String userId) { return updateListMember(id, userId, ListType.MAINTAINERS, false); }
    public RWorld addPlayer(String id, String userId) { return updateListMember(id, userId, ListType.PLAYERS, true); }
    public RWorld removePlayer(String id, String userId) { return updateListMember(id, userId, ListType.PLAYERS, false); }

    public RWorld upsertEntryPoint(String id, RWorld.EntryPoint ep) {
        RWorld existing = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("World not found: " + id));
        existing.upsertEntryPoint(ep);
        return repository.save(existing);
    }
    public RWorld removeEntryPoint(String id, String name) {
        RWorld existing = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("World not found: " + id));
        existing.removeEntryPoint(name);
        return repository.save(existing);
    }

    private enum ListType { OWNERS, EDITORS, MAINTAINERS, PLAYERS }

    private RWorld updateListMember(String id, String userId, ListType type, boolean add) {
        if (!StringUtils.hasText(userId)) throw new IllegalArgumentException("userId is blank");
        RWorld existing = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("World not found: " + id));
        boolean changed = false;
        switch (type) {
            case OWNERS -> { changed = add ? existing.addOwner(userId) : existing.removeOwner(userId); }
            case EDITORS -> { changed = add ? existing.addEditor(userId) : existing.removeEditor(userId); }
            case MAINTAINERS -> { changed = add ? existing.addMaintainer(userId) : existing.removeMaintainer(userId); }
            case PLAYERS -> { changed = add ? existing.addPlayer(userId) : existing.removePlayer(userId); }
        }
        return changed ? repository.save(existing) : existing;
    }

    // utils
    private List<String> csvToList(String csv) {
        if (!StringUtils.hasText(csv)) return new ArrayList<>();
        Set<String> set = Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toCollection(LinkedHashSet::new));
        return new ArrayList<>(set);
    }
}
