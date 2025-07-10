package de.mhus.nimbus.worldlife.service;

import de.mhus.nimbus.shared.character.CharacterType;
import de.mhus.nimbus.shared.avro.CharacterOperationMessage;
import de.mhus.nimbus.worldlife.entity.WorldCharacter;
import de.mhus.nimbus.worldlife.repository.WorldCharacterRepository;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Service for managing world life operations
 */
@Service
@Transactional
@Slf4j
public class WorldLifeService {

    private final WorldCharacterRepository worldCharacterRepository;

    @Autowired
    public WorldLifeService(WorldCharacterRepository worldCharacterRepository) {
        this.worldCharacterRepository = worldCharacterRepository;
    }

    /**
     * Creates a new world character
     *
     * @param worldId       The world identifier
     * @param characterType The type of character
     * @param x             X coordinate
     * @param y             Y coordinate
     * @param z             Z coordinate
     * @param name          Optional character name
     * @return The created WorldCharacter
     */
    public WorldCharacter createCharacter(String worldId, CharacterType characterType,
                                        double x, double y, double z, String name) {
        log.debug("Creating character of type {} at position ({}, {}, {}) in world {}",
                    characterType, x, y, z, worldId);

        WorldCharacter character = new WorldCharacter(worldId, characterType, x, y, z, name);
        WorldCharacter saved = worldCharacterRepository.save(character);

        log.info("Created character with ID {} of type {} in world {}",
                   saved.getId(), characterType, worldId);

        return saved;
    }

    /**
     * Gets a character by ID
     *
     * @param characterId The character ID
     * @return The character if found
     */
    @Transactional(readOnly = true)
    public Optional<WorldCharacter> getCharacter(Long characterId) {
        log.debug("Getting character with ID {}", characterId);
        return worldCharacterRepository.findById(characterId);
    }

    /**
     * Gets all characters in a world
     *
     * @param worldId The world identifier
     * @return List of characters in the world
     */
    @Transactional(readOnly = true)
    public List<WorldCharacter> getCharactersInWorld(String worldId) {
        log.debug("Getting all characters in world {}", worldId);
        return worldCharacterRepository.findByWorldId(worldId);
    }

    /**
     * Gets characters by type in a world
     *
     * @param worldId       The world identifier
     * @param characterType The character type
     * @return List of characters of the specified type
     */
    @Transactional(readOnly = true)
    public List<WorldCharacter> getCharactersByType(String worldId, CharacterType characterType) {
        log.debug("Getting characters of type {} in world {}", characterType, worldId);
        return worldCharacterRepository.findByWorldIdAndCharacterType(worldId, characterType);
    }

    /**
     * Gets active characters in a world
     *
     * @param worldId The world identifier
     * @return List of active characters
     */
    @Transactional(readOnly = true)
    public List<WorldCharacter> getActiveCharacters(String worldId) {
        log.debug("Getting active characters in world {}", worldId);
        return worldCharacterRepository.findByWorldIdAndActiveTrue(worldId);
    }

    /**
     * Updates a character's position
     *
     * @param characterId The character ID
     * @param x           New X coordinate
     * @param y           New Y coordinate
     * @param z           New Z coordinate
     * @return Updated character or empty if not found
     */
    public Optional<WorldCharacter> updateCharacterPosition(Long characterId, double x, double y, double z) {
        log.debug("Updating position for character {} to ({}, {}, {})", characterId, x, y, z);

        Optional<WorldCharacter> characterOpt = worldCharacterRepository.findById(characterId);
        if (characterOpt.isPresent()) {
            WorldCharacter character = characterOpt.get();
            character.updatePosition(x, y, z);
            WorldCharacter updated = worldCharacterRepository.save(character);

            log.info("Updated position for character {} in world {} to ({}, {}, {})",
                       characterId, character.getWorldId(), x, y, z);

            return Optional.of(updated);
        }

        log.warn("Character with ID {} not found for position update", characterId);
        return Optional.empty();
    }

    /**
     * Updates a character's health
     *
     * @param characterId The character ID
     * @param health      New health value
     * @return Updated character or empty if not found
     */
    public Optional<WorldCharacter> updateCharacterHealth(Long characterId, int health) {
        log.debug("Updating health for character {} to {}", characterId, health);

        Optional<WorldCharacter> characterOpt = worldCharacterRepository.findById(characterId);
        if (characterOpt.isPresent()) {
            WorldCharacter character = characterOpt.get();
            character.updateHealth(health);
            WorldCharacter updated = worldCharacterRepository.save(character);

            log.info("Updated health for character {} to {}", characterId, health);
            return Optional.of(updated);
        }

        log.warn("Character with ID {} not found for health update", characterId);
        return Optional.empty();
    }

