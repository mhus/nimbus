package de.mhus.nimbus.identity.repository;

import de.mhus.nimbus.identity.entity.PlayerCharacter;
import de.mhus.nimbus.identity.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository für PlayerCharacter-Entity
 */
@Repository
public interface PlayerCharacterRepository extends JpaRepository<PlayerCharacter, Long> {

    /**
     * Findet alle PlayerCharacters eines bestimmten Users
     */
    List<PlayerCharacter> findByUser(User user);

    /**
     * Findet alle aktiven PlayerCharacters eines Users
     */
    List<PlayerCharacter> findByUserAndActiveTrue(User user);

    /**
     * Findet einen PlayerCharacter anhand des Namens
     */
    Optional<PlayerCharacter> findByName(String name);

    /**
     * Findet PlayerCharacters auf einem bestimmten Planeten
     */
    List<PlayerCharacter> findByCurrentPlanet(String currentPlanet);

    /**
     * Findet PlayerCharacters in einer bestimmten Welt
     */
    List<PlayerCharacter> findByCurrentWorldId(String currentWorldId);

    /**
     * Prüft ob ein Character-Name bereits existiert
     */
    boolean existsByName(String name);

    /**
     * Findet PlayerCharacters eines Users nach Level-Bereich
     */
    List<PlayerCharacter> findByUserAndLevelBetween(User user, Integer minLevel, Integer maxLevel);

    /**
     * Findet PlayerCharacters nach Klasse
     */
    List<PlayerCharacter> findByCharacterClass(String characterClass);
}
