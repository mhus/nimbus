package de.mhus.nimbus.shared.persistence;

import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface SKeyRepository extends CrudRepository<SKey, Long> {

    Optional<SKey> findByTypeAndKindAndName(String type, String kind, String name);
}