    /**
     * Updates a character's name and description
     *
     * @param characterId The character ID
     * @param name        New name
     * @param description New description
     * @return Updated character or empty if not found
     */
    public Optional<WorldCharacter> updateCharacterInfo(Long characterId, String name, String description) {
        return updateCharacterInfo(characterId, name, null, description);
    }

    /**
     * Updates a character's name, display name and description
     *
     * @param characterId The character ID
     * @param name        New name
     * @param displayName New display name
     * @param description New description
     * @return Updated character or empty if not found
     */
    public Optional<WorldCharacter> updateCharacterInfo(Long characterId, String name, String displayName, String description) {
        log.debug("Updating info for character {} - name: {}, displayName: {}, description: {}",
                    characterId, name, displayName, description);

        Optional<WorldCharacter> characterOpt = worldCharacterRepository.findById(characterId);
        if (characterOpt.isPresent()) {
            WorldCharacter character = characterOpt.get();
            character.setName(name);
            character.setDisplayName(displayName);
            character.setDescription(description);
            character.setLastModified(LocalDateTime.now());
            WorldCharacter updated = worldCharacterRepository.save(character);

            log.info("Updated info for character {}", characterId);
            return Optional.of(updated);
        }

        log.warn("Character with ID {} not found for info update", characterId);
        return Optional.empty();
    }

    /**
     * Deletes a character by ID
     *
     * @param characterId The character ID to delete
     * @return true if character was deleted, false if not found
     */
    public boolean deleteCharacter(Long characterId) {
        log.debug("Deleting character with ID {}", characterId);

        if (worldCharacterRepository.existsById(characterId)) {
            worldCharacterRepository.deleteById(characterId);
            log.info("Deleted character with ID {}", characterId);
            return true;
        }

        log.warn("Character with ID {} not found for deletion", characterId);
        return false;
    }

    /**
     * Saves multiple characters
     *
     * @param characters List of characters to save
     * @return List of saved characters
     */
    public List<WorldCharacter> saveCharacters(List<WorldCharacter> characters) {
        log.debug("Saving {} characters", characters.size());

        List<WorldCharacter> saved = worldCharacterRepository.saveAll(characters);

        log.info("Successfully saved {} characters", saved.size());
        return saved;
    }

    /**
     * Gets characters within a range/bounding box
     *
     * @param worldId The world identifier
     * @param minX Minimum X coordinate
     * @param maxX Maximum X coordinate
     * @param minY Minimum Y coordinate
     * @param maxY Maximum Y coordinate
     * @param minZ Minimum Z coordinate
     * @param maxZ Maximum Z coordinate
     * @return List of characters within the range
     */
    @Transactional(readOnly = true)
    public List<WorldCharacter> getCharactersInRange(String worldId, double minX, double maxX,
                                                   double minY, double maxY, double minZ, double maxZ) {
        log.debug("Getting characters in range for world {} - X({}, {}), Y({}, {}), Z({}, {})",
                    worldId, minX, maxX, minY, maxY, minZ, maxZ);

        return worldCharacterRepository.findByWorldIdAndXBetweenAndYBetweenAndZBetween(
            worldId, minX, maxX, minY, maxY, minZ, maxZ);
    }

    /**
     * Gets characters within a radius from a center point
     *
     * @param worldId The world identifier
     * @param centerX Center X coordinate
     * @param centerY Center Y coordinate
     * @param centerZ Center Z coordinate
     * @param radius Radius from center point
     * @return List of characters within the radius
     */
    @Transactional(readOnly = true)
    public List<WorldCharacter> getCharactersInRadius(String worldId, double centerX, double centerY,
                                                    double centerZ, double radius) {
        log.debug("Getting characters in radius {} from point ({}, {}, {}) in world {}",
                    radius, centerX, centerY, centerZ, worldId);

        List<WorldCharacter> allCharacters = worldCharacterRepository.findByWorldId(worldId);
        return allCharacters.stream()
            .filter(character -> {
                double dx = character.getX() - centerX;
                double dy = character.getY() - centerY;
                double dz = character.getZ() - centerZ;
                double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
                return distance <= radius;
            })
            .toList();
    }

    /**
     * Gets the count of active characters in a world
     *
     * @param worldId The world identifier
     * @return Count of active characters
     */
    @Transactional(readOnly = true)
    public long getActiveCharacterCount(String worldId) {
        log.debug("Getting active character count for world {}", worldId);
        return worldCharacterRepository.countByWorldIdAndActiveTrue(worldId);
    }

    /**
     * Gets the count of characters by type in a world
     *
     * @param worldId The world identifier
     * @param characterType The character type
     * @return Count of characters of the specified type
     */
    @Transactional(readOnly = true)
    public long getCharacterCountByType(String worldId, CharacterType characterType) {
        log.debug("Getting character count for type {} in world {}", characterType, worldId);
        return worldCharacterRepository.countByWorldIdAndCharacterType(worldId, characterType);
    }

