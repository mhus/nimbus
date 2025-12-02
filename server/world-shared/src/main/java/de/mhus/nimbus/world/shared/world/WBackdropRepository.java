package de.mhus.nimbus.world.shared.world;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * MongoDB Repository for WBackdrop entities.
 */
@Repository
public interface WBackdropRepository extends MongoRepository<WBackdrop, String> {

    Optional<WBackdrop> findByBackdropId(String backdropId);

    List<WBackdrop> findByRegionId(String regionId);

    List<WBackdrop> findByWorldId(String worldId);

    List<WBackdrop> findByEnabled(boolean enabled);

    boolean existsByBackdropId(String backdropId);

    void deleteByBackdropId(String backdropId);
}
