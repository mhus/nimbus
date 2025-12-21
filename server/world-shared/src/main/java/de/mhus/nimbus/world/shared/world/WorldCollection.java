package de.mhus.nimbus.world.shared.world;

import de.mhus.nimbus.shared.types.WorldId;

public record WorldCollection(TYPE type, WorldId worldId, String path) {
    public enum TYPE {
        WORLD,
        REGION,
        PUBLIC,
        SHARED
    }

    public static WorldCollection of(WorldId worldId, String path) {
        if (worldId.isCollection()) {
            var type = switch(worldId.getRegionId()) {
                case WorldId.COLLECTION_REGION -> TYPE.REGION;
                case WorldId.COLLECTION_PUBLIC -> TYPE.PUBLIC;
                default -> TYPE.SHARED;
            };
            int pos = path.indexOf(':');
            if (pos >= 0) path = path.substring(pos + 1);
            return new WorldCollection(type, worldId, path);
        }
        int pos = path.indexOf(':');
        if (pos < 0) {
            if (path.startsWith("w/")) { // legacy support
                path = path.substring(2);
            }
            return new WorldCollection(TYPE.WORLD, worldId, path);
        }
        var group = path.substring(0, pos).toLowerCase();
        path = path.substring(pos + 1);

        switch (group) {
            case "w":
                return new WorldCollection(TYPE.WORLD, worldId, path);
            case "r":
                return new WorldCollection(TYPE.REGION, WorldId.of(WorldId.COLLECTION_REGION, worldId.getRegionId()).get(), path);
            case "p":
                return new WorldCollection(TYPE.PUBLIC, WorldId.of(WorldId.COLLECTION_PUBLIC, worldId.getRegionId()).get(), path);
            default:
                return new WorldCollection(TYPE.SHARED, WorldId.of(WorldId.COLLECTION_SHARED, group).get(), path);
        }
    }

    public String typeString() {
        switch (type) {
            case WORLD:
                return "w";
            case REGION:
                return "r";
            case PUBLIC:
                return "p";
            case SHARED:
                return worldId.getWorldName();
        }
        return "w"; // should not happen
    }

}
