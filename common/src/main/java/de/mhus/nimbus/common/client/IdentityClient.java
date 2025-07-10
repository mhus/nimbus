package de.mhus.nimbus.common.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.mhus.nimbus.common.exception.NimbusException;
import de.mhus.nimbus.shared.avro.*;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Client für Identity-Service-Operationen
 * Verwendet Kafka für die Kommunikation mit dem Identity-Service
 */
@Component
@Slf4j
public class IdentityClient {


    private final KafkaTemplate<String, Object> kafkaTemplate;

    // Maps für pending Requests
    private final ConcurrentHashMap<String, CompletableFuture<LoginResponse>> pendingLoginRequests = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, CompletableFuture<UserLookupResponse>> pendingUserLookupRequests = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, CompletableFuture<PlayerCharacterLookupResponse>> pendingCharacterLookupRequests = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, CompletableFuture<PublicKeyResponse>> pendingPublicKeyRequests = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, CompletableFuture<AceCreateResponse>> pendingAceCreateRequests = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, CompletableFuture<AceLookupResponse>> pendingAceLookupRequests = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, CompletableFuture<AceUpdateResponse>> pendingAceUpdateRequests = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, CompletableFuture<AceDeleteResponse>> pendingAceDeleteRequests = new ConcurrentHashMap<>();

    // Kafka Topics
    private static final String LOGIN_REQUEST_TOPIC = "login-request";
    private static final String USER_LOOKUP_REQUEST_TOPIC = "user-lookup-request";
    private static final String IDENTITY_CHARACTER_LOOKUP_REQUEST_TOPIC = "identity-character-lookup-request";
    private static final String PUBLIC_KEY_REQUEST_TOPIC = "public-key-request";
    private static final String ACE_CREATE_REQUEST_TOPIC = "ace-create-request";
    private static final String ACE_LOOKUP_REQUEST_TOPIC = "ace-lookup-request";
    private static final String ACE_UPDATE_REQUEST_TOPIC = "ace-update-request";
    private static final String ACE_DELETE_REQUEST_TOPIC = "ace-delete-request";

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

        log.info("Sending login request for user '{}' with requestId {}", username, requestId);

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

        log.info("Sending user lookup request for userId {} with requestId {}", userId, requestId);

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

        log.info("Sending user lookup request for username '{}' with requestId {}", username, requestId);

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

        log.info("Sending user lookup request for email '{}' with requestId {}", email, requestId);

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

        log.info("Sending extended user lookup request with requestId {}", requestId);

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

        log.info("Sending character lookup request for characterId {} with requestId {}", characterId, requestId);

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

        log.info("Sending character lookup request for characterName '{}' with requestId {}", characterName, requestId);

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

        log.info("Sending character lookup request for userId {} with requestId {}", userId, requestId);

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

        log.info("Sending extended character lookup request with requestId {}", requestId);

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

