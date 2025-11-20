package de.mhus.nimbus.region.world;

import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RWorldRepository extends MongoRepository<RWorld, String> {
    Optional<RWorld> findByWorldId(String worldId);
    boolean existsByWorldId(String worldId);

    // Membership queries (array contains userId)
    List<RWorld> findByOwnersContaining(String userId);
    List<RWorld> findByEditorsContaining(String userId);
    List<RWorld> findByMaintainersContaining(String userId);
    List<RWorld> findByPlayersContaining(String userId);
}
