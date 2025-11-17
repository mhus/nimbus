package de.mhus.nimbus.quadrant.registry;

import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface QuadrantRepository extends MongoRepository<Quadrant, String> {
    Optional<Quadrant> findByName(String name);
    Optional<Quadrant> findByApiUrl(String apiUrl);
    boolean existsByName(String name);
    boolean existsByApiUrl(String apiUrl);
}

