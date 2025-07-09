package de.mhus.nimbus.common.client;

import de.mhus.nimbus.shared.avro.PlayerCharacterLookupRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Client for communicating with world-life module via Kafka
 * Verwendet standardisierte Avro-Objekte für Character-Lookup-Operationen
 */
@Component
public class WorldLifeClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(WorldLifeClient.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;

    // Kafka Topics
    private static final String CHARACTER_LOOKUP_TOPIC = "character-lookup";

    @Autowired
    public WorldLifeClient(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * Sucht nach einem Charakter anhand der Charakter-ID
     *
     * @param characterId Die Charakter-ID
     * @return CompletableFuture für asynchrone Verarbeitung
     */
    public CompletableFuture<Void> lookupCharacterById(Long characterId) {
        String requestId = UUID.randomUUID().toString();

        PlayerCharacterLookupRequest message = PlayerCharacterLookupRequest.newBuilder()
                .setRequestId(requestId)
                .setCharacterId(characterId)
                .setActiveOnly(true)
                .setTimestamp(Instant.now())
                .build();

        LOGGER.info("Looking up character by ID {} with requestId {}", characterId, requestId);

        return sendMessage(CHARACTER_LOOKUP_TOPIC, requestId, message);
    }

    /**
     * Sucht nach einem Charakter anhand des Charakternamens
     *
     * @param characterName Der Charaktername
     * @return CompletableFuture für asynchrone Verarbeitung
     */
    public CompletableFuture<Void> lookupCharacterByName(String characterName) {
        String requestId = UUID.randomUUID().toString();

        PlayerCharacterLookupRequest message = PlayerCharacterLookupRequest.newBuilder()
                .setRequestId(requestId)
                .setCharacterName(characterName)
                .setActiveOnly(true)
                .setTimestamp(Instant.now())
                .build();

        LOGGER.info("Looking up character by name '{}' with requestId {}", characterName, requestId);

        return sendMessage(CHARACTER_LOOKUP_TOPIC, requestId, message);
    }

    /**
     * Sucht nach Charakteren eines bestimmten Users
     *
     * @param userId Die User-ID
     * @param activeOnly Nur aktive Charaktere
     * @return CompletableFuture für asynchrone Verarbeitung
     */
    public CompletableFuture<Void> lookupCharactersByUserId(Long userId, boolean activeOnly) {
        String requestId = UUID.randomUUID().toString();

        PlayerCharacterLookupRequest message = PlayerCharacterLookupRequest.newBuilder()
                .setRequestId(requestId)
                .setUserId(userId)
                .setActiveOnly(activeOnly)
                .setTimestamp(Instant.now())
                .build();

        LOGGER.info("Looking up characters for userId {} (activeOnly: {}) with requestId {}",
                   userId, activeOnly, requestId);

        return sendMessage(CHARACTER_LOOKUP_TOPIC, requestId, message);
    }

    /**
     * Sucht nach Charakteren in einer bestimmten Welt
     *
     * @param worldId Die Welt-ID
     * @param activeOnly Nur aktive Charaktere
     * @return CompletableFuture für asynchrone Verarbeitung
     */
    public CompletableFuture<Void> lookupCharactersByWorldId(String worldId, boolean activeOnly) {
        String requestId = UUID.randomUUID().toString();

        PlayerCharacterLookupRequest message = PlayerCharacterLookupRequest.newBuilder()
                .setRequestId(requestId)
                .setCurrentWorldId(worldId)
                .setActiveOnly(activeOnly)
                .setTimestamp(Instant.now())
                .build();

        LOGGER.info("Looking up characters in world {} (activeOnly: {}) with requestId {}",
                   worldId, activeOnly, requestId);

        return sendMessage(CHARACTER_LOOKUP_TOPIC, requestId, message);
    }

    /**
     * Sucht nach Charakteren auf einem bestimmten Planeten
     *
     * @param planetName Der Planetenname
     * @param activeOnly Nur aktive Charaktere
     * @return CompletableFuture für asynchrone Verarbeitung
     */
    public CompletableFuture<Void> lookupCharactersByPlanet(String planetName, boolean activeOnly) {
        String requestId = UUID.randomUUID().toString();

        PlayerCharacterLookupRequest message = PlayerCharacterLookupRequest.newBuilder()
                .setRequestId(requestId)
                .setCurrentPlanet(planetName)
                .setActiveOnly(activeOnly)
                .setTimestamp(Instant.now())
                .build();

        LOGGER.info("Looking up characters on planet {} (activeOnly: {}) with requestId {}",
                   planetName, activeOnly, requestId);

        return sendMessage(CHARACTER_LOOKUP_TOPIC, requestId, message);
    }

    /**
     * Erweiterte Charakter-Suche mit mehreren Kriterien
     *
     * @param characterId Die Charakter-ID (optional)
     * @param characterName Der Charaktername (optional)
     * @param userId Die User-ID (optional)
     * @param currentPlanet Der aktuelle Planet (optional)
     * @param currentWorldId Die aktuelle Welt-ID (optional)
     * @param activeOnly Nur aktive Charaktere
     * @return CompletableFuture für asynchrone Verarbeitung
     */
    public CompletableFuture<Void> lookupCharacters(Long characterId, String characterName, Long userId,
                                                   String currentPlanet, String currentWorldId, boolean activeOnly) {
        String requestId = UUID.randomUUID().toString();

        PlayerCharacterLookupRequest.Builder builder = PlayerCharacterLookupRequest.newBuilder()
                .setRequestId(requestId)
                .setActiveOnly(activeOnly)
                .setTimestamp(Instant.now());

        if (characterId != null) {
            builder.setCharacterId(characterId);
        }
        if (characterName != null) {
            builder.setCharacterName(characterName);
        }
        if (userId != null) {
            builder.setUserId(userId);
        }
        if (currentPlanet != null) {
            builder.setCurrentPlanet(currentPlanet);
        }
        if (currentWorldId != null) {
            builder.setCurrentWorldId(currentWorldId);
        }

        PlayerCharacterLookupRequest message = builder.build();

        LOGGER.info("Extended character lookup with requestId {}", requestId);

        return sendMessage(CHARACTER_LOOKUP_TOPIC, requestId, message);
    }

    /**
     * Sendet eine Nachricht an ein Kafka-Topic
     *
     * @param topic     Das Kafka-Topic
     * @param messageId Die Nachrichten-ID (wird als Key verwendet)
     * @param message   Die zu sendende Nachricht
     * @return CompletableFuture für asynchrone Verarbeitung
     */
    private CompletableFuture<Void> sendMessage(String topic, String messageId, Object message) {
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
