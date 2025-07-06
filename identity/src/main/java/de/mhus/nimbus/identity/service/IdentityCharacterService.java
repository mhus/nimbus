package de.mhus.nimbus.identity.service;

import de.mhus.nimbus.identity.entity.IdentityCharacter;
import de.mhus.nimbus.identity.entity.User;
import de.mhus.nimbus.identity.repository.IdentityCharacterRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Service für IdentityCharacter-Management
 */
@Service
@Transactional
public class IdentityCharacterService {

    private final IdentityCharacterRepository identityCharacterRepository;

    public IdentityCharacterService(IdentityCharacterRepository identityCharacterRepository) {
        this.identityCharacterRepository = identityCharacterRepository;
    }

    /**
     * Erstellt einen neuen IdentityCharacter für einen User
     */
    public IdentityCharacter createIdentityCharacter(User user, String name, String characterClass, String description) {
        if (identityCharacterRepository.existsByName(name)) {
            throw new IllegalArgumentException("Character name already exists: " + name);
        }

        IdentityCharacter character = new IdentityCharacter(name, characterClass, user);
        character.setDescription(description);

        return identityCharacterRepository.save(character);
    }

    /**
     * Findet alle IdentityCharacters eines Users
     */
    @Transactional(readOnly = true)
    public List<IdentityCharacter> findByUser(User user) {
        return identityCharacterRepository.findByUser(user);
    }

    /**
     * Findet alle aktiven IdentityCharacters eines Users
     */
    @Transactional(readOnly = true)
    public List<IdentityCharacter> findActiveCharactersByUser(User user) {
        return identityCharacterRepository.findByUserAndActiveTrue(user);
    }

    /**
     * Findet einen IdentityCharacter anhand der ID
     */
    @Transactional(readOnly = true)
    public Optional<IdentityCharacter> findById(Long id) {
        return identityCharacterRepository.findById(id);
    }

    /**
     * Findet einen IdentityCharacter anhand des Namens
     */
    @Transactional(readOnly = true)
    public Optional<IdentityCharacter> findByName(String name) {
        return identityCharacterRepository.findByName(name);
    }

    /**
     * Aktualisiert die Position eines IdentityCharacters
     */
    public IdentityCharacter updatePosition(Long characterId, String worldId, String planet,
                                        Double x, Double y, Double z) {
        IdentityCharacter character = identityCharacterRepository.findById(characterId)
                .orElseThrow(() -> new IllegalArgumentException("Character not found: " + characterId));

        character.setCurrentWorldId(worldId);
        character.setCurrentPlanet(planet);
        character.setPositionX(x);
        character.setPositionY(y);
        character.setPositionZ(z);

        return identityCharacterRepository.save(character);
    }

    /**
     * Aktualisiert Level und Erfahrungspunkte
     */
    public IdentityCharacter updateLevelAndExperience(Long characterId, Integer level, Long experiencePoints) {
        IdentityCharacter character = identityCharacterRepository.findById(characterId)
                .orElseThrow(() -> new IllegalArgumentException("Character not found: " + characterId));

        character.setLevel(level);
        character.setExperiencePoints(experiencePoints);

        return identityCharacterRepository.save(character);
    }

    /**
     * Aktualisiert Gesundheits- und Manapunkte
     */
    public IdentityCharacter updateHealthAndMana(Long characterId, Integer healthPoints, Integer manaPoints) {
        IdentityCharacter character = identityCharacterRepository.findById(characterId)
                .orElseThrow(() -> new IllegalArgumentException("Character not found: " + characterId));

        character.setHealthPoints(healthPoints);
        character.setManaPoints(manaPoints);

        return identityCharacterRepository.save(character);
    }

    /**
     * Setzt den letzten Login-Zeitstempel
     */
    public IdentityCharacter updateLastLogin(Long characterId) {
        IdentityCharacter character = identityCharacterRepository.findById(characterId)
                .orElseThrow(() -> new IllegalArgumentException("Character not found: " + characterId));

        character.setLastLogin(Instant.now());

        return identityCharacterRepository.save(character);
    }

    /**
     * Deaktiviert einen IdentityCharacter
     */
    public void deactivateIdentityCharacter(Long characterId) {
        IdentityCharacter character = identityCharacterRepository.findById(characterId)
                .orElseThrow(() -> new IllegalArgumentException("Character not found: " + characterId));

        character.setActive(false);
        identityCharacterRepository.save(character);
    }

    /**
     * Reaktiviert einen IdentityCharacter
     */
    public IdentityCharacter reactivateIdentityCharacter(Long characterId) {
        IdentityCharacter character = identityCharacterRepository.findById(characterId)
                .orElseThrow(() -> new IllegalArgumentException("Character not found: " + characterId));

        character.setActive(true);
        return identityCharacterRepository.save(character);
    }

    /**
     * Findet IdentityCharacters auf einem bestimmten Planeten
     */
    @Transactional(readOnly = true)
    public List<IdentityCharacter> findByPlanet(String planet) {
        return identityCharacterRepository.findByCurrentPlanet(planet);
    }

    /**
     * Findet IdentityCharacters in einer bestimmten Welt
     */
    @Transactional(readOnly = true)
    public List<IdentityCharacter> findByWorld(String worldId) {
        return identityCharacterRepository.findByCurrentWorldId(worldId);
    }
}
