package de.mhus.nimbus.registry.service;

import de.mhus.nimbus.registry.entity.World;
import de.mhus.nimbus.shared.avro.PlanetWorld;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper zwischen JPA-Entitäten und Avro-Objekten
 */
@Component
public class PlanetEntityMapper {

    /**
     * Konvertiert eine World-Entität zu einem PlanetWorld-Avro-Objekt
     */
    public PlanetWorld toAvro(World world) {
        long lastUpdateTimestamp = world.getLastHealthCheck() != null ?
            world.getLastHealthCheck().toEpochMilli() :
            world.getUpdatedAt().toEpochMilli();

        return PlanetWorld.newBuilder()
                .setWorldId(world.getWorldId())
                .setWorldName(world.getName())
                .setManagementUrl(world.getManagementUrl())
                .setApiUrl(world.getApiUrl())
                .setWebUrl(world.getWebUrl())
                .setStatus(world.getStatus())
                .setLastUpdate(Instant.ofEpochMilli(lastUpdateTimestamp))
                .setMetadata(world.getMetadata())
                .build();
    }

    /**
     * Konvertiert eine Liste von World-Entitäten zu PlanetWorld-Avro-Objekten
     */
    public List<PlanetWorld> toAvro(List<World> worlds) {
        return worlds.stream()
                .map(this::toAvro)
                .collect(Collectors.toList());
    }

    /**
     * Konvertiert ein PlanetWorld-Avro-Objekt zu einer World-Entität
     */
    public World toEntity(PlanetWorld planetWorld) {
        World world = new World();
        world.setWorldId(planetWorld.getWorldId());
        world.setName(planetWorld.getWorldName());
        world.setManagementUrl(planetWorld.getManagementUrl());
        world.setApiUrl(planetWorld.getApiUrl());
        world.setWebUrl(planetWorld.getWebUrl());
        world.setStatus(planetWorld.getStatus());

        if (planetWorld.getLastUpdate() != null) {
            world.setLastHealthCheck(Instant.ofEpochMilli(planetWorld.getLastUpdate().toEpochMilli()));
        }

        if (planetWorld.getMetadata() != null) {
            world.setMetadata(planetWorld.getMetadata());
        }

        return world;
    }

    /**
     * Aktualisiert eine bestehende World-Entität mit Daten aus einem PlanetWorld-Avro-Objekt
     */
    public void updateEntity(World existingWorld, PlanetWorld planetWorld) {
        existingWorld.setName(planetWorld.getWorldName());
        existingWorld.setManagementUrl(planetWorld.getManagementUrl());
        existingWorld.setApiUrl(planetWorld.getApiUrl());
        existingWorld.setWebUrl(planetWorld.getWebUrl());
        existingWorld.setStatus(planetWorld.getStatus());

        if (planetWorld.getLastUpdate() != null) {
            existingWorld.setLastHealthCheck(Instant.ofEpochMilli(planetWorld.getLastUpdate().toEpochMilli()));
        }

        if (planetWorld.getMetadata() != null) {
            existingWorld.getMetadata().clear();
            existingWorld.getMetadata().putAll(planetWorld.getMetadata());
        }
    }
}
