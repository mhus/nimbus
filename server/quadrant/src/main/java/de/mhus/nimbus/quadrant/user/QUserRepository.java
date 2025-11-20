package de.mhus.nimbus.quadrant.user;

import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface QUserRepository extends MongoRepository<QUser, String> {
    Optional<QUser> findByUsername(String username);
    Optional<QUser> findByEmail(String email);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
}
