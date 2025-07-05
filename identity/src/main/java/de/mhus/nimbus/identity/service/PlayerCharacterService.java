package de.mhus.nimbus.identity.service;

import de.mhus.nimbus.identity.entity.PlayerCharacter;
import de.mhus.nimbus.identity.entity.User;
import de.mhus.nimbus.identity.repository.PlayerCharacterRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Service für PlayerCharacter-Management
 */
@Service
@Transactional
public class PlayerCharacterService {

    private final PlayerCharacterRepository playerCharacterRepository;

    public PlayerCharacterService(PlayerCharacterRepository playerCharacterRepository) {
        this.playerCharacterRepository = playerCharacterRepository;
    }

    /**
     * Erstellt einen neuen PlayerCharacter für einen User
     */
    public PlayerCharacter createPlayerCharacter(User user, String name, String characterClass, String description) {
        if (playerCharacterRepository.existsByName(name)) {
            throw new IllegalArgumentException("Character name already exists: " + name);
        }

        PlayerCharacter character = new PlayerCharacter(name, characterClass, user);
        character.setDescription(description);

        return playerCharacterRepository.save(character);
    }

    /**
     * Findet alle PlayerCharacters eines Users
     */
    @Transactional(readOnly = true)
    public List<PlayerCharacter> findByUser(User user) {
        return playerCharacterRepository.findByUser(user);
    }

    /**
     * Findet alle aktiven PlayerCharacters eines Users
     */
    @Transactional(readOnly = true)
    public List<PlayerCharacter> findActiveCharactersByUser(User user) {
        return playerCharacterRepository.findByUserAndActiveTrue(user);
    }

    /**
     * Findet einen PlayerCharacter anhand der ID
     */
    @Transactional(readOnly = true)
    public Optional<PlayerCharacter> findById(Long id) {
        return playerCharacterRepository.findById(id);
    }

    /**
     * Findet einen PlayerCharacter anhand des Namens
     */
    @Transactional(readOnly = true)
    public Optional<PlayerCharacter> findByName(String name) {
        return playerCharacterRepository.findByName(name);
    }

    /**
     * Aktualisiert die Position eines PlayerCharacters
     */
    public PlayerCharacter updatePosition(Long characterId, String worldId, String planet,
                                        Double x, Double y, Double z) {
        PlayerCharacter character = playerCharacterRepository.findById(characterId)
                .orElseThrow(() -> new IllegalArgumentException("Character not found: " + characterId));

        character.setCurrentWorldId(worldId);
        character.setCurrentPlanet(planet);
        character.setPositionX(x);
        character.setPositionY(y);
        character.setPositionZ(z);

        return playerCharacterRepository.save(character);
    }

    /**
     * Aktualisiert Level und Erfahrungspunkte
     */
    public PlayerCharacter updateLevelAndExperience(Long characterId, Integer level, Long experiencePoints) {
        PlayerCharacter character = playerCharacterRepository.findById(characterId)
                .orElseThrow(() -> new IllegalArgumentException("Character not found: " + characterId));

        character.setLevel(level);
        character.setExperiencePoints(experiencePoints);

        return playerCharacterRepository.save(character);
    }

    /**
     * Aktualisiert Gesundheits- und Manapunkte
     */
    public PlayerCharacter updateHealthAndMana(Long characterId, Integer healthPoints, Integer manaPoints) {
        PlayerCharacter character = playerCharacterRepository.findById(characterId)
                .orElseThrow(() -> new IllegalArgumentException("Character not found: " + characterId));

        character.setHealthPoints(healthPoints);
        character.setManaPoints(manaPoints);

        return playerCharacterRepository.save(character);
    }

    /**
     * Setzt den letzten Login-Zeitstempel
     */
    public PlayerCharacter updateLastLogin(Long characterId) {
        PlayerCharacter character = playerCharacterRepository.findById(characterId)
                .orElseThrow(() -> new IllegalArgumentException("Character not found: " + characterId));

        character.setLastLogin(Instant.now());

        return playerCharacterRepository.save(character);
    }

    /**
     * Deaktiviert einen PlayerCharacter
     */
    public void deactivatePlayerCharacter(Long characterId) {
        PlayerCharacter character = playerCharacterRepository.findById(characterId)
                .orElseThrow(() -> new IllegalArgumentException("Character not found: " + characterId));

        character.setActive(false);
        playerCharacterRepository.save(character);
    }

    /**
     * Reaktiviert einen PlayerCharacter
     */
    public PlayerCharacter reactivatePlayerCharacter(Long characterId) {
        PlayerCharacter character = playerCharacterRepository.findById(characterId)
                .orElseThrow(() -> new IllegalArgumentException("Character not found: " + characterId));

        character.setActive(true);
        return playerCharacterRepository.save(character);
    }

    /**
     * Findet PlayerCharacters auf einem bestimmten Planeten
     */
    @Transactional(readOnly = true)
    public List<PlayerCharacter> findByPlanet(String planet) {
        return playerCharacterRepository.findByCurrentPlanet(planet);
    }

    /**
     * Findet PlayerCharacters in einer bestimmten Welt
     */
    @Transactional(readOnly = true)
    public List<PlayerCharacter> findByWorld(String worldId) {
        return playerCharacterRepository.findByCurrentWorldId(worldId);
    }
}
