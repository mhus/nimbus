package de.mhus.nimbus.universe.region;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface URegionRepository extends MongoRepository<URegion, String> {

    boolean existsByName(String name);

    boolean existsByApiUrl(String apiUrl);

    Optional<URegion> findByName(String name);
}
