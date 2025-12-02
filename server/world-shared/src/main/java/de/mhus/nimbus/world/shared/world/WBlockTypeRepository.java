package de.mhus.nimbus.world.shared.world;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * MongoDB Repository for WBlockType entities.
 */
@Repository
public interface WBlockTypeRepository extends MongoRepository<WBlockType, String> {

    Optional<WBlockType> findByBlockId(String blockId);

    List<WBlockType> findByBlockTypeGroup(String blockTypeGroup);

    List<WBlockType> findByRegionId(String regionId);

    List<WBlockType> findByWorldId(String worldId);

    List<WBlockType> findByEnabled(boolean enabled);

    boolean existsByBlockId(String blockId);

    void deleteByBlockId(String blockId);
}
