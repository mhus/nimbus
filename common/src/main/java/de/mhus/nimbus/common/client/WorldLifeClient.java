package de.mhus.nimbus.common.client;

import de.mhus.nimbus.shared.avro.PlayerCharacterLookupRequest;
import de.mhus.nimbus.shared.avro.PlayerCharacterLookupResponse;
import de.mhus.nimbus.shared.avro.CharacterOperationMessage;
import de.mhus.nimbus.shared.avro.CharacterData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Client for communicating with world-life module via Kafka
 * Verwendet standardisierte Avro-Objekte für Character-Operationen
 */
@Component
public class WorldLifeClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(WorldLifeClient.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;

    // Maps für pending Requests
    private final ConcurrentHashMap<String, CompletableFuture<PlayerCharacterLookupResponse>> pendingCharacterLookups = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, CompletableFuture<Void>> pendingCharacterOperations = new ConcurrentHashMap<>();

    // Kafka Topics
    private static final String CHARACTER_LOOKUP_TOPIC = "character-lookup";
    private static final String CHARACTER_OPERATIONS_TOPIC = "character-operations";

    // Default Timeout für Responses in Sekunden
    private static final int DEFAULT_TIMEOUT_SECONDS = 30;

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
     * Sucht nach einem Charakter anhand der Charakter-ID mit Response-Handling
     *
     * @param characterId Die Charakter-ID
     * @return CompletableFuture mit PlayerCharacterLookupResponse
     */
    public CompletableFuture<PlayerCharacterLookupResponse> lookupCharacterByIdWithResponse(Long characterId) {
        String requestId = UUID.randomUUID().toString();

        PlayerCharacterLookupRequest message = PlayerCharacterLookupRequest.newBuilder()
                .setRequestId(requestId)
                .setCharacterId(characterId)
                .setActiveOnly(true)
                .setTimestamp(Instant.now())
                .setRequestedBy("WorldLifeClient")
                .build();

        LOGGER.info("Looking up character by ID {} with requestId {} (with response)", characterId, requestId);

        CompletableFuture<PlayerCharacterLookupResponse> future = new CompletableFuture<>();
        pendingCharacterLookups.put(requestId, future);

        sendMessage(CHARACTER_LOOKUP_TOPIC, requestId, message)
            .orTimeout(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .whenComplete((result, throwable) -> {
                if (throwable != null) {
                    future.completeExceptionally(throwable);
                }
            });

        return future;
    }

    /**
     * Sucht nach einem Charakter anhand des Charakternamens mit Response-Handling
     *
     * @param characterName Der Charaktername
     * @return CompletableFuture mit PlayerCharacterLookupResponse
     */
    public CompletableFuture<PlayerCharacterLookupResponse> lookupCharacterByNameWithResponse(String characterName) {
        String requestId = UUID.randomUUID().toString();

        PlayerCharacterLookupRequest message = PlayerCharacterLookupRequest.newBuilder()
                .setRequestId(requestId)
                .setCharacterName(characterName)
                .setActiveOnly(true)
                .setTimestamp(Instant.now())
                .setRequestedBy("WorldLifeClient")
                .build();

        LOGGER.info("Looking up character by name '{}' with requestId {} (with response)", characterName, requestId);

        CompletableFuture<PlayerCharacterLookupResponse> future = new CompletableFuture<>();
        pendingCharacterLookups.put(requestId, future);

        sendMessage(CHARACTER_LOOKUP_TOPIC, requestId, message)
            .orTimeout(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .whenComplete((result, throwable) -> {
                if (throwable != null) {
                    future.completeExceptionally(throwable);
                }
            });

        return future;
    }

    /**
     * Sucht nach Charakteren eines bestimmten Users mit Response-Handling
     *
     * @param userId Die User-ID
     * @param activeOnly Nur aktive Charaktere
     * @return CompletableFuture mit PlayerCharacterLookupResponse
     */
    public CompletableFuture<PlayerCharacterLookupResponse> lookupCharactersByUserIdWithResponse(Long userId, boolean activeOnly) {
        String requestId = UUID.randomUUID().toString();

        PlayerCharacterLookupRequest message = PlayerCharacterLookupRequest.newBuilder()
                .setRequestId(requestId)
                .setUserId(userId)
                .setActiveOnly(activeOnly)
                .setTimestamp(Instant.now())
                .setRequestedBy("WorldLifeClient")
                .build();

        LOGGER.info("Looking up characters for userId {} (activeOnly: {}) with requestId {} (with response)",
                   userId, activeOnly, requestId);

        CompletableFuture<PlayerCharacterLookupResponse> future = new CompletableFuture<>();
        pendingCharacterLookups.put(requestId, future);

        sendMessage(CHARACTER_LOOKUP_TOPIC, requestId, message)
            .orTimeout(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .whenComplete((result, throwable) -> {
                if (throwable != null) {
                    future.completeExceptionally(throwable);
                }
            });

        return future;
    }

    /**
     * Erstellt einen neuen Charakter
     *
     * @param worldId Die Welt-ID
     * @param characterType Der Charaktertyp
     * @param x X-Koordinate
     * @param y Y-Koordinate
     * @param z Z-Koordinate
     * @param name Charaktername
     * @return CompletableFuture für asynchrone Verarbeitung
     */
    public CompletableFuture<Void> createCharacter(String worldId, String characterType,
                                                  Double x, Double y, Double z, String name) {
        String messageId = UUID.randomUUID().toString();

        CharacterData characterData = CharacterData.newBuilder()
                .setCharacterType(characterType)
                .setX(x)
                .setY(y)
                .setZ(z)
                .setName(name)
                .build();

        CharacterOperationMessage message = CharacterOperationMessage.newBuilder()
                .setMessageId(messageId)
                .setWorldId(worldId)
                .setOperation(de.mhus.nimbus.shared.avro.CharacterOperationType.CREATE)
                .setCharacterData(characterData)
                .setTimestamp(Instant.now())
                .build();

        LOGGER.info("Creating character '{}' of type {} in world {} with messageId {}",
                   name, characterType, worldId, messageId);

        CompletableFuture<Void> future = new CompletableFuture<>();
        pendingCharacterOperations.put(messageId, future);

        sendMessage(CHARACTER_OPERATIONS_TOPIC, messageId, message)
            .orTimeout(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .whenComplete((result, throwable) -> {
                if (throwable != null) {
                    future.completeExceptionally(throwable);
                }
            });

        return future;
    }

    /**
     * Aktualisiert die Position eines Charakters
     *
     * @param characterId Die Charakter-ID
     * @param x Neue X-Koordinate
     * @param y Neue Y-Koordinate
     * @param z Neue Z-Koordinate
     * @return CompletableFuture für asynchrone Verarbeitung
     */
    public CompletableFuture<Void> updateCharacterPosition(Long characterId, Double x, Double y, Double z) {
        String messageId = UUID.randomUUID().toString();

        CharacterData characterData = CharacterData.newBuilder()
                .setCharacterId(characterId)
                .setX(x)
                .setY(y)
                .setZ(z)
                .build();

        CharacterOperationMessage message = CharacterOperationMessage.newBuilder()
                .setMessageId(messageId)
                .setOperation(de.mhus.nimbus.shared.avro.CharacterOperationType.UPDATE_POSITION)
                .setCharacterData(characterData)
                .setTimestamp(Instant.now())
                .build();

        LOGGER.info("Updating position of character {} to ({}, {}, {}) with messageId {}",
                   characterId, x, y, z, messageId);

        CompletableFuture<Void> future = new CompletableFuture<>();
        pendingCharacterOperations.put(messageId, future);

        sendMessage(CHARACTER_OPERATIONS_TOPIC, messageId, message)
            .orTimeout(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .whenComplete((result, throwable) -> {
                if (throwable != null) {
                    future.completeExceptionally(throwable);
                }
            });

        return future;
    }

    /**
     * Aktualisiert die Gesundheit eines Charakters
     *
     * @param characterId Die Charakter-ID
     * @param health Neue Gesundheitspunkte
     * @return CompletableFuture für asynchrone Verarbeitung
     */
    public CompletableFuture<Void> updateCharacterHealth(Long characterId, Integer health) {
        String messageId = UUID.randomUUID().toString();

        CharacterData characterData = CharacterData.newBuilder()
                .setCharacterId(characterId)
                .setHealth(health)
                .build();

        CharacterOperationMessage message = CharacterOperationMessage.newBuilder()
                .setMessageId(messageId)
                .setOperation(de.mhus.nimbus.shared.avro.CharacterOperationType.UPDATE_HEALTH)
                .setCharacterData(characterData)
                .setTimestamp(Instant.now())
                .build();

        LOGGER.info("Updating health of character {} to {} with messageId {}",
                   characterId, health, messageId);

        CompletableFuture<Void> future = new CompletableFuture<>();
        pendingCharacterOperations.put(messageId, future);

        sendMessage(CHARACTER_OPERATIONS_TOPIC, messageId, message)
            .orTimeout(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .whenComplete((result, throwable) -> {
                if (throwable != null) {
                    future.completeExceptionally(throwable);
                }
            });

        return future;
    }

    /**
     * Aktualisiert die Informationen eines Charakters
     *
     * @param characterId Die Charakter-ID
     * @param name Neuer Name (optional)
     * @param displayName Neuer Display-Name (optional)
     * @param description Neue Beschreibung (optional)
     * @return CompletableFuture für asynchrone Verarbeitung
     */
    public CompletableFuture<Void> updateCharacterInfo(Long characterId, String name, String displayName, String description) {
        String messageId = UUID.randomUUID().toString();

        CharacterData.Builder builder = CharacterData.newBuilder()
                .setCharacterId(characterId);

        if (name != null) {
            builder.setName(name);
        }
        if (displayName != null) {
            builder.setDisplayName(displayName);
        }
        if (description != null) {
            builder.setDescription(description);
        }

        CharacterData characterData = builder.build();

        CharacterOperationMessage message = CharacterOperationMessage.newBuilder()
                .setMessageId(messageId)
                .setOperation(de.mhus.nimbus.shared.avro.CharacterOperationType.UPDATE_INFO)
                .setCharacterData(characterData)
                .setTimestamp(Instant.now())
                .build();

        LOGGER.info("Updating info of character {} with messageId {}", characterId, messageId);

        CompletableFuture<Void> future = new CompletableFuture<>();
        pendingCharacterOperations.put(messageId, future);

        sendMessage(CHARACTER_OPERATIONS_TOPIC, messageId, message)
            .orTimeout(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .whenComplete((result, throwable) -> {
                if (throwable != null) {
                    future.completeExceptionally(throwable);
                }
            });

        return future;
    }

    /**
     * Löscht einen Charakter
     *
     * @param characterId Die Charakter-ID
     * @return CompletableFuture für asynchrone Verarbeitung
     */
    public CompletableFuture<Void> deleteCharacter(Long characterId) {
        String messageId = UUID.randomUUID().toString();

        CharacterData characterData = CharacterData.newBuilder()
                .setCharacterId(characterId)
                .build();

        CharacterOperationMessage message = CharacterOperationMessage.newBuilder()
                .setMessageId(messageId)
                .setOperation(de.mhus.nimbus.shared.avro.CharacterOperationType.DELETE)
                .setCharacterData(characterData)
                .setTimestamp(Instant.now())
                .build();

        LOGGER.info("Deleting character {} with messageId {}", characterId, messageId);

        CompletableFuture<Void> future = new CompletableFuture<>();
        pendingCharacterOperations.put(messageId, future);

        sendMessage(CHARACTER_OPERATIONS_TOPIC, messageId, message)
            .orTimeout(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .whenComplete((result, throwable) -> {
                if (throwable != null) {
                    future.completeExceptionally(throwable);
                }
            });

        return future;
    }

    /**
     * Handler für eingehende PlayerCharacterLookup-Responses
     * Diese Methode sollte von einem Kafka-Consumer aufgerufen werden
     *
     * @param response Die PlayerCharacterLookupResponse
     * @return true wenn die Response zugeordnet werden konnte, false sonst
     */
    public boolean handleCharacterLookupResponse(PlayerCharacterLookupResponse response) {
        String requestId = response.getRequestId();
        CompletableFuture<PlayerCharacterLookupResponse> future = pendingCharacterLookups.remove(requestId);

        if (future != null) {
            LOGGER.debug("Completing character lookup request {} with status {}", requestId, response.getStatus());
            future.complete(response);
            return true;
        } else {
            LOGGER.warn("Received character lookup response for unknown request ID: {}", requestId);
            return false;
        }
    }

    /**
     * Handler für Character-Operation-Bestätigungen
     * Diese Methode wird aufgerufen, wenn eine Character-Operation erfolgreich verarbeitet wurde
     *
     * @param messageId Die Message-ID der ursprünglichen Operation
     * @return true wenn die Response zugeordnet werden konnte, false sonst
     */
    public boolean handleCharacterOperationConfirmation(String messageId) {
        CompletableFuture<Void> future = pendingCharacterOperations.remove(messageId);

        if (future != null) {
            LOGGER.debug("Completing character operation with messageId {}", messageId);
            future.complete(null);
            return true;
        } else {
            LOGGER.warn("Received character operation confirmation for unknown message ID: {}", messageId);
            return false;
        }
    }

    /**
     * Handler für Character-Operation-Fehler
     * Diese Methode wird aufgerufen, wenn eine Character-Operation fehlgeschlagen ist
     *
     * @param messageId Die Message-ID der ursprünglichen Operation
     * @param error Die Fehlermeldung
     * @return true wenn die Response zugeordnet werden konnte, false sonst
     */
    public boolean handleCharacterOperationError(String messageId, String error) {
        CompletableFuture<Void> future = pendingCharacterOperations.remove(messageId);

        if (future != null) {
            LOGGER.debug("Completing character operation with error for messageId {}: {}", messageId, error);
            future.completeExceptionally(new RuntimeException("Character operation failed: " + error));
            return true;
        } else {
            LOGGER.warn("Received character operation error for unknown message ID: {}", messageId);
            return false;
        }
    }

    /**
     * Bereinigt abgelaufene Requests aus den pending Maps
     * Diese Methode sollte periodisch aufgerufen werden
     */
    public void cleanupExpiredRequests() {
        long expiredCount = 0;

        // Cleanup für Character Lookup Requests
        expiredCount += cleanupExpiredRequests(pendingCharacterLookups, "character lookup");

        // Cleanup für Character Operation Requests
        expiredCount += cleanupExpiredRequests(pendingCharacterOperations, "character operation");

        if (expiredCount > 0) {
            LOGGER.info("Cleaned up {} expired request(s)", expiredCount);
        }
    }

    /**
     * Hilfsmethode zum Bereinigen von abgelaufenen Requests
     */
    private <T> long cleanupExpiredRequests(ConcurrentHashMap<String, CompletableFuture<T>> pendingRequests, String requestType) {
        long removedCount = 0;
        var iterator = pendingRequests.entrySet().iterator();

        while (iterator.hasNext()) {
            var entry = iterator.next();
            CompletableFuture<T> future = entry.getValue();
            if (future.isDone() || future.isCancelled()) {
                LOGGER.debug("Removing completed/cancelled {} request: {}", requestType, entry.getKey());
                iterator.remove();
                removedCount++;
            }
        }

        return removedCount;
    }

    /**
     * Gibt die Anzahl der wartenden Requests zurück
     */
    public int getPendingRequestCount() {
        return pendingCharacterLookups.size() + pendingCharacterOperations.size();
    }

    /**
     * Gibt Statistiken über wartende Requests zurück
     */
    public String getPendingRequestStats() {
        return String.format("Pending requests - CharacterLookup: %d, CharacterOperations: %d",
                pendingCharacterLookups.size(),
                pendingCharacterOperations.size());
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
