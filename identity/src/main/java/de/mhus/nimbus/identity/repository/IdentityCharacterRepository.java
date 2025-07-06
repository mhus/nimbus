package de.mhus.nimbus.identity.repository;

import de.mhus.nimbus.identity.entity.IdentityCharacter;
import de.mhus.nimbus.identity.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository für IdentityCharacter-Entity
 */
@Repository
public interface IdentityCharacterRepository extends JpaRepository<IdentityCharacter, Long> {

    /**
     * Findet alle IdentityCharacters eines bestimmten Users
     */
    List<IdentityCharacter> findByUser(User user);

    /**
     * Findet alle aktiven IdentityCharacters eines Users
     */
    List<IdentityCharacter> findByUserAndActiveTrue(User user);

    /**
     * Findet einen IdentityCharacter anhand des Namens
     */
    Optional<IdentityCharacter> findByName(String name);

    /**
     * Findet IdentityCharacters auf einem bestimmten Planeten
     */
    List<IdentityCharacter> findByCurrentPlanet(String currentPlanet);

    /**
     * Findet IdentityCharacters in einer bestimmten Welt
     */
    List<IdentityCharacter> findByCurrentWorldId(String currentWorldId);

    /**
     * Prüft ob ein Character-Name bereits existiert
     */
    boolean existsByName(String name);

    /**
     * Findet IdentityCharacters eines Users nach Level-Bereich
     */
    List<IdentityCharacter> findByUserAndLevelBetween(User user, Integer minLevel, Integer maxLevel);

    /**
     * Findet IdentityCharacters nach Klasse
     */
    List<IdentityCharacter> findByCharacterClass(String characterClass);
}
