package de.mhus.nimbus.common.client;

import de.mhus.nimbus.shared.avro.LoginRequest;
import de.mhus.nimbus.shared.avro.LoginResponse;
import de.mhus.nimbus.shared.avro.UserLookupRequest;
import de.mhus.nimbus.shared.avro.UserLookupResponse;
import de.mhus.nimbus.shared.avro.PlayerCharacterLookupRequest;
import de.mhus.nimbus.shared.avro.PlayerCharacterLookupResponse;
import de.mhus.nimbus.shared.avro.PublicKeyRequest;
import de.mhus.nimbus.shared.avro.PublicKeyResponse;
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
 * Client for communicating with identity module via Kafka
 */
@Component
public class IdentityClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(IdentityClient.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;

    // Maps für pending Requests
    private final ConcurrentHashMap<String, CompletableFuture<LoginResponse>> pendingLoginRequests = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, CompletableFuture<UserLookupResponse>> pendingUserLookupRequests = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, CompletableFuture<PlayerCharacterLookupResponse>> pendingCharacterLookupRequests = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, CompletableFuture<PublicKeyResponse>> pendingPublicKeyRequests = new ConcurrentHashMap<>();

    // Kafka Topics
    private static final String LOGIN_REQUEST_TOPIC = "login-request";
    private static final String USER_LOOKUP_REQUEST_TOPIC = "user-lookup-request";
    private static final String IDENTITY_CHARACTER_LOOKUP_REQUEST_TOPIC = "identity-character-lookup-request";
    private static final String PUBLIC_KEY_REQUEST_TOPIC = "public-key-request";

    // Default Timeout für Responses in Sekunden
    private static final int DEFAULT_TIMEOUT_SECONDS = 30;

    @Autowired
    public IdentityClient(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * Sendet eine Login-Anfrage
     *
     * @param username   Benutzername
     * @param password   Passwort
     * @param clientInfo Client-Informationen
     * @return CompletableFuture für asynchrone Verarbeitung
     */
    public CompletableFuture<LoginResponse> requestLogin(String username, String password, String clientInfo) {
        String requestId = UUID.randomUUID().toString();

        LoginRequest message = LoginRequest.newBuilder()
                .setRequestId(requestId)
                .setUsername(username)
                .setPassword(password)
                .setTimestamp(Instant.now())
                .setClientInfo(clientInfo)
                .build();

        LOGGER.info("Sending login request for user '{}' with requestId {}", username, requestId);

        CompletableFuture<LoginResponse> future = new CompletableFuture<>();
        pendingLoginRequests.put(requestId, future);

        sendMessage(LOGIN_REQUEST_TOPIC, requestId, message)
            .orTimeout(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .whenComplete((result, throwable) -> {
                if (throwable != null) {
                    future.completeExceptionally(throwable);
                }
            });

        return future;
    }

    /**
     * Sendet eine einfache Login-Anfrage
     *
     * @param username Benutzername
     * @param password Passwort
     * @return CompletableFuture für asynchrone Verarbeitung
     */
    public CompletableFuture<LoginResponse> requestLogin(String username, String password) {
        return requestLogin(username, password, "IdentityClient");
    }

    /**
     * Sucht einen Benutzer anhand der Benutzer-ID
     *
     * @param userId Die Benutzer-ID
     * @return CompletableFuture für asynchrone Verarbeitung
     */
    public CompletableFuture<UserLookupResponse> lookupUserById(Long userId) {
        String requestId = UUID.randomUUID().toString();

        UserLookupRequest message = UserLookupRequest.newBuilder()
                .setRequestId(requestId)
                .setUserId(userId)
                .setTimestamp(Instant.now())
                .setRequestedBy("IdentityClient")
                .build();

        LOGGER.info("Sending user lookup request for userId {} with requestId {}", userId, requestId);

        CompletableFuture<UserLookupResponse> future = new CompletableFuture<>();
        pendingUserLookupRequests.put(requestId, future);

        sendMessage(USER_LOOKUP_REQUEST_TOPIC, requestId, message)
            .orTimeout(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .whenComplete((result, throwable) -> {
                if (throwable != null) {
                    future.completeExceptionally(throwable);
                }
            });

        return future;
    }

    /**
     * Sucht einen Benutzer anhand des Benutzernamens
     *
     * @param username Der Benutzername
     * @return CompletableFuture für asynchrone Verarbeitung
     */
    public CompletableFuture<UserLookupResponse> lookupUserByUsername(String username) {
        String requestId = UUID.randomUUID().toString();

        UserLookupRequest message = UserLookupRequest.newBuilder()
                .setRequestId(requestId)
                .setUsername(username)
                .setTimestamp(Instant.now())
                .setRequestedBy("IdentityClient")
                .build();

        LOGGER.info("Sending user lookup request for username '{}' with requestId {}", username, requestId);

        CompletableFuture<UserLookupResponse> future = new CompletableFuture<>();
        pendingUserLookupRequests.put(requestId, future);

        sendMessage(USER_LOOKUP_REQUEST_TOPIC, requestId, message)
            .orTimeout(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .whenComplete((result, throwable) -> {
                if (throwable != null) {
                    future.completeExceptionally(throwable);
                }
            });

        return future;
    }

    /**
     * Sucht einen Benutzer anhand der E-Mail-Adresse
     *
     * @param email Die E-Mail-Adresse
     * @return CompletableFuture für asynchrone Verarbeitung
     */
    public CompletableFuture<UserLookupResponse> lookupUserByEmail(String email) {
        String requestId = UUID.randomUUID().toString();

        UserLookupRequest message = UserLookupRequest.newBuilder()
                .setRequestId(requestId)
                .setEmail(email)
                .setTimestamp(Instant.now())
                .setRequestedBy("IdentityClient")
                .build();

        LOGGER.info("Sending user lookup request for email '{}' with requestId {}", email, requestId);

        CompletableFuture<UserLookupResponse> future = new CompletableFuture<>();
        pendingUserLookupRequests.put(requestId, future);

        sendMessage(USER_LOOKUP_REQUEST_TOPIC, requestId, message)
            .orTimeout(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .whenComplete((result, throwable) -> {
                if (throwable != null) {
                    future.completeExceptionally(throwable);
                }
            });

        return future;
    }

    /**
     * Erweiterte Benutzer-Suche
     *
     * @param userId          Die Benutzer-ID (optional)
     * @param username        Der Benutzername (optional)
     * @param email           Die E-Mail-Adresse (optional)
     * @return CompletableFuture für asynchrone Verarbeitung
     */
    public CompletableFuture<UserLookupResponse> lookupUser(Long userId, String username, String email) {
        String requestId = UUID.randomUUID().toString();

        UserLookupRequest.Builder builder = UserLookupRequest.newBuilder()
                .setRequestId(requestId)
                .setTimestamp(Instant.now())
                .setRequestedBy("IdentityClient");

        if (userId != null) {
            builder.setUserId(userId);
        }
        if (username != null) {
            builder.setUsername(username);
        }
        if (email != null) {
            builder.setEmail(email);
        }

        UserLookupRequest message = builder.build();

        LOGGER.info("Sending extended user lookup request with requestId {}", requestId);

        CompletableFuture<UserLookupResponse> future = new CompletableFuture<>();
        pendingUserLookupRequests.put(requestId, future);

        sendMessage(USER_LOOKUP_REQUEST_TOPIC, requestId, message)
            .orTimeout(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .whenComplete((result, throwable) -> {
                if (throwable != null) {
                    future.completeExceptionally(throwable);
                }
            });

        return future;
    }

    /**
     * Sucht einen Identity-Charakter anhand der Charakter-ID
     *
     * @param characterId Die Charakter-ID
     * @return CompletableFuture für asynchrone Verarbeitung
     */
    public CompletableFuture<PlayerCharacterLookupResponse> lookupCharacterById(Long characterId) {
        String requestId = UUID.randomUUID().toString();

        PlayerCharacterLookupRequest message = PlayerCharacterLookupRequest.newBuilder()
                .setRequestId(requestId)
                .setCharacterId(characterId)
                .setActiveOnly(true)
                .setTimestamp(Instant.now())
                .setRequestedBy("IdentityClient")
                .build();

        LOGGER.info("Sending character lookup request for characterId {} with requestId {}", characterId, requestId);

        CompletableFuture<PlayerCharacterLookupResponse> future = new CompletableFuture<>();
        pendingCharacterLookupRequests.put(requestId, future);

        sendMessage(IDENTITY_CHARACTER_LOOKUP_REQUEST_TOPIC, requestId, message)
            .orTimeout(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .whenComplete((result, throwable) -> {
                if (throwable != null) {
                    future.completeExceptionally(throwable);
                }
            });

        return future;
    }

    /**
     * Sucht einen Identity-Charakter anhand des Charakternamens
     *
     * @param characterName Der Charaktername
     * @return CompletableFuture für asynchrone Verarbeitung
     */
    public CompletableFuture<PlayerCharacterLookupResponse> lookupCharacterByName(String characterName) {
        String requestId = UUID.randomUUID().toString();

        PlayerCharacterLookupRequest message = PlayerCharacterLookupRequest.newBuilder()
                .setRequestId(requestId)
                .setCharacterName(characterName)
                .setActiveOnly(true)
                .setTimestamp(Instant.now())
                .setRequestedBy("IdentityClient")
                .build();

        LOGGER.info("Sending character lookup request for characterName '{}' with requestId {}", characterName, requestId);

        CompletableFuture<PlayerCharacterLookupResponse> future = new CompletableFuture<>();
        pendingCharacterLookupRequests.put(requestId, future);

        sendMessage(IDENTITY_CHARACTER_LOOKUP_REQUEST_TOPIC, requestId, message)
            .orTimeout(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .whenComplete((result, throwable) -> {
                if (throwable != null) {
                    future.completeExceptionally(throwable);
                }
            });

        return future;
    }

    /**
     * Sucht Identity-Charaktere anhand der Benutzer-ID
     *
     * @param userId Die Benutzer-ID
     * @return CompletableFuture für asynchrone Verarbeitung
     */
    public CompletableFuture<PlayerCharacterLookupResponse> lookupCharactersByUserId(Long userId) {
        String requestId = UUID.randomUUID().toString();

        PlayerCharacterLookupRequest message = PlayerCharacterLookupRequest.newBuilder()
                .setRequestId(requestId)
                .setUserId(userId)
                .setActiveOnly(true)
                .setTimestamp(Instant.now())
                .setRequestedBy("IdentityClient")
                .build();

        LOGGER.info("Sending character lookup request for userId {} with requestId {}", userId, requestId);

        CompletableFuture<PlayerCharacterLookupResponse> future = new CompletableFuture<>();
        pendingCharacterLookupRequests.put(requestId, future);

        sendMessage(IDENTITY_CHARACTER_LOOKUP_REQUEST_TOPIC, requestId, message)
            .orTimeout(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .whenComplete((result, throwable) -> {
                if (throwable != null) {
                    future.completeExceptionally(throwable);
                }
            });

        return future;
    }

    /**
     * Erweiterte Charakter-Suche mit Planeten- und Welt-Filtern
     *
     * @param characterId     Die Charakter-ID (optional)
     * @param characterName   Der Charaktername (optional)
     * @param userId          Die Benutzer-ID (optional)
     * @param currentPlanet   Der aktuelle Planet (optional)
     * @param currentWorldId  Die aktuelle Welt-ID (optional)
     * @param activeOnly      Nur aktive Charaktere
     * @return CompletableFuture für asynchrone Verarbeitung
     */
    public CompletableFuture<PlayerCharacterLookupResponse> lookupCharacter(Long characterId, String characterName, Long userId,
                                                   String currentPlanet, String currentWorldId, boolean activeOnly) {
        String requestId = UUID.randomUUID().toString();

        PlayerCharacterLookupRequest.Builder builder = PlayerCharacterLookupRequest.newBuilder()
                .setRequestId(requestId)
                .setActiveOnly(activeOnly)
                .setTimestamp(Instant.now())
                .setRequestedBy("IdentityClient");

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

        LOGGER.info("Sending extended character lookup request with requestId {}", requestId);

        CompletableFuture<PlayerCharacterLookupResponse> future = new CompletableFuture<>();
        pendingCharacterLookupRequests.put(requestId, future);

        sendMessage(IDENTITY_CHARACTER_LOOKUP_REQUEST_TOPIC, requestId, message)
            .orTimeout(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .whenComplete((result, throwable) -> {
                if (throwable != null) {
                    future.completeExceptionally(throwable);
                }
            });

        return future;
    }

    /**
     * Fordert den öffentlichen Schlüssel an
     *
     * @return CompletableFuture für asynchrone Verarbeitung
     */
    public CompletableFuture<PublicKeyResponse> requestPublicKey() {
        String requestId = UUID.randomUUID().toString();

        PublicKeyRequest message = PublicKeyRequest.newBuilder()
                .setRequestId(requestId)
                .setTimestamp(Instant.now())
                .setRequestedBy("IdentityClient")
                .build();

        LOGGER.info("Sending public key request with requestId {}", requestId);

        CompletableFuture<PublicKeyResponse> future = new CompletableFuture<>();
        pendingPublicKeyRequests.put(requestId, future);

        sendMessage(PUBLIC_KEY_REQUEST_TOPIC, requestId, message)
            .orTimeout(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .whenComplete((result, throwable) -> {
                if (throwable != null) {
                    future.completeExceptionally(throwable);
                }
            });

        return future;
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

    /**
     * Handler für eingehende Login-Responses
     * Diese Methode sollte von einem Kafka-Consumer aufgerufen werden
     *
     * @param response Die LoginResponse
     */
    public void handleLoginResponse(LoginResponse response) {
        String requestId = response.getRequestId();
        CompletableFuture<LoginResponse> future = pendingLoginRequests.remove(requestId);

        if (future != null) {
            LOGGER.debug("Completing login request {} with status {}", requestId, response.getStatus());
            future.complete(response);
        } else {
            LOGGER.warn("Received login response for unknown request ID: {}", requestId);
        }
    }

    /**
     * Handler für eingehende UserLookup-Responses
     * Diese Methode sollte von einem Kafka-Consumer aufgerufen werden
     *
     * @param response Die UserLookupResponse
     */
    public void handleUserLookupResponse(UserLookupResponse response) {
        String requestId = response.getRequestId();
        CompletableFuture<UserLookupResponse> future = pendingUserLookupRequests.remove(requestId);

        if (future != null) {
            LOGGER.debug("Completing user lookup request {} with status {}", requestId, response.getStatus());
            future.complete(response);
        } else {
            LOGGER.warn("Received user lookup response for unknown request ID: {}", requestId);
        }
    }

    /**
     * Handler für eingehende PlayerCharacterLookup-Responses
     * Diese Methode sollte von einem Kafka-Consumer aufgerufen werden
     *
     * @param response Die PlayerCharacterLookupResponse
     */
    public void handleCharacterLookupResponse(PlayerCharacterLookupResponse response) {
        String requestId = response.getRequestId();
        CompletableFuture<PlayerCharacterLookupResponse> future = pendingCharacterLookupRequests.remove(requestId);

        if (future != null) {
            LOGGER.debug("Completing character lookup request {} with status {}", requestId, response.getStatus());
            future.complete(response);
        } else {
            LOGGER.warn("Received character lookup response for unknown request ID: {}", requestId);
        }
    }

    /**
     * Handler für eingehende PublicKey-Responses
     * Diese Methode sollte von einem Kafka-Consumer aufgerufen werden
     *
     * @param response Die PublicKeyResponse
     */
    public void handlePublicKeyResponse(PublicKeyResponse response) {
        String requestId = response.getRequestId();
        CompletableFuture<PublicKeyResponse> future = pendingPublicKeyRequests.remove(requestId);

        if (future != null) {
            LOGGER.debug("Completing public key request {} with status {}", requestId, response.getStatus());
            future.complete(response);
        } else {
            LOGGER.warn("Received public key response for unknown request ID: {}", requestId);
        }
    }

    /**
     * Bereinigt abgelaufene Requests aus den pending Maps
     * Diese Methode sollte periodisch aufgerufen werden
     */
    public void cleanupExpiredRequests() {
        long expiredCount = 0;

        // Cleanup für Login Requests
        expiredCount += cleanupExpiredRequests(pendingLoginRequests, "login");

        // Cleanup für User Lookup Requests
        expiredCount += cleanupExpiredRequests(pendingUserLookupRequests, "user lookup");

        // Cleanup für Character Lookup Requests
        expiredCount += cleanupExpiredRequests(pendingCharacterLookupRequests, "character lookup");

        // Cleanup für Public Key Requests
        expiredCount += cleanupExpiredRequests(pendingPublicKeyRequests, "public key");

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
        return pendingLoginRequests.size() +
               pendingUserLookupRequests.size() +
               pendingCharacterLookupRequests.size() +
               pendingPublicKeyRequests.size();
    }

    /**
     * Gibt Statistiken über wartende Requests zurück
     */
    public String getPendingRequestStats() {
        return String.format("Pending requests - Login: %d, UserLookup: %d, CharacterLookup: %d, PublicKey: %d",
                pendingLoginRequests.size(),
                pendingUserLookupRequests.size(),
                pendingCharacterLookupRequests.size(),
                pendingPublicKeyRequests.size());
    }
}
