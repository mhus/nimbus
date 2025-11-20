package de.mhus.nimbus.region.registry;

import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RegionRepository extends MongoRepository<Region, String> {
    Optional<Region> findByName(String name);
    Optional<Region> findByApiUrl(String apiUrl);
    boolean existsByName(String name);
    boolean existsByApiUrl(String apiUrl);
}

