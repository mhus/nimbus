package de.mhus.nimbus.world.shared.world;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WWorldRepository extends MongoRepository<WWorld, String> {
    Optional<WWorld> findByWorldId(String worldId);
    boolean existsByWorldId(String worldId);
    List<WWorld> findByRegionId(String regionId);
}

