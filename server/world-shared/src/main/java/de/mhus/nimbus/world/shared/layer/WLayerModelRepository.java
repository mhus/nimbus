package de.mhus.nimbus.world.shared.layer;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for WLayerModel entities.
 */
@Repository
public interface WLayerModelRepository extends MongoRepository<WLayerModel, String> {

    /**
     * Find model by layerDataId (1:1 relationship).
     */
    Optional<WLayerModel> findByLayerDataId(String layerDataId);

    /**
     * Find all models for a world.
     */
    List<WLayerModel> findByWorldId(String worldId);

    /**
     * Delete model by layerDataId.
     */
    void deleteByLayerDataId(String layerDataId);
}
