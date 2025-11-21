package de.mhus.nimbus.shared.persistence;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SKeyRepository extends MongoRepository<SKey, String> {

    Optional<SKey> findByTypeAndKindAndName(String type, String kind, String name);
}
