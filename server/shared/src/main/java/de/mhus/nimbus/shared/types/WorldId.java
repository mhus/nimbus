package de.mhus.nimbus.shared.types;

import lombok.Getter;

import java.util.Optional;

/*
 * WorldId represents a unique identifier for a world in the format
 * "regionId:worldName[:zone][@branch][#instance]".
 * or
 * Every part is a string 'a-zA-Z0-9_-' from 1 to 64 characters.
 */
public class WorldId {
    @Getter
    private String id;
    private String regionId;
    private String branch;
    private String worldName;
    private String zone;
    private String instance;

    public WorldId(String id) {
        this.id = id;
    }

    public String getRegionId() {
        if (regionId == null) {
            parseId();
        }
        return regionId;
    }

    public String getBranch() {
        if (regionId == null) {
            parseId();
        }
        return branch;
    }

    public String getWorldName() {
        if (regionId == null) {
            parseId();
        }
        return worldName;
    }

    public String getZone() {
        if (regionId == null) {
            parseId();
        }
        return zone;
    }

    public String getInstance() {
        if (regionId == null) {
            parseId();
        }
        return instance;
    }

    private void parseId() {
        var string = id;
        if (string.indexOf('#') > 0) {
            var parts = id.split("#", 3); // one mor for garbage
            if (parts.length > 1) {
                instance = parts[1];
            }
            string = parts[0];
        }
        if (string.indexOf('@') > 0) {
            var parts = string.split("@", 3); // one mor for garbage
            if (parts.length > 1) {
                branch = parts[1];
            }
            string = parts[0];
        }
        var parts = string.split(":", 4); // one mor for garbage
        regionId = parts[0];
        worldName = parts[1];
        if (parts.length > 2) {
            zone = parts[2];
        }
    }

    public String toString() {
        return id;
    }

    public static Optional<WorldId> of(String id) {
        if (!validate(id)) return Optional.empty();
        return Optional.of(new WorldId(id));
    }

    public static boolean validate(String id) {
        if (id == null || id.isBlank()) return false;
        if (id.length() < 3) return false;
         // Every part is a string 'a-zA-Z0-9_-' from 1 to 64 characters.
        return id.matches("^[a-zA-Z0-9_\\-]{1,64}:[a-zA-Z0-9_\\-]{1,64}(:[a-zA-Z0-9_\\-]{1,64})?(@[a-zA-Z0-9_\\-]{1,64})?(#[a-zA-Z0-9_\\-]{1,64})?$");
    }

    public boolean isMain() {
        return zone == null && branch == null && instance == null;
    }

    public boolean isBranch() {
        return branch != null;
    }
}
