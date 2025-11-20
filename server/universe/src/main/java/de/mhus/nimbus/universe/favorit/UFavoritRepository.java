package de.mhus.nimbus.universe.favorit;

import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UFavoritRepository extends MongoRepository<UFavorit, String> {
    List<UFavorit> findByUserId(String userId);
    Optional<UFavorit> findByUserIdAndRegionIdAndSolarSystemIdAndWorldIdAndEntryPointId(
        String userId, String regionId, String solarSystemId, String worldId, String entryPointId);
}

