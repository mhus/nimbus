package de.mhus.nimbus.world.shared.world;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * MongoDB Repository for WEntityModel entities.
 */
@Repository
public interface WEntityModelRepository extends MongoRepository<WEntityModel, String> {

    Optional<WEntityModel> findByModelId(String modelId);

    List<WEntityModel> findByRegionId(String regionId);

    List<WEntityModel> findByWorldId(String worldId);

    List<WEntityModel> findByEnabled(boolean enabled);

    boolean existsByModelId(String modelId);

    void deleteByModelId(String modelId);
}
