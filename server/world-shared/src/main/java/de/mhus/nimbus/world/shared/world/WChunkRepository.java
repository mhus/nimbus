package de.mhus.nimbus.world.shared.world;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WChunkRepository extends MongoRepository<WChunk, String> {
    Optional<WChunk> findByRegionIdAndWorldIdAndChunk(String regionId, String worldId, String chunk);
    boolean existsByRegionIdAndWorldIdAndChunk(String regionId, String worldId, String chunk);
    void deleteByRegionIdAndWorldIdAndChunk(String regionId, String worldId, String chunk);
}

