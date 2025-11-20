package de.mhus.nimbus.quadrant.world;

import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface QWorldRepository extends MongoRepository<QWorld, String> {
    Optional<QWorld> findByWorldId(String worldId);
    boolean existsByWorldId(String worldId);

    // Membership queries (array contains userId)
    List<QWorld> findByOwnersContaining(String userId);
    List<QWorld> findByEditorsContaining(String userId);
    List<QWorld> findByMaintainersContaining(String userId);
    List<QWorld> findByPlayersContaining(String userId);
}
