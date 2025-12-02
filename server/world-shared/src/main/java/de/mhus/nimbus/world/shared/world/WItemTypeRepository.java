package de.mhus.nimbus.world.shared.world;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * MongoDB Repository for WItemType entities.
 */
@Repository
public interface WItemTypeRepository extends MongoRepository<WItemType, String> {

    Optional<WItemType> findByItemType(String itemType);

    List<WItemType> findByRegionId(String regionId);

    List<WItemType> findByWorldId(String worldId);

    List<WItemType> findByEnabled(boolean enabled);

    boolean existsByItemType(String itemType);

    void deleteByItemType(String itemType);
}
