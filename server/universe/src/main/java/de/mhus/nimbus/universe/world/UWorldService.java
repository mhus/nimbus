package de.mhus.nimbus.universe.world;

import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.List;
import java.util.Optional;

@Service
@Validated
public class UWorldService {

    private final UWorldRepository repository;

    public UWorldService(UWorldRepository repository) {
        this.repository = repository;
    }

    public UWorld create(String name, String description,
                         String regionId, String planetId, String solarSystemId, String galaxyId,
                         String worldId, String coordinates) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("name must not be blank");
        if (repository.existsByName(name)) throw new IllegalArgumentException("World name exists: " + name);
        UWorld w = new UWorld();
        w.setName(name);
        w.setDescription(description);
        w.setRegionId(regionId);
        w.setPlanetId(planetId);
        w.setSolarSystemId(solarSystemId);
        w.setGalaxyId(galaxyId);
        w.setWorldId(worldId);
        w.setCoordinates(coordinates);
        return repository.save(w);
    }

    public Optional<UWorld> getById(String id) { return repository.findById(id); }
    public List<UWorld> listAll() { return repository.findAll(); }

    public UWorld update(String id, String name, String description,
                         String regionId, String planetId, String solarSystemId, String galaxyId,
                         String worldId, String coordinates) {
        UWorld w = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("World not found: " + id));
        if (name != null && !name.isBlank() && !name.equals(w.getName())) {
            if (repository.existsByName(name)) throw new IllegalArgumentException("World name exists: " + name);
            w.setName(name);
        }
        if (description != null) w.setDescription(description);
        if (regionId != null) w.setRegionId(regionId);
        if (planetId != null) w.setPlanetId(planetId);
        if (solarSystemId != null) w.setSolarSystemId(solarSystemId);
        if (galaxyId != null) w.setGalaxyId(galaxyId);
        if (worldId != null) w.setWorldId(worldId);
        if (coordinates != null) w.setCoordinates(coordinates);
        return repository.save(w);
    }

    public void delete(String id) { repository.deleteById(id); }
}
