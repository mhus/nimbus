package de.mhus.nimbus.shared.persistence;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SKeyRepository extends MongoRepository<SKey, String> {

    Optional<SKey> findByTypeAndKindAndName(String type, String kind, String name);
    void deleteByTypeAndKindAndName(String type, String kind, String name);

    Optional<SKey> findByTypeAndKindAndOwnerAndName(String type, String kind, String owner, String name);
    void deleteByTypeAndKindAndOwnerAndName(String type, String kind, String owner, String name);
}
