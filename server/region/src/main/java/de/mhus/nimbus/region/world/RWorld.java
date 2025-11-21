package de.mhus.nimbus.region.world;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Document(collection = "worlds")
public class RWorld {

    @Id
    private String id;

    /**
     * Human readable unique identifier for the world (e.g. slug)
     */
    @Indexed(unique = true)
    private String worldId;

    private String name;
    private String description;

    private String worldApiUrl;
    private String websocketUrl;
    private String controlsUrl;

    private Visibility visibility = Visibility.PRIVATE;
    private String branch; // optional branch name
    private State state = State.WIP;

    @Indexed
    private List<String> owners = new ArrayList<>();
    @Indexed
    private List<String> editors = new ArrayList<>();
    @Indexed
    private List<String> maintainers = new ArrayList<>();
    @Indexed
    private List<String> players = new ArrayList<>();

    private List<EntryPoint> entryPoints = new ArrayList<>();

    @CreatedDate
    private Date createdAt;

    public enum Visibility { PUBLIC, PRIVATE }
    public enum State { READY, ARCHIVED, WIP }

    public static class EntryPoint {
        private String name; // identifier of entrypoint
        private String path; // optional url path or identifier
        private Access access = Access.PRIVATE;
        private boolean main; // marks the main entry point

        public enum Access { PUBLIC, PRIVATE }

        public EntryPoint() {}
        public EntryPoint(String name, String path, Access access, boolean main) {
            this.name = name;
            this.path = path;
            this.access = access == null ? Access.PRIVATE : access;
            this.main = main;
        }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getPath() { return path; }
        public void setPath(String path) { this.path = path; }
        public Access getAccess() { return access; }
        public void setAccess(Access access) { this.access = access; }
        public boolean isMain() { return main; }
        public void setMain(boolean main) { this.main = main; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            EntryPoint that = (EntryPoint) o;
            return Objects.equals(name, that.name);
        }
        @Override
        public int hashCode() { return Objects.hash(name); }
    }

    public RWorld() {}

    public RWorld(String worldId, String name) {
        this.worldId = worldId;
        this.name = name;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getWorldId() { return worldId; }
    public void setWorldId(String worldId) { this.worldId = worldId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getApiUrl() { return worldApiUrl; }
    public void setApiUrl(String apiUrl) { this.worldApiUrl = apiUrl; }

    public String getWebsocketUrl() { return websocketUrl; }
    public void setWebsocketUrl(String websocketUrl) { this.websocketUrl = websocketUrl; }

    public String getControlsUrl() { return controlsUrl; }
    public void setControlsUrl(String controlsUrl) { this.controlsUrl = controlsUrl; }

    public Visibility getVisibility() { return visibility; }
    public void setVisibility(Visibility visibility) { this.visibility = visibility; }

    public String getBranch() { return branch; }
    public void setBranch(String branch) { this.branch = branch; }

    public State getState() { return state; }
    public void setState(State state) { this.state = state; }

    public List<String> getOwners() { return owners; }
    public void setOwners(List<String> owners) { this.owners = owners != null ? new ArrayList<>(owners) : new ArrayList<>(); }

    public List<String> getEditors() { return editors; }
    public void setEditors(List<String> editors) { this.editors = editors != null ? new ArrayList<>(editors) : new ArrayList<>(); }

    public List<String> getMaintainers() { return maintainers; }
    public void setMaintainers(List<String> maintainers) { this.maintainers = maintainers != null ? new ArrayList<>(maintainers) : new ArrayList<>(); }

    public List<String> getPlayers() { return players; }
    public void setPlayers(List<String> players) { this.players = players != null ? new ArrayList<>(players) : new ArrayList<>(); }

    public List<EntryPoint> getEntryPoints() { return entryPoints; }
    public void setEntryPoints(List<EntryPoint> entryPoints) { this.entryPoints = entryPoints != null ? new ArrayList<>(entryPoints) : new ArrayList<>(); }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    // Region-Zuordnung (optional, je nach Einsatz)
    private String regionId;

    // Getter/Setter für regionId
    public String getRegionId() { return regionId; }
    public void setRegionId(String regionId) { this.regionId = regionId; }

    // Convenience methods
    public boolean addOwner(String userId) { return addUnique(owners, userId); }
    public boolean removeOwner(String userId) { return owners != null && owners.remove(userId); }
    public boolean addEditor(String userId) { return addUnique(editors, userId); }
    public boolean removeEditor(String userId) { return editors != null && editors.remove(userId); }
    public boolean addMaintainer(String userId) { return addUnique(maintainers, userId); }
    public boolean removeMaintainer(String userId) { return maintainers != null && maintainers.remove(userId); }
    public boolean addPlayer(String userId) { return addUnique(players, userId); }
    public boolean removePlayer(String userId) { return players != null && players.remove(userId); }

    public boolean upsertEntryPoint(EntryPoint ep) {
        if (ep == null || ep.getName() == null || ep.getName().isBlank()) return false;
        if (entryPoints == null) entryPoints = new ArrayList<>();
        int idx = -1;
        for (int i = 0; i < entryPoints.size(); i++) {
            if (Objects.equals(entryPoints.get(i).getName(), ep.getName())) { idx = i; break; }
        }
        if (idx >= 0) { entryPoints.set(idx, ep); } else { entryPoints.add(ep); }
        return true;
    }
    public boolean removeEntryPoint(String name) {
        if (name == null) return false;
        return entryPoints != null && entryPoints.removeIf(e -> name.equals(e.getName()));
    }

    private boolean addUnique(List<String> list, String value) {
        if (value == null || value.isBlank()) return false;
        if (list == null) list = new ArrayList<>();
        if (list.contains(value)) return false;
        list.add(value);
        return true;
    }
}
