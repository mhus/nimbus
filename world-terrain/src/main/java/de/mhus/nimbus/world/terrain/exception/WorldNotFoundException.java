package de.mhus.nimbus.world.terrain.exception;

public class WorldNotFoundException extends RuntimeException {
    public WorldNotFoundException(String worldId) {
        super("World not found: " + worldId);
    }
}
