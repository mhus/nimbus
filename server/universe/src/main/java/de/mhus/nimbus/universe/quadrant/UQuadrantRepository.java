package de.mhus.nimbus.universe.quadrant;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UQuadrantRepository extends MongoRepository<UQuadrant, String> {

    boolean existsByName(String name);

    boolean existsByApiUrl(String apiUrl);

    Optional<UQuadrant> findByName(String name);
}
