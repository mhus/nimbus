package de.mhus.nimbus.worldlife.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.mhus.nimbus.shared.dto.CharacterOperationMessage;
import de.mhus.nimbus.worldlife.entity.WorldCharacter;
import de.mhus.nimbus.worldlife.service.WorldLifeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Kafka consumer for world character operations
 */
@Component
public class WorldLifeConsumer {

    private static final Logger LOGGER = LoggerFactory.getLogger(WorldLifeConsumer.class);

    private final WorldLifeService worldLifeService;
    private final ObjectMapper objectMapper;

    @Autowired
    public WorldLifeConsumer(WorldLifeService worldLifeService) {
        this.worldLifeService = worldLifeService;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Consumes character operation messages from Kafka
     */
    @KafkaListener(topics = "character-operations", groupId = "world-life-service")
    public void consumeCharacterOperation(CharacterOperationMessage message) {
        try {
            LOGGER.debug("Kafka: Received character operation {} for world {} with messageId {}",
                        message.getOperation(), message.getWorldId(), message.getMessageId());

            switch (message.getOperation()) {
                case CREATE:
                    handleCreateCharacter(message);
                    break;
                case UPDATE_POSITION:
                    handleUpdatePosition(message);
                    break;
                case UPDATE_HEALTH:
                    handleUpdateHealth(message);
                    break;
                case UPDATE_INFO:
                    handleUpdateInfo(message);
                    break;
                case DELETE:
                    handleDeleteCharacter(message);
                    break;
                case BATCH_CREATE:
                    handleBatchCreateCharacters(message);
                    break;
                default:
                    LOGGER.warn("Unknown operation type: {}", message.getOperation());
            }

            LOGGER.debug("Kafka: Successfully processed operation {} for messageId {}",
                        message.getOperation(), message.getMessageId());

        } catch (Exception e) {
            LOGGER.error("Kafka: Failed to process character operation message with ID {}: {}",
                        message.getMessageId(), e.getMessage(), e);
        }
    }

    /**
     * Handles create character operation
     */
    private void handleCreateCharacter(CharacterOperationMessage message) {
        try {
            if (message.getCharacterData() != null) {
                CharacterOperationMessage.CharacterData data = message.getCharacterData();

                WorldCharacter character = worldLifeService.createCharacter(
                    message.getWorldId(),
                    data.getCharacterType(),
                    data.getX(),
                    data.getY(),
                    data.getZ(),
                    data.getName()
                );

                // Set additional properties if provided
                if (data.getHealth() != null) {
                    worldLifeService.updateCharacterHealth(character.getId(), data.getHealth());
                }

                if (data.getDisplayName() != null || data.getDescription() != null) {
                    worldLifeService.updateCharacterInfo(character.getId(), data.getName(), data.getDisplayName(), data.getDescription());
                }

                LOGGER.info("Kafka: Created character with ID {} of type {} at ({}, {}, {}) in world {}",
                           character.getId(), data.getCharacterType(), data.getX(), data.getY(), data.getZ(), message.getWorldId());
            }
        } catch (Exception e) {
            LOGGER.error("Kafka: Failed to create character: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to create character", e);
        }
    }

    /**
     * Handles update position operation
     */
    private void handleUpdatePosition(CharacterOperationMessage message) {
        try {
            if (message.getCharacterData() != null) {
                CharacterOperationMessage.CharacterData data = message.getCharacterData();

                Optional<WorldCharacter> updated = worldLifeService.updateCharacterPosition(
                    data.getCharacterId(),
                    data.getX(),
                    data.getY(),
                    data.getZ()
                );

                if (updated.isPresent()) {
                    LOGGER.info("Kafka: Updated position for character {} to ({}, {}, {})",
                               data.getCharacterId(), data.getX(), data.getY(), data.getZ());
                } else {
                    LOGGER.warn("Kafka: Character with ID {} not found for position update", data.getCharacterId());
                }
            }
        } catch (Exception e) {
            LOGGER.error("Kafka: Failed to update character position: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to update character position", e);
        }
    }

    /**
     * Handles update health operation
     */
    private void handleUpdateHealth(CharacterOperationMessage message) {
        try {
            if (message.getCharacterData() != null) {
                CharacterOperationMessage.CharacterData data = message.getCharacterData();

                Optional<WorldCharacter> updated = worldLifeService.updateCharacterHealth(
                    data.getCharacterId(),
                    data.getHealth()
                );

                if (updated.isPresent()) {
                    LOGGER.info("Kafka: Updated health for character {} to {}",
                               data.getCharacterId(), data.getHealth());
                } else {
                    LOGGER.warn("Kafka: Character with ID {} not found for health update", data.getCharacterId());
                }
            }
        } catch (Exception e) {
            LOGGER.error("Kafka: Failed to update character health: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to update character health", e);
        }
    }

    /**
     * Handles update info operation
     */
    private void handleUpdateInfo(CharacterOperationMessage message) {
        try {
            if (message.getCharacterData() != null) {
                CharacterOperationMessage.CharacterData data = message.getCharacterData();

                Optional<WorldCharacter> updated = worldLifeService.updateCharacterInfo(
                    data.getCharacterId(),
                    data.getName(),
                    data.getDisplayName(),
                    data.getDescription()
                );

                if (updated.isPresent()) {
                    LOGGER.info("Kafka: Updated info for character {}", data.getCharacterId());
                } else {
                    LOGGER.warn("Kafka: Character with ID {} not found for info update", data.getCharacterId());
                }
            }
        } catch (Exception e) {
            LOGGER.error("Kafka: Failed to update character info: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to update character info", e);
        }
    }

    /**
     * Handles delete character operation
     */
    private void handleDeleteCharacter(CharacterOperationMessage message) {
        try {
            if (message.getCharacterData() != null) {
                CharacterOperationMessage.CharacterData data = message.getCharacterData();

                boolean deleted = worldLifeService.deleteCharacter(data.getCharacterId());

                if (deleted) {
                    LOGGER.info("Kafka: Deleted character with ID {}", data.getCharacterId());
                } else {
                    LOGGER.warn("Kafka: Character with ID {} not found for deletion", data.getCharacterId());
                }
            }
        } catch (Exception e) {
            LOGGER.error("Kafka: Failed to delete character: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to delete character", e);
        }
    }

    /**
     * Handles batch create characters operation
     */
    private void handleBatchCreateCharacters(CharacterOperationMessage message) {
        try {
            if (message.getBatchData() != null) {
                WorldCharacter[] characterArray = objectMapper.readValue(
                    message.getBatchData().getCharactersJson(),
                    WorldCharacter[].class
                );
                List<WorldCharacter> characters = Arrays.asList(characterArray);

                List<WorldCharacter> saved = worldLifeService.saveCharacters(characters);

                LOGGER.info("Kafka: Batch created {} characters in world {}",
                           saved.size(), message.getWorldId());
            }
        } catch (Exception e) {
            LOGGER.error("Kafka: Failed to batch create characters: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to batch create characters", e);
        }
    }
}
