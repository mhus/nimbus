package de.mhus.nimbus.common.client;

import de.mhus.nimbus.shared.dto.IdentityCharacterLookupMessage;
import de.mhus.nimbus.shared.dto.LoginOperationMessage;
import de.mhus.nimbus.shared.dto.UserLookupMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Client for communicating with identity module via Kafka
 */
@Component
public class IdentityClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(IdentityClient.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;

    // Kafka Topics
    private static final String LOGIN_REQUEST_TOPIC = "login-request";
    private static final String USER_LOOKUP_REQUEST_TOPIC = "user-lookup-request";
    private static final String IDENTITY_CHARACTER_LOOKUP_REQUEST_TOPIC = "identity-character-lookup-request";
    private static final String PUBLIC_KEY_REQUEST_TOPIC = "public-key-request";

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
     * @param rememberMe Soll die Anmeldung gespeichert werden
     * @return CompletableFuture für asynchrone Verarbeitung
     */
    public CompletableFuture<Void> requestLogin(String username, String password, String clientInfo, boolean rememberMe) {
        String requestId = UUID.randomUUID().toString();

        LoginOperationMessage message = new LoginOperationMessage();
        message.setRequestId(requestId);
        message.setUsername(username);
        message.setPassword(password);
        message.setClientInfo(clientInfo);
        message.setTimestamp(System.currentTimeMillis());
        message.setRememberMe(rememberMe);

        LOGGER.info("Sending login request for user '{}' with requestId {}", username, requestId);

        return sendMessage(LOGIN_REQUEST_TOPIC, requestId, message);
    }

    /**
     * Sendet eine einfache Login-Anfrage
     *
     * @param username Benutzername
     * @param password Passwort
     * @return CompletableFuture für asynchrone Verarbeitung
     */
    public CompletableFuture<Void> requestLogin(String username, String password) {
        return requestLogin(username, password, "IdentityClient", false);
    }

    /**
     * Sucht einen Benutzer anhand der Benutzer-ID
     *
     * @param userId Die Benutzer-ID
     * @return CompletableFuture für asynchrone Verarbeitung
     */
    public CompletableFuture<Void> lookupUserById(Long userId) {
        String requestId = UUID.randomUUID().toString();

        UserLookupMessage message = new UserLookupMessage();
        message.setRequestId(requestId);
        message.setUserId(userId);
        message.setIncludeInactive(false);

        LOGGER.info("Sending user lookup request for userId {} with requestId {}", userId, requestId);

        return sendMessage(USER_LOOKUP_REQUEST_TOPIC, requestId, message);
    }

    /**
     * Sucht einen Benutzer anhand des Benutzernamens
     *
     * @param username Der Benutzername
     * @return CompletableFuture für asynchrone Verarbeitung
     */
    public CompletableFuture<Void> lookupUserByUsername(String username) {
        String requestId = UUID.randomUUID().toString();

        UserLookupMessage message = new UserLookupMessage();
        message.setRequestId(requestId);
        message.setUsername(username);
        message.setIncludeInactive(false);

        LOGGER.info("Sending user lookup request for username '{}' with requestId {}", username, requestId);

        return sendMessage(USER_LOOKUP_REQUEST_TOPIC, requestId, message);
    }

    /**
     * Sucht einen Benutzer anhand der E-Mail-Adresse
     *
     * @param email Die E-Mail-Adresse
     * @return CompletableFuture für asynchrone Verarbeitung
     */
    public CompletableFuture<Void> lookupUserByEmail(String email) {
        String requestId = UUID.randomUUID().toString();

        UserLookupMessage message = new UserLookupMessage();
        message.setRequestId(requestId);
        message.setEmail(email);
        message.setIncludeInactive(false);

        LOGGER.info("Sending user lookup request for email '{}' with requestId {}", email, requestId);

        return sendMessage(USER_LOOKUP_REQUEST_TOPIC, requestId, message);
    }

    /**
     * Erweiterte Benutzer-Suche mit Option für inaktive Benutzer
     *
     * @param userId          Die Benutzer-ID (optional)
     * @param username        Der Benutzername (optional)
     * @param email           Die E-Mail-Adresse (optional)
     * @param includeInactive Sollen inaktive Benutzer eingeschlossen werden
     * @return CompletableFuture für asynchrone Verarbeitung
     */
    public CompletableFuture<Void> lookupUser(Long userId, String username, String email, boolean includeInactive) {
        String requestId = UUID.randomUUID().toString();

        UserLookupMessage message = new UserLookupMessage();
        message.setRequestId(requestId);
        message.setUserId(userId);
        message.setUsername(username);
        message.setEmail(email);
        message.setIncludeInactive(includeInactive);

        LOGGER.info("Sending extended user lookup request with requestId {}", requestId);

        return sendMessage(USER_LOOKUP_REQUEST_TOPIC, requestId, message);
    }

    /**
     * Sucht einen Identity-Charakter anhand der Charakter-ID
     *
     * @param characterId Die Charakter-ID
     * @return CompletableFuture für asynchrone Verarbeitung
     */
    public CompletableFuture<Void> lookupCharacterById(Long characterId) {
        String requestId = UUID.randomUUID().toString();

        IdentityCharacterLookupMessage message = new IdentityCharacterLookupMessage();
        message.setRequestId(requestId);
        message.setCharacterId(characterId);
        message.setActiveOnly(true);

        LOGGER.info("Sending character lookup request for characterId {} with requestId {}", characterId, requestId);

        return sendMessage(IDENTITY_CHARACTER_LOOKUP_REQUEST_TOPIC, requestId, message);
    }

    /**
     * Sucht einen Identity-Charakter anhand des Charakternamens
     *
     * @param characterName Der Charaktername
     * @return CompletableFuture für asynchrone Verarbeitung
     */
    public CompletableFuture<Void> lookupCharacterByName(String characterName) {
        String requestId = UUID.randomUUID().toString();

        IdentityCharacterLookupMessage message = new IdentityCharacterLookupMessage();
        message.setRequestId(requestId);
        message.setCharacterName(characterName);
        message.setActiveOnly(true);

        LOGGER.info("Sending character lookup request for characterName '{}' with requestId {}", characterName, requestId);

        return sendMessage(IDENTITY_CHARACTER_LOOKUP_REQUEST_TOPIC, requestId, message);
    }

    /**
     * Sucht Identity-Charaktere anhand der Benutzer-ID
     *
     * @param userId Die Benutzer-ID
     * @return CompletableFuture für asynchrone Verarbeitung
     */
    public CompletableFuture<Void> lookupCharactersByUserId(Long userId) {
        String requestId = UUID.randomUUID().toString();

        IdentityCharacterLookupMessage message = new IdentityCharacterLookupMessage();
        message.setRequestId(requestId);
        message.setUserId(userId);
        message.setActiveOnly(true);

        LOGGER.info("Sending character lookup request for userId {} with requestId {}", userId, requestId);

        return sendMessage(IDENTITY_CHARACTER_LOOKUP_REQUEST_TOPIC, requestId, message);
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
    public CompletableFuture<Void> lookupCharacter(Long characterId, String characterName, Long userId,
                                                   String currentPlanet, String currentWorldId, boolean activeOnly) {
        String requestId = UUID.randomUUID().toString();

        IdentityCharacterLookupMessage message = new IdentityCharacterLookupMessage();
        message.setRequestId(requestId);
        message.setCharacterId(characterId);
        message.setCharacterName(characterName);
        message.setUserId(userId);
        message.setCurrentPlanet(currentPlanet);
        message.setCurrentWorldId(currentWorldId);
        message.setActiveOnly(activeOnly);

        LOGGER.info("Sending extended character lookup request with requestId {}", requestId);

        return sendMessage(IDENTITY_CHARACTER_LOOKUP_REQUEST_TOPIC, requestId, message);
    }

    /**
     * Fordert den öffentlichen Schlüssel an
     *
     * @return CompletableFuture für asynchrone Verarbeitung
     */
    public CompletableFuture<Void> requestPublicKey() {
        String requestId = UUID.randomUUID().toString();

        LOGGER.info("Sending public key request with requestId {}", requestId);

        return sendMessage(PUBLIC_KEY_REQUEST_TOPIC, requestId, requestId);
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
