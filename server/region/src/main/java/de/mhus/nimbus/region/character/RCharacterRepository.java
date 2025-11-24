package de.mhus.nimbus.region.character;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RCharacterRepository extends MongoRepository<RCharacter, String> {
    Optional<RCharacter> findByUserIdAndName(String userId, String name);
    boolean existsByUserIdAndName(String userId, String name);
    List<RCharacter> findByUserId(String userId);
}

