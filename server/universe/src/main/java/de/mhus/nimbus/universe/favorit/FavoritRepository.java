package de.mhus.nimbus.universe.favorit;

import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FavoritRepository extends MongoRepository<Favorit, String> {
    List<Favorit> findByUserId(String userId);
    Optional<Favorit> findByUserIdAndQuadrantIdAndSolarSystemIdAndWorldIdAndEntryPointId(
        String userId, String quadrantId, String solarSystemId, String worldId, String entryPointId);
}

