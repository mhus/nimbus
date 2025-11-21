package de.mhus.nimbus.universe.world;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UWorldRepository extends MongoRepository<UWorld, String> {
    boolean existsByName(String name);
    java.util.Optional<UWorld> findByRegionIdAndWorldId(String regionId, String worldId);
}