        log.info("Sending public key request with requestId {}", requestId);

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
                .thenRun(() -> log.debug("Successfully sent message with ID {} to topic {}", messageId, topic))
                .handle((result, throwable) -> {
                    if (throwable != null) {
                        log.error("Failed to send message with ID {} to topic {}: {}",
                                   messageId, topic, throwable.getMessage(), throwable);
                        throw new RuntimeException(throwable);
                    }
                    return null;
                });

        } catch (Exception e) {
            log.error("Failed to send message with ID {} to topic {}: {}",
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
     * @return true wenn die Response zugeordnet werden konnte, false sonst
     */
    public boolean handleLoginResponse(LoginResponse response) {
        String requestId = response.getRequestId();
        CompletableFuture<LoginResponse> future = pendingLoginRequests.remove(requestId);

        if (future != null) {
            log.debug("Completing login request {} with status {}", requestId, response.getStatus());
            future.complete(response);
            return true;
        } else {
            log.warn("Received login response for unknown request ID: {}", requestId);
            return false;
        }
    }

    /**
     * Handler für eingehende UserLookup-Responses
     * Diese Methode sollte von einem Kafka-Consumer aufgerufen werden
     *
     * @param response Die UserLookupResponse
     * @return true wenn die Response zugeordnet werden konnte, false sonst
     */
    public boolean handleUserLookupResponse(UserLookupResponse response) {
        String requestId = response.getRequestId();
        CompletableFuture<UserLookupResponse> future = pendingUserLookupRequests.remove(requestId);

        if (future != null) {
            log.debug("Completing user lookup request {} with status {}", requestId, response.getStatus());
            future.complete(response);
            return true;
        } else {
            log.warn("Received user lookup response for unknown request ID: {}", requestId);
            return false;
        }
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
        CompletableFuture<PlayerCharacterLookupResponse> future = pendingCharacterLookupRequests.remove(requestId);

        if (future != null) {
            log.debug("Completing character lookup request {} with status {}", requestId, response.getStatus());
            future.complete(response);
            return true;
        } else {
            log.warn("Received character lookup response for unknown request ID: {}", requestId);
            return false;
        }
    }

    /**
     * Handler für eingehende PublicKey-Responses
     * Diese Methode sollte von einem Kafka-Consumer aufgerufen werden
     *
     * @param response Die PublicKeyResponse
     * @return true wenn die Response zugeordnet werden konnte, false sonst
     */
    public boolean handlePublicKeyResponse(PublicKeyResponse response) {
        String requestId = response.getRequestId();
        CompletableFuture<PublicKeyResponse> future = pendingPublicKeyRequests.remove(requestId);

        if (future != null) {
            log.debug("Completing public key request {} with status {}", requestId, response.getStatus());
            future.complete(response);
            return true;
        } else {
            log.warn("Received public key response for unknown request ID: {}", requestId);
            return false;
        }
    }

    /**
     * Handler für eingehende AceCreate-Responses
     * Diese Methode sollte von einem Kafka-Consumer aufgerufen werden
     *
     * @param response Die AceCreateResponse
     * @return true wenn die Response zugeordnet werden konnte, false sonst
     */
    public boolean handleAceCreateResponse(AceCreateResponse response) {
        String requestId = response.getRequestId();
        CompletableFuture<AceCreateResponse> future = pendingAceCreateRequests.remove(requestId);

        if (future != null) {
            log.debug("Completing ACE create request {} with success: {}", requestId, response.getSuccess());
            future.complete(response);
            return true;
        } else {
            log.warn("Received ACE create response for unknown request ID: {}", requestId);
            return false;
        }
    }

    /**
     * Handler für eingehende AceLookup-Responses
     * Diese Methode sollte von einem Kafka-Consumer aufgerufen werden
     *
     * @param response Die AceLookupResponse
     * @return true wenn die Response zugeordnet werden konnte, false sonst
     */
    public boolean handleAceLookupResponse(AceLookupResponse response) {
        String requestId = response.getRequestId();
        CompletableFuture<AceLookupResponse> future = pendingAceLookupRequests.remove(requestId);

        if (future != null) {
            log.debug("Completing ACE lookup request {} with success: {}, found {} ACEs",
                        requestId, response.getSuccess(), response.getAces().size());
            future.complete(response);
            return true;
        } else {
            log.warn("Received ACE lookup response for unknown request ID: {}", requestId);
            return false;
        }
    }

    /**
     * Handler für eingehende AceUpdate-Responses
     * Diese Methode sollte von einem Kafka-Consumer aufgerufen werden
     *
     * @param response Die AceUpdateResponse
     * @return true wenn die Response zugeordnet werden konnte, false sonst
     */
    public boolean handleAceUpdateResponse(AceUpdateResponse response) {
        String requestId = response.getRequestId();
        CompletableFuture<AceUpdateResponse> future = pendingAceUpdateRequests.remove(requestId);

        if (future != null) {
            log.debug("Completing ACE update request {} with success: {}", requestId, response.getSuccess());
            future.complete(response);
            return true;
        } else {
            log.warn("Received ACE update response for unknown request ID: {}", requestId);
            return false;
        }
    }

    /**
     * Handler für eingehende AceDelete-Responses
     * Diese Methode sollte von einem Kafka-Consumer aufgerufen werden
     *
     * @param response Die AceDeleteResponse
     * @return true wenn die Response zugeordnet werden konnte, false sonst
     */
    public boolean handleAceDeleteResponse(AceDeleteResponse response) {
        String requestId = response.getRequestId();
        CompletableFuture<AceDeleteResponse> future = pendingAceDeleteRequests.remove(requestId);

        if (future != null) {
            log.debug("Completing ACE delete request {} with success: {}, deleted {} ACEs",
                        requestId, response.getSuccess(), response.getDeletedCount());
            future.complete(response);
            return true;
        } else {
            log.warn("Received ACE delete response for unknown request ID: {}", requestId);
            return false;
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

        // Cleanup für ACE Create Requests
        expiredCount += cleanupExpiredRequests(pendingAceCreateRequests, "ACE create");

        // Cleanup für ACE Lookup Requests
        expiredCount += cleanupExpiredRequests(pendingAceLookupRequests, "ACE lookup");

        // Cleanup für ACE Update Requests
        expiredCount += cleanupExpiredRequests(pendingAceUpdateRequests, "ACE update");

        // Cleanup für ACE Delete Requests
        expiredCount += cleanupExpiredRequests(pendingAceDeleteRequests, "ACE delete");

        if (expiredCount > 0) {
            log.info("Cleaned up {} expired request(s)", expiredCount);
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
                log.debug("Removing completed/cancelled {} request: {}", requestType, entry.getKey());
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
               pendingPublicKeyRequests.size() +
               pendingAceCreateRequests.size() +
               pendingAceLookupRequests.size() +
               pendingAceUpdateRequests.size() +
               pendingAceDeleteRequests.size();
    }

    /**
     * Gibt Statistiken über wartende Requests zurück
     */
    public String getPendingRequestStats() {
        return String.format("Pending requests - Login: %d, UserLookup: %d, CharacterLookup: %d, PublicKey: %d, AceCreate: %d, AceLookup: %d, AceUpdate: %d, AceDelete: %d",
                pendingLoginRequests.size(),
                pendingUserLookupRequests.size(),
                pendingCharacterLookupRequests.size(),
                pendingPublicKeyRequests.size(),
                pendingAceCreateRequests.size(),
                pendingAceLookupRequests.size(),
                pendingAceUpdateRequests.size(),
                pendingAceDeleteRequests.size());
    }

    /**
     * Extrahiert Character-Namen aus einem JWT Token (ohne Validierung)
     * Nur für Testzwecke - in Produktionsumgebung sollte der Token validiert werden
     */
    @SuppressWarnings("unchecked")
    public List<String> extractCharacterNamesFromToken(String token) {
        try {
            // JWT Token besteht aus drei Teilen: header.payload.signature
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                log.warn("Invalid JWT token format");
                return List.of();
            }

            // Dekodiere den Payload (Base64URL)
            String payload = parts[1];
            // Base64URL zu Base64 konvertieren
            payload = payload.replace('-', '+').replace('_', '/');
            // Padding hinzufügen falls nötig
            while (payload.length() % 4 != 0) {
                payload += "=";
            }

            byte[] decodedBytes = Base64.getDecoder().decode(payload);
            String decodedPayload = new String(decodedBytes);

            // JSON parsen
            ObjectMapper mapper = new ObjectMapper();
            JsonNode jsonNode = mapper.readTree(decodedPayload);

            // Character-Namen extrahieren
            JsonNode characterNamesNode = jsonNode.get("characterNames");
            if (characterNamesNode != null && characterNamesNode.isArray()) {
                return mapper.convertValue(characterNamesNode, List.class);
            }

            return List.of();
        } catch (Exception e) {
            log.warn("Failed to extract character names from token", e);
            return List.of();
        }
    }

    /**
     * Extrahiert ACE-Regeln aus einem JWT Token (ohne Validierung)
     * Nur für Testzwecke - in Produktionsumgebung sollte der Token validiert werden
     */
    @SuppressWarnings("unchecked")
    public List<String> extractAceRulesFromToken(String token) {
        try {
            // JWT Token besteht aus drei Teilen: header.payload.signature
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                log.warn("Invalid JWT token format");
                return List.of();
            }

            // Dekodiere den Payload (Base64URL)
            String payload = parts[1];
            // Base64URL zu Base64 konvertieren
            payload = payload.replace('-', '+').replace('_', '/');
            // Padding hinzufügen falls nötig
            while (payload.length() % 4 != 0) {
                payload += "=";
            }

            byte[] decodedBytes = Base64.getDecoder().decode(payload);
            String decodedPayload = new String(decodedBytes);

            // JSON parsen
            ObjectMapper mapper = new ObjectMapper();
            JsonNode jsonNode = mapper.readTree(decodedPayload);

            // ACE-Regeln extrahieren
            JsonNode aceRulesNode = jsonNode.get("aceRules");
            if (aceRulesNode != null && aceRulesNode.isArray()) {
                return mapper.convertValue(aceRulesNode, List.class);
            }

            return List.of();
        } catch (Exception e) {
            log.warn("Failed to extract ACE rules from token", e);
            return List.of();
        }
    }

    /**
     * Extrahiert alle wichtigen Claims aus einem JWT Token (ohne Validierung)
     * Nur für Testzwecke - in Produktionsumgebung sollte der Token validiert werden
     */
    @SuppressWarnings("unchecked")
    public TokenClaims extractTokenClaims(String token) {
        try {
            // JWT Token besteht aus drei Teilen: header.payload.signature
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                log.warn("Invalid JWT token format");
                return new TokenClaims();
            }

            // Dekodiere den Payload (Base64URL)
            String payload = parts[1];
            // Base64URL zu Base64 konvertieren
            payload = payload.replace('-', '+').replace('_', '/');
            // Padding hinzufügen falls nötig
            while (payload.length() % 4 != 0) {
                payload += "=";
            }

            byte[] decodedBytes = Base64.getDecoder().decode(payload);
            String decodedPayload = new String(decodedBytes);

            // JSON parsen
            ObjectMapper mapper = new ObjectMapper();
            JsonNode jsonNode = mapper.readTree(decodedPayload);

            TokenClaims claims = new TokenClaims();

            // Standard Claims extrahieren
            JsonNode userIdNode = jsonNode.get("userId");
            if (userIdNode != null) {
                claims.setUserId(userIdNode.asLong());
            }

            JsonNode usernameNode = jsonNode.get("username");
            if (usernameNode != null) {
                claims.setUsername(usernameNode.asText());
            }

            JsonNode emailNode = jsonNode.get("email");
            if (emailNode != null) {
                claims.setEmail(emailNode.asText());
            }

            JsonNode subjectNode = jsonNode.get("sub");
            if (subjectNode != null) {
                claims.setSubject(subjectNode.asText());
            }

            JsonNode issuerNode = jsonNode.get("iss");
            if (issuerNode != null) {
                claims.setIssuer(issuerNode.asText());
            }

            // Character-Namen extrahieren
            JsonNode characterNamesNode = jsonNode.get("characterNames");
            if (characterNamesNode != null && characterNamesNode.isArray()) {
                claims.setCharacterNames(mapper.convertValue(characterNamesNode, List.class));
            }

            // ACE-Regeln extrahieren
            JsonNode aceRulesNode = jsonNode.get("aceRules");
            if (aceRulesNode != null && aceRulesNode.isArray()) {
                claims.setAceRules(mapper.convertValue(aceRulesNode, List.class));
            }

            return claims;
        } catch (Exception e) {
            log.warn("Failed to extract token claims", e);
            return new TokenClaims();
        }
    }

    /**
     * Hilfsklasse für Token Claims
     */
    public static class TokenClaims {
        private Long userId;
        private String username;
        private String email;
        private String subject;
        private String issuer;
        private List<String> characterNames = List.of();
        private List<String> aceRules = List.of();

        // Getters and Setters
        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }

        public String getSubject() { return subject; }
        public void setSubject(String subject) { this.subject = subject; }

        public String getIssuer() { return issuer; }
        public void setIssuer(String issuer) { this.issuer = issuer; }

        public List<String> getCharacterNames() { return characterNames; }
        public void setCharacterNames(List<String> characterNames) { this.characterNames = characterNames != null ? characterNames : List.of(); }

        public List<String> getAceRules() { return aceRules; }
        public void setAceRules(List<String> aceRules) { this.aceRules = aceRules != null ? aceRules : List.of(); }

        @Override
        public String toString() {
            return "TokenClaims{" +
                    "userId=" + userId +
                    ", username='" + username + '\'' +
                    ", email='" + email + '\'' +
                    ", subject='" + subject + '\'' +
                    ", issuer='" + issuer + '\'' +
                    ", characterNames=" + characterNames +
                    ", aceRules=" + aceRules +
                    '}';
        }
    }

    // ===== ACE (Access Control Entity) Methoden =====

    /**
     * Erstellt eine neue ACE
     *
     * @param rule        Die ACE-Regel
     * @param userId      Die Benutzer-ID
     * @param description Optionale Beschreibung
     * @return CompletableFuture für asynchrone Verarbeitung
     */
    public CompletableFuture<AceCreateResponse> createAce(String rule, Long userId, String description) {
        String requestId = UUID.randomUUID().toString();

        AceCreateRequest message = AceCreateRequest.newBuilder()
                .setRequestId(requestId)
                .setRule(rule)
                .setUserId(userId)
                .setDescription(description)
                .setTimestamp(Instant.now())
                .setRequestedBy("IdentityClient")
                .build();

        log.info("Sending ACE create request for user {} with rule '{}' and requestId {}", userId, rule, requestId);

        CompletableFuture<AceCreateResponse> future = new CompletableFuture<>();
        pendingAceCreateRequests.put(requestId, future);

        sendMessage(ACE_CREATE_REQUEST_TOPIC, requestId, message)
            .orTimeout(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .whenComplete((result, throwable) -> {
                if (throwable != null) {
                    future.completeExceptionally(throwable);
                }
            });

        return future;
    }

    /**
     * Erstellt eine neue ACE mit spezifischer Reihenfolge
     *
     * @param rule        Die ACE-Regel
     * @param userId      Die Benutzer-ID
     * @param orderValue  Die gewünschte Reihenfolge
     * @param description Optionale Beschreibung
     * @return CompletableFuture für asynchrone Verarbeitung
     */
    public CompletableFuture<AceCreateResponse> createAceWithOrder(String rule, Long userId, Integer orderValue, String description) {
        String requestId = UUID.randomUUID().toString();

        AceCreateRequest message = AceCreateRequest.newBuilder()
                .setRequestId(requestId)
                .setRule(rule)
                .setUserId(userId)
                .setOrderValue(orderValue)
                .setDescription(description)
                .setTimestamp(Instant.now())
                .setRequestedBy("IdentityClient")
                .build();

        log.info("Sending ACE create request for user {} with rule '{}', order {} and requestId {}", userId, rule, orderValue, requestId);

        CompletableFuture<AceCreateResponse> future = new CompletableFuture<>();
        pendingAceCreateRequests.put(requestId, future);

        sendMessage(ACE_CREATE_REQUEST_TOPIC, requestId, message)
            .orTimeout(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .whenComplete((result, throwable) -> {
                if (throwable != null) {
                    future.completeExceptionally(throwable);
                }
            });

        return future;
    }

    /**
     * Sucht ACE anhand der ACE-ID
     *
     * @param aceId Die ACE-ID
     * @return CompletableFuture für asynchrone Verarbeitung
     */
    public CompletableFuture<AceLookupResponse> lookupAceById(Long aceId) {
        String requestId = UUID.randomUUID().toString();

        AceLookupRequest message = AceLookupRequest.newBuilder()
                .setRequestId(requestId)
                .setAceId(aceId)
                .setTimestamp(Instant.now())
                .setRequestedBy("IdentityClient")
                .build();

        log.info("Sending ACE lookup request for aceId {} with requestId {}", aceId, requestId);

        CompletableFuture<AceLookupResponse> future = new CompletableFuture<>();
        pendingAceLookupRequests.put(requestId, future);

        sendMessage(ACE_LOOKUP_REQUEST_TOPIC, requestId, message)
            .orTimeout(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .whenComplete((result, throwable) -> {
                if (throwable != null) {
                    future.completeExceptionally(throwable);
                }
            });

        return future;
    }

    /**
     * Sucht alle ACEs für einen Benutzer
     *
     * @param userId     Die Benutzer-ID
     * @param activeOnly Nur aktive ACEs zurückgeben
     * @return CompletableFuture für asynchrone Verarbeitung
     */
    public CompletableFuture<AceLookupResponse> lookupAcesByUserId(Long userId, boolean activeOnly) {
        String requestId = UUID.randomUUID().toString();

        AceLookupRequest message = AceLookupRequest.newBuilder()
                .setRequestId(requestId)
                .setUserId(userId)
                .setActiveOnly(activeOnly)
                .setTimestamp(Instant.now())
                .setRequestedBy("IdentityClient")
                .build();

        log.info("Sending ACE lookup request for userId {} (activeOnly: {}) with requestId {}", userId, activeOnly, requestId);

        CompletableFuture<AceLookupResponse> future = new CompletableFuture<>();
        pendingAceLookupRequests.put(requestId, future);

        sendMessage(ACE_LOOKUP_REQUEST_TOPIC, requestId, message)
            .orTimeout(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .whenComplete((result, throwable) -> {
                if (throwable != null) {
                    future.completeExceptionally(throwable);
                }
            });

        return future;
    }

    /**
     * Sucht ACEs anhand eines Regel-Musters
     *
     * @param rulePattern Das Suchmuster für Regeln
     * @param activeOnly  Nur aktive ACEs zurückgeben
     * @return CompletableFuture für asynchrone Verarbeitung
     */
    public CompletableFuture<AceLookupResponse> lookupAcesByRule(String rulePattern, boolean activeOnly) {
        String requestId = UUID.randomUUID().toString();

        AceLookupRequest message = AceLookupRequest.newBuilder()
                .setRequestId(requestId)
                .setRulePattern(rulePattern)
                .setActiveOnly(activeOnly)
                .setTimestamp(Instant.now())
                .setRequestedBy("IdentityClient")
                .build();

        log.info("Sending ACE lookup request for rule pattern '{}' (activeOnly: {}) with requestId {}", rulePattern, activeOnly, requestId);

        CompletableFuture<AceLookupResponse> future = new CompletableFuture<>();
        pendingAceLookupRequests.put(requestId, future);

        sendMessage(ACE_LOOKUP_REQUEST_TOPIC, requestId, message)
            .orTimeout(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .whenComplete((result, throwable) -> {
                if (throwable != null) {
                    future.completeExceptionally(throwable);
                }
            });

        return future;
    }

    /**
     * Erweiterte ACE-Suche
     *
     * @param aceId       Spezifische ACE-ID (optional)
     * @param userId      Benutzer-ID (optional)
     * @param rulePattern Regel-Muster (optional)
     * @param activeOnly  Nur aktive ACEs
     * @return CompletableFuture für asynchrone Verarbeitung
     */
    public CompletableFuture<AceLookupResponse> lookupAces(Long aceId, Long userId, String rulePattern, boolean activeOnly) {
        String requestId = UUID.randomUUID().toString();

        AceLookupRequest.Builder builder = AceLookupRequest.newBuilder()
                .setRequestId(requestId)
                .setActiveOnly(activeOnly)
                .setTimestamp(Instant.now())
                .setRequestedBy("IdentityClient");

        if (aceId != null) {
            builder.setAceId(aceId);
        }
        if (userId != null) {
            builder.setUserId(userId);
        }
        if (rulePattern != null) {
            builder.setRulePattern(rulePattern);
        }

        AceLookupRequest message = builder.build();

        log.info("Sending extended ACE lookup request with requestId {}", requestId);

        CompletableFuture<AceLookupResponse> future = new CompletableFuture<>();
        pendingAceLookupRequests.put(requestId, future);

        sendMessage(ACE_LOOKUP_REQUEST_TOPIC, requestId, message)
            .orTimeout(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .whenComplete((result, throwable) -> {
                if (throwable != null) {
                    future.completeExceptionally(throwable);
                }
            });

        return future;
    }

    /**
     * Aktualisiert eine bestehende ACE
     *
     * @param aceId       Die ACE-ID
     * @param rule        Neue Regel (optional)
     * @param orderValue  Neue Reihenfolge (optional)
     * @param description Neue Beschreibung (optional)
     * @param active      Neuer Aktiv-Status (optional)
     * @return CompletableFuture für asynchrone Verarbeitung
     */
    public CompletableFuture<AceUpdateResponse> updateAce(Long aceId, String rule, Integer orderValue, String description, Boolean active) {
        String requestId = UUID.randomUUID().toString();

        AceUpdateRequest.Builder builder = AceUpdateRequest.newBuilder()
                .setRequestId(requestId)
                .setAceId(aceId)
                .setTimestamp(Instant.now())
                .setRequestedBy("IdentityClient");

        if (rule != null) {
            builder.setRule(rule);
        }
        if (orderValue != null) {
            builder.setOrderValue(orderValue);
        }
        if (description != null) {
            builder.setDescription(description);
        }
        if (active != null) {
            builder.setActive(active);
        }

        AceUpdateRequest message = builder.build();

        log.info("Sending ACE update request for aceId {} with requestId {}", aceId, requestId);

        CompletableFuture<AceUpdateResponse> future = new CompletableFuture<>();
        pendingAceUpdateRequests.put(requestId, future);

        sendMessage(ACE_UPDATE_REQUEST_TOPIC, requestId, message)
            .orTimeout(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .whenComplete((result, throwable) -> {
                if (throwable != null) {
                    future.completeExceptionally(throwable);
                }
            });

        return future;
    }

    /**
     * Löscht eine spezifische ACE
     *
     * @param aceId Die ACE-ID
     * @return CompletableFuture für asynchrone Verarbeitung
     */
    public CompletableFuture<AceDeleteResponse> deleteAce(Long aceId) {
        String requestId = UUID.randomUUID().toString();

        AceDeleteRequest message = AceDeleteRequest.newBuilder()
                .setRequestId(requestId)
                .setAceId(aceId)
                .setTimestamp(Instant.now())
                .setRequestedBy("IdentityClient")
                .build();

        log.info("Sending ACE delete request for aceId {} with requestId {}", aceId, requestId);

        CompletableFuture<AceDeleteResponse> future = new CompletableFuture<>();
        pendingAceDeleteRequests.put(requestId, future);

        sendMessage(ACE_DELETE_REQUEST_TOPIC, requestId, message)
            .orTimeout(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .whenComplete((result, throwable) -> {
                if (throwable != null) {
                    future.completeExceptionally(throwable);
                }
            });

        return future;
    }

    /**
     * Löscht alle ACEs für einen Benutzer
     *
     * @param userId Die Benutzer-ID
     * @return CompletableFuture für asynchrone Verarbeitung
     */
    public CompletableFuture<AceDeleteResponse> deleteAllAcesForUser(Long userId) {
        String requestId = UUID.randomUUID().toString();

        AceDeleteRequest message = AceDeleteRequest.newBuilder()
                .setRequestId(requestId)
                .setUserId(userId)
                .setTimestamp(Instant.now())
                .setRequestedBy("IdentityClient")
                .build();

        log.info("Sending ACE delete request for all ACEs of userId {} with requestId {}", userId, requestId);

        CompletableFuture<AceDeleteResponse> future = new CompletableFuture<>();
        pendingAceDeleteRequests.put(requestId, future);

        sendMessage(ACE_DELETE_REQUEST_TOPIC, requestId, message)
            .orTimeout(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .whenComplete((result, throwable) -> {
                if (throwable != null) {
                    future.completeExceptionally(throwable);
                }
            });

        return future;
    }

}
