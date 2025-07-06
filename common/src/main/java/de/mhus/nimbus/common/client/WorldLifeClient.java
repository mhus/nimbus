package de.mhus.nimbus.common.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.mhus.nimbus.shared.character.CharacterType;
import de.mhus.nimbus.shared.dto.CharacterOperationMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Client for communicating with world-life module via Kafka
 */
@Component
public class WorldLifeClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(WorldLifeClient.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    // Kafka Topics
    private static final String CHARACTER_OPERATIONS_TOPIC = "character-operations";

    @Autowired
    public WorldLifeClient(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Erstellt einen neuen Charakter
     *
     * @param worldId       Die Welt-ID
     * @param characterType Der Charakter-Typ
     * @param x             X-Koordinate
     * @param y             Y-Koordinate
     * @param z             Z-Koordinate
     * @param name          Name des Charakters
     * @return CompletableFuture für asynchrone Verarbeitung
     */
    public CompletableFuture<Void> createCharacter(String worldId, CharacterType characterType,
                                                   double x, double y, double z, String name) {
        String messageId = UUID.randomUUID().toString();

        CharacterOperationMessage message = new CharacterOperationMessage();
        message.setMessageId(messageId);
        message.setOperation(CharacterOperationMessage.OperationType.CREATE);
        message.setWorldId(worldId);
        message.setCharacterData(new CharacterOperationMessage.CharacterData());

        CharacterOperationMessage.CharacterData data = message.getCharacterData();
        data.setCharacterType(characterType);
        data.setX(x);
        data.setY(y);
        data.setZ(z);
        data.setName(name);

        return sendMessage(CHARACTER_OPERATIONS_TOPIC, messageId, message);
    }

    /**
     * Erstellt einen neuen Charakter mit erweiterten Informationen
     *
     * @param worldId       Die Welt-ID
     * @param characterType Der Charakter-Typ
     * @param x             X-Koordinate
     * @param y             Y-Koordinate
     * @param z             Z-Koordinate
     * @param name          Name des Charakters
     * @param displayName   Anzeigename
     * @param description   Beschreibung
     * @param health        Gesundheit
     * @param maxHealth     Maximale Gesundheit
     * @return CompletableFuture für asynchrone Verarbeitung
     */
    public CompletableFuture<Void> createCharacterWithDetails(String worldId, CharacterType characterType,
                                                             double x, double y, double z, String name,
                                                             String displayName, String description,
                                                             Integer health, Integer maxHealth) {
        String messageId = UUID.randomUUID().toString();

        CharacterOperationMessage message = new CharacterOperationMessage();
        message.setMessageId(messageId);
        message.setOperation(CharacterOperationMessage.OperationType.CREATE);
        message.setWorldId(worldId);

        CharacterOperationMessage.CharacterData data = new CharacterOperationMessage.CharacterData();
        data.setCharacterType(characterType);
        data.setX(x);
        data.setY(y);
        data.setZ(z);
        data.setName(name);
        data.setDisplayName(displayName);
        data.setDescription(description);
        data.setHealth(health);
        data.setMaxHealth(maxHealth);
        data.setActive(true);

        message.setCharacterData(data);

        return sendMessage(CHARACTER_OPERATIONS_TOPIC, messageId, message);
    }

    /**
     * Aktualisiert die Position eines Charakters
     *
     * @param worldId     Die Welt-ID
     * @param characterId Die Charakter-ID
     * @param x           Neue X-Koordinate
     * @param y           Neue Y-Koordinate
     * @param z           Neue Z-Koordinate
     * @return CompletableFuture für asynchrone Verarbeitung
     */
    public CompletableFuture<Void> updateCharacterPosition(String worldId, Long characterId, double x, double y, double z) {
        String messageId = UUID.randomUUID().toString();

        CharacterOperationMessage message = new CharacterOperationMessage();
        message.setMessageId(messageId);
        message.setOperation(CharacterOperationMessage.OperationType.UPDATE_POSITION);
        message.setWorldId(worldId);

        CharacterOperationMessage.CharacterData data = new CharacterOperationMessage.CharacterData();
        data.setCharacterId(characterId);
        data.setX(x);
        data.setY(y);
        data.setZ(z);

        message.setCharacterData(data);

        return sendMessage(CHARACTER_OPERATIONS_TOPIC, messageId, message);
    }

    /**
     * Aktualisiert die Gesundheit eines Charakters
     *
     * @param worldId     Die Welt-ID
     * @param characterId Die Charakter-ID
     * @param health      Neue Gesundheit
     * @return CompletableFuture für asynchrone Verarbeitung
     */
    public CompletableFuture<Void> updateCharacterHealth(String worldId, Long characterId, Integer health) {
        String messageId = UUID.randomUUID().toString();

        CharacterOperationMessage message = new CharacterOperationMessage();
        message.setMessageId(messageId);
        message.setOperation(CharacterOperationMessage.OperationType.UPDATE_HEALTH);
        message.setWorldId(worldId);

        CharacterOperationMessage.CharacterData data = new CharacterOperationMessage.CharacterData();
        data.setCharacterId(characterId);
        data.setHealth(health);

        message.setCharacterData(data);

        return sendMessage(CHARACTER_OPERATIONS_TOPIC, messageId, message);
    }

    /**
     * Aktualisiert die Informationen eines Charakters
     *
     * @param worldId     Die Welt-ID
     * @param characterId Die Charakter-ID
     * @param name        Neuer Name
     * @param displayName Neuer Anzeigename
     * @param description Neue Beschreibung
     * @return CompletableFuture für asynchrone Verarbeitung
     */
    public CompletableFuture<Void> updateCharacterInfo(String worldId, Long characterId, String name,
                                                       String displayName, String description) {
        String messageId = UUID.randomUUID().toString();

        CharacterOperationMessage message = new CharacterOperationMessage();
        message.setMessageId(messageId);
        message.setOperation(CharacterOperationMessage.OperationType.UPDATE_INFO);
        message.setWorldId(worldId);

        CharacterOperationMessage.CharacterData data = new CharacterOperationMessage.CharacterData();
        data.setCharacterId(characterId);
        data.setName(name);
        data.setDisplayName(displayName);
        data.setDescription(description);

        message.setCharacterData(data);

        return sendMessage(CHARACTER_OPERATIONS_TOPIC, messageId, message);
    }

    /**
     * Löscht einen Charakter
     *
     * @param worldId     Die Welt-ID
     * @param characterId Die Charakter-ID
     * @return CompletableFuture für asynchrone Verarbeitung
     */
    public CompletableFuture<Void> deleteCharacter(String worldId, Long characterId) {
        String messageId = UUID.randomUUID().toString();

        CharacterOperationMessage message = new CharacterOperationMessage();
        message.setMessageId(messageId);
        message.setOperation(CharacterOperationMessage.OperationType.DELETE);
        message.setWorldId(worldId);

        CharacterOperationMessage.CharacterData data = new CharacterOperationMessage.CharacterData();
        data.setCharacterId(characterId);

        message.setCharacterData(data);

        return sendMessage(CHARACTER_OPERATIONS_TOPIC, messageId, message);
    }

    /**
     * Erstellt mehrere Charaktere in einem Batch
     *
     * @param worldId    Die Welt-ID
     * @param characters Liste der zu erstellenden Charaktere
     * @return CompletableFuture für asynchrone Verarbeitung
     */
    public CompletableFuture<Void> batchCreateCharacters(String worldId, List<CharacterOperationMessage.CharacterData> characters) {
        try {
            String messageId = UUID.randomUUID().toString();
            String charactersJson = objectMapper.writeValueAsString(characters);

            CharacterOperationMessage message = new CharacterOperationMessage();
            message.setMessageId(messageId);
            message.setOperation(CharacterOperationMessage.OperationType.BATCH_CREATE);
            message.setWorldId(worldId);
            message.setBatchData(new CharacterOperationMessage.BatchData(charactersJson));

            return sendMessage(CHARACTER_OPERATIONS_TOPIC, messageId, message);

        } catch (Exception e) {
            LOGGER.error("Failed to serialize characters for batch create operation", e);
            CompletableFuture<Void> future = new CompletableFuture<>();
            future.completeExceptionally(e);
            return future;
        }
    }

    /**
     * Aktiviert oder deaktiviert einen Charakter
     *
     * @param worldId     Die Welt-ID
     * @param characterId Die Charakter-ID
     * @param active      Aktivierungsstatus
     * @return CompletableFuture für asynchrone Verarbeitung
     */
    public CompletableFuture<Void> setCharacterActive(String worldId, Long characterId, boolean active) {
        String messageId = UUID.randomUUID().toString();

        CharacterOperationMessage message = new CharacterOperationMessage();
        message.setMessageId(messageId);
        message.setOperation(CharacterOperationMessage.OperationType.UPDATE_INFO);
        message.setWorldId(worldId);

        CharacterOperationMessage.CharacterData data = new CharacterOperationMessage.CharacterData();
        data.setCharacterId(characterId);
        data.setActive(active);

        message.setCharacterData(data);

        return sendMessage(CHARACTER_OPERATIONS_TOPIC, messageId, message);
    }

    /**
     * Sendet eine Nachricht an ein Kafka-Topic
     *
     * @param topic     Das Kafka-Topic
     * @param messageId Die Nachrichten-ID (wird als Key verwendet)
     * @param message   Die zu sendende Nachricht
     * @return CompletableFuture für asynchrone Verarbeitung
     */
    private CompletableFuture<Void> sendMessage(String topic, String messageId, CharacterOperationMessage message) {
        try {
            return kafkaTemplate.send(topic, messageId, message)
                .thenRun(() -> LOGGER.debug("Successfully sent message with ID {} to topic {}", messageId, topic))
                .handle((result, throwable) -> {
                    if (throwable != null) {
                        LOGGER.error("Failed to send message with ID {} to topic {}: {}",
                                   messageId, topic, throwable.getMessage(), throwable);
                        throw new RuntimeException(throwable);
                    }
                    return null;
                });

        } catch (Exception e) {
            LOGGER.error("Failed to send message with ID {} to topic {}: {}",
                       messageId, topic, e.getMessage(), e);
            CompletableFuture<Void> future = new CompletableFuture<>();
            future.completeExceptionally(e);
            return future;
        }
    }
}
