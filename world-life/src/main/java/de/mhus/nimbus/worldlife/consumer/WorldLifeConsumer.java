package de.mhus.nimbus.worldlife.consumer;

import de.mhus.nimbus.shared.avro.CharacterOperationMessage;
import de.mhus.nimbus.shared.avro.CharacterData;
import de.mhus.nimbus.shared.avro.CharacterBatchData;
import de.mhus.nimbus.worldlife.entity.WorldCharacter;
import de.mhus.nimbus.worldlife.service.WorldLifeService;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Kafka consumer for world character operations
 */
@Component
@Slf4j
public class WorldLifeConsumer {

    private final WorldLifeService worldLifeService;

    @Autowired
    public WorldLifeConsumer(WorldLifeService worldLifeService) {
        this.worldLifeService = worldLifeService;
    }

    /**
     * Consumes character operation messages from Kafka
     */
    @KafkaListener(topics = "character-operations", groupId = "world-life-service")
    public void consumeCharacterOperation(CharacterOperationMessage message) {
        try {
            log.debug("Kafka: Received character operation {} for world {} with messageId {}",
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
                    log.warn("Unknown operation type: {}", message.getOperation());
            }

            log.debug("Kafka: Successfully processed operation {} for messageId {}",
                        message.getOperation(), message.getMessageId());

        } catch (Exception e) {
            log.error("Kafka: Failed to process character operation message with ID {}: {}",
                        message.getMessageId(), e.getMessage(), e);
        }
    }

    /**
     * Handles create character operation
     */
    private void handleCreateCharacter(CharacterOperationMessage message) {
        try {
            if (message.getCharacterData() != null) {
                CharacterData data = message.getCharacterData();

                // Convert string to CharacterType enum
                de.mhus.nimbus.shared.character.CharacterType characterType;
                try {
                    characterType = de.mhus.nimbus.shared.character.CharacterType.valueOf(data.getCharacterType().toUpperCase());
                } catch (IllegalArgumentException e) {
                    log.error("Invalid character type: {}", data.getCharacterType());
                    return;
                }

                WorldCharacter character = worldLifeService.createCharacter(
                    message.getWorldId(),
                    characterType,
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

                log.info("Kafka: Created character with ID {} of type {} at ({}, {}, {}) in world {}",
                           character.getId(), data.getCharacterType(), data.getX(), data.getY(), data.getZ(), message.getWorldId());
            }
        } catch (Exception e) {
            log.error("Kafka: Failed to create character: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to create character", e);
        }
    }

    /**
     * Handles update position operation
     */
    private void handleUpdatePosition(CharacterOperationMessage message) {
        try {
            if (message.getCharacterData() != null) {
                CharacterData data = message.getCharacterData();

                Optional<WorldCharacter> updated = worldLifeService.updateCharacterPosition(
                    data.getCharacterId(),
                    data.getX(),
                    data.getY(),
                    data.getZ()
                );

                if (updated.isPresent()) {
                    log.info("Kafka: Updated position for character {} to ({}, {}, {})",
                               data.getCharacterId(), data.getX(), data.getY(), data.getZ());
                } else {
                    log.warn("Kafka: Character with ID {} not found for position update", data.getCharacterId());
                }
            }
        } catch (Exception e) {
            log.error("Kafka: Failed to update character position: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to update character position", e);
        }
    }

    /**
     * Handles update health operation
     */
    private void handleUpdateHealth(CharacterOperationMessage message) {
        try {
            if (message.getCharacterData() != null) {
                CharacterData data = message.getCharacterData();

                Optional<WorldCharacter> updated = worldLifeService.updateCharacterHealth(
                    data.getCharacterId(),
                    data.getHealth()
                );

                if (updated.isPresent()) {
                    log.info("Kafka: Updated health for character {} to {}",
                               data.getCharacterId(), data.getHealth());
                } else {
                    log.warn("Kafka: Character with ID {} not found for health update", data.getCharacterId());
                }
            }
        } catch (Exception e) {
            log.error("Kafka: Failed to update character health: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to update character health", e);
        }
    }

    /**
     * Handles update info operation
     */
    private void handleUpdateInfo(CharacterOperationMessage message) {
        try {
            if (message.getCharacterData() != null) {
                CharacterData data = message.getCharacterData();

                Optional<WorldCharacter> updated = worldLifeService.updateCharacterInfo(
                    data.getCharacterId(),
                    data.getName(),
                    data.getDisplayName(),
                    data.getDescription()
                );

                if (updated.isPresent()) {
                    log.info("Kafka: Updated info for character {}", data.getCharacterId());
                } else {
                    log.warn("Kafka: Character with ID {} not found for info update", data.getCharacterId());
                }
            }
        } catch (Exception e) {
            log.error("Kafka: Failed to update character info: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to update character info", e);
        }
    }

    /**
     * Handles delete character operation
     */
    private void handleDeleteCharacter(CharacterOperationMessage message) {
        try {
            if (message.getCharacterData() != null) {
                CharacterData data = message.getCharacterData();

                boolean deleted = worldLifeService.deleteCharacter(data.getCharacterId());

                if (deleted) {
                    log.info("Kafka: Deleted character with ID {}", data.getCharacterId());
                } else {
                    log.warn("Kafka: Character with ID {} not found for deletion", data.getCharacterId());
                }
            }
        } catch (Exception e) {
            log.error("Kafka: Failed to delete character: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to delete character", e);
        }
    }

    /**
     * Handles batch create characters operation
     */
    private void handleBatchCreateCharacters(CharacterOperationMessage message) {
        try {
            if (message.getBatchData() != null) {
                CharacterBatchData batchData = message.getBatchData();

                // TODO: Parse charactersJson and convert to List<WorldCharacter>
                // For now, log that we received the batch data
                log.info("Kafka: Received batch create request with JSON data: {}",
                           batchData.getCharactersJson());

                // This needs to be implemented when the JSON parsing logic is defined
                // List<WorldCharacter> characters = parseCharactersFromJson(batchData.getCharactersJson());
                // List<WorldCharacter> savedCharacters = worldLifeService.saveCharacters(characters);

                log.warn("Kafka: Batch create characters operation not yet implemented - JSON parsing needed");
            }
        } catch (Exception e) {
            log.error("Kafka: Failed to batch create characters: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to batch create characters", e);
        }
    }
}
