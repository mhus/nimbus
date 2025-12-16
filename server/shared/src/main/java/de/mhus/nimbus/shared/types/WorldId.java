package de.mhus.nimbus.shared.types;

import lombok.Getter;

import java.util.Optional;

/*
 * WorldId represents a unique identifier for a world in the format
 * "regionId:worldName[:zone][@branch][!instance]".
 * or
 * @collection:collectinId
 * Every part is a string 'a-zA-Z0-9_-' from 1 to 64 characters.
 */
public class WorldId implements Comparable<WorldId> {
    public static final String COLLECTION_REGION = "@region";
    public static final String COLLECTION_SHARED = "@shared";
    public static final String COLLECTION_PUBLIC = "@public";

    @Getter
    private String id;
    private String regionId;
    private String branch;
    private String worldName;
    private String zone;
    private String instance;

    private WorldId(String id) {
        this.id = id;
    }

    public static WorldId unchecked(String worldId) {
        if (worldId == null) throw new NullPointerException("worldId is null");
        return new WorldId(worldId);
    }

    public String getRegionId() {
        parseId();
        return regionId;
    }

    public String getBranch() {
        parseId();
        return branch;
    }

    public String getWorldName() {
        parseId();
        return worldName;
    }

    public String getZone() {
        parseId();
        return zone;
    }

    public String getInstance() {
        parseId();
        return instance;
    }

    public boolean isCollection() {
        return id.startsWith("@");
    }

    private void parseId() { // no need for sync, worst case double parse
        if (regionId != null) return;
        var string = id;
        if (string.startsWith("@")) {
            // Collection ID
            var parts = string.split(":", 3); // one more for garbage
            regionId = parts[0];
            worldName = parts[1];
            branch = null;
            zone = null;
            instance = null;
            return;
        }
        if (string.indexOf('!') > 0) {
            var parts = id.split("!", 3); // one more for garbage
            if (parts.length > 1) {
                instance = parts[1];
            }
            string = parts[0];
        }
        if (string.indexOf('@') > 0) {
            var parts = string.split("@", 3); // one more for garbage
            if (parts.length > 1) {
                branch = parts[1];
            }
            string = parts[0];
        }
        var parts = string.split(":", 4); // one more for garbage
        regionId = parts[0];
        worldName = parts[1];
        if (parts.length > 2) {
            zone = parts[2];
        }
    }

    public String toString() {
        return id;
    }

    public static Optional<WorldId> of(String first, String second) {
        return of(first + ":" + second);
    }

    public static Optional<WorldId> of(String id) {
        if (!validate(id)) return Optional.empty();
        return Optional.of(new WorldId(id));
    }

    public static boolean validate(String id) {
        if (id == null || id.isBlank()) return false;
        if (id.length() < 3) return false;
        if (id.startsWith("@")) {
            // Collection ID
            return id.matches("^@[a-zA-Z0-9_\\-]{1,64}:[a-zA-Z0-9_\\-]{1,64}$");
        }
         // Every part is a string 'a-zA-Z0-9_-' from 1 to 64 characters.
        return id.matches("^[a-zA-Z0-9_\\-]{1,64}:[a-zA-Z0-9_\\-]{1,64}(:[a-zA-Z0-9_\\-]{1,64})?(@[a-zA-Z0-9_\\-]{1,64})?(![a-zA-Z0-9_\\-]{1,64})?$");
    }

    public boolean isMain() {
        parseId();
        return zone == null && branch == null && instance == null;
    }

    public boolean isBranch() {
        parseId();
        return branch != null;
    }

    public boolean isInstance() {
        parseId();
        return instance != null;
    }

    public boolean isZone() {
        parseId();
        return zone != null;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WorldId worldId = (WorldId) o;
        return id.equals(worldId.id);
    }

    @Override
    public int compareTo(WorldId o) {
        return this.id.compareTo(o.id);
    }

    public WorldId withoutInstance() {
        parseId();
        if (instance == null) return this;
        StringBuilder sb = new StringBuilder();
        sb.append(regionId).append(":").append(worldName);
        if (zone != null) sb.append(":").append(zone);
        if (branch != null) sb.append("@").append(branch);
        return new WorldId(sb.toString());
    }

    public WorldId withoutBranchAndInstance() {
        parseId();
        if (branch == null && instance == null) return this;
        StringBuilder sb = new StringBuilder();
        sb.append(regionId).append(":").append(worldName);
        if (zone != null) sb.append(":").append(zone);
        return new WorldId(sb.toString());
    }

    public WorldId withoutInstanceAndZone() {
        parseId();
        if (instance == null && zone == null) return this;
        StringBuilder sb = new StringBuilder();
        sb.append(regionId).append(":").append(worldName);
        if (branch != null) sb.append("@").append(branch);
        return new WorldId(sb.toString());
    }
}
