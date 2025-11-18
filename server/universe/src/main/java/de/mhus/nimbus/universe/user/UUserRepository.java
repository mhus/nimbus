package de.mhus.nimbus.universe.user;

import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UUserRepository extends MongoRepository<UUser, String> {
    Optional<UUser> findByUsername(String username);
    Optional<UUser> findByEmail(String email);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
}

