package de.mhus.nimbus.region.registry;

import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RRegionRepository extends MongoRepository<RRegion, String> {
    Optional<RRegion> findByName(String name);
    Optional<RRegion> findByApiUrl(String apiUrl);
    boolean existsByName(String name);
    boolean existsByApiUrl(String apiUrl);

    List<String> findAllIds();
}