    /**
     * Clears all characters from a world
     *
     * @param worldId The world identifier
     * @return Number of characters deleted
     */
    public long clearWorldCharacters(String worldId) {
        log.debug("Clearing all characters from world {}", worldId);

        long count = worldCharacterRepository.countByWorldId(worldId);
        worldCharacterRepository.deleteByWorldId(worldId);

        log.info("Cleared {} characters from world {}", count, worldId);
        return count;
    }

    /**
     * Updates character data (AbstractCharacter implementation)
     *
     * @param characterId The character ID
     * @param characterData The AbstractCharacter implementation to store
     * @return Updated character or empty if not found
     */
    public Optional<WorldCharacter> updateCharacterData(Long characterId, de.mhus.nimbus.shared.character.AbstractCharacter characterData) {
        log.debug("Updating character data for character {}", characterId);

        Optional<WorldCharacter> characterOpt = worldCharacterRepository.findById(characterId);
        if (characterOpt.isPresent()) {
            WorldCharacter character = characterOpt.get();
            character.setCharacter(characterData);
            WorldCharacter updated = worldCharacterRepository.save(character);

            log.info("Updated character data for character {}", characterId);
            return Optional.of(updated);
        }

        log.warn("Character with ID {} not found for character data update", characterId);
        return Optional.empty();
    }

    /**
     * Gets character data as AbstractCharacter
     *
     * @param characterId The character ID
     * @return The AbstractCharacter implementation or empty if not found
     */
    @Transactional(readOnly = true)
    public Optional<de.mhus.nimbus.shared.character.AbstractCharacter> getCharacterData(Long characterId) {
        log.debug("Getting character data for character {}", characterId);

        Optional<WorldCharacter> characterOpt = worldCharacterRepository.findById(characterId);
        if (characterOpt.isPresent()) {
            WorldCharacter character = characterOpt.get();
            if (character.hasCharacterData()) {
                try {
                    return Optional.ofNullable(character.getCharacter());
                } catch (Exception e) {
                    log.error("Failed to deserialize character data for character {}: {}", characterId, e.getMessage());
                    return Optional.empty();
                }
            }
        }

        return Optional.empty();
    }

    /**
     * Gets character data as specific type
     *
     * @param characterId The character ID
     * @param characterClass The class of the AbstractCharacter implementation
     * @return The typed character implementation or empty if not found
     */
    @Transactional(readOnly = true)
    public <T extends de.mhus.nimbus.shared.character.AbstractCharacter> Optional<T> getCharacterData(Long characterId, Class<T> characterClass) {
        log.debug("Getting character data for character {} as type {}", characterId, characterClass.getSimpleName());

        Optional<WorldCharacter> characterOpt = worldCharacterRepository.findById(characterId);
        if (characterOpt.isPresent()) {
            WorldCharacter character = characterOpt.get();
            if (character.hasCharacterData()) {
                try {
                    return Optional.ofNullable(character.getCharacter(characterClass));
                } catch (Exception e) {
                    log.error("Failed to deserialize character data for character {} as type {}: {}",
                               characterId, characterClass.getSimpleName(), e.getMessage());
                    return Optional.empty();
                }
            }
        }

        return Optional.empty();
    }

    /**
     * Creates a character with AbstractCharacter data
     *
     * @param worldId The world identifier
     * @param characterType The type of character
     * @param x X coordinate
     * @param y Y coordinate
     * @param z Z coordinate
     * @param name Character name
     * @param characterData The AbstractCharacter implementation
     * @return The created WorldCharacter
     */
    public WorldCharacter createCharacterWithData(String worldId, de.mhus.nimbus.shared.character.CharacterType characterType,
                                                 double x, double y, double z, String name,
                                                 de.mhus.nimbus.shared.character.AbstractCharacter characterData) {
        log.debug("Creating character with data of type {} at position ({}, {}, {}) in world {}",
                    characterType, x, y, z, worldId);

        // Convert shared CharacterType to local CharacterType
        CharacterType localCharacterType = CharacterType.valueOf(characterType.name());

        WorldCharacter character = new WorldCharacter(worldId, localCharacterType, x, y, z, name);

        if (characterData != null) {
            character.setCharacter(characterData);
        }

        WorldCharacter saved = worldCharacterRepository.save(character);

        log.info("Created character with ID {} and data of type {} in world {}",
                   saved.getId(), characterType, worldId);

        return saved;
    }

    /**
     * Example method for world life service
     */
    public String getServiceInfo() {
        log.debug("Getting world life service info");
        return "World Life Service is running";
    }

    /**
     * Health check method
     */
    public boolean isHealthy() {
        return true;
    }
}
