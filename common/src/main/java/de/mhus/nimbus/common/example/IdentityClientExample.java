package de.mhus.nimbus.common.example;

import de.mhus.nimbus.common.client.IdentityClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * Beispiel-Service, der zeigt, wie der IdentityClient verwendet wird
 */
@Service
public class IdentityClientExample {

    private static final Logger LOGGER = LoggerFactory.getLogger(IdentityClientExample.class);

    private final IdentityClient identityClient;

    @Autowired
    public IdentityClientExample(IdentityClient identityClient) {
        this.identityClient = identityClient;
    }

    /**
     * Beispiel: Führt einen Login-Vorgang durch
     */
    public CompletableFuture<Void> loginExample(String username, String password, boolean rememberMe) {
        LOGGER.info("Starte Login-Vorgang für Benutzer '{}'", username);

        return identityClient.requestLogin(username, password, "WebClient", rememberMe)
            .thenRun(() -> LOGGER.info("Login-Anfrage für '{}' erfolgreich gesendet", username))
            .exceptionally(throwable -> {
                LOGGER.error("Fehler beim Login für '{}': {}", username, throwable.getMessage());
                return null;
            });
    }

    /**
     * Beispiel: Einfacher Login ohne zusätzliche Optionen
     */
    public CompletableFuture<Void> simpleLoginExample(String username, String password) {
        LOGGER.info("Starte einfachen Login für Benutzer '{}'", username);

        return identityClient.requestLogin(username, password)
            .thenRun(() -> LOGGER.info("Einfache Login-Anfrage für '{}' erfolgreich gesendet", username))
            .exceptionally(throwable -> {
                LOGGER.error("Fehler beim einfachen Login für '{}': {}", username, throwable.getMessage());
                return null;
            });
    }

    /**
     * Beispiel: Sucht einen Benutzer anhand der ID
     */
    public CompletableFuture<Void> findUserByIdExample(Long userId) {
        LOGGER.info("Suche Benutzer mit ID {}", userId);

        return identityClient.lookupUserById(userId)
            .thenRun(() -> LOGGER.info("Benutzer-Suche für ID {} erfolgreich gesendet", userId))
            .exceptionally(throwable -> {
                LOGGER.error("Fehler bei der Benutzer-Suche für ID {}: {}", userId, throwable.getMessage());
                return null;
            });
    }

    /**
     * Beispiel: Sucht einen Benutzer anhand des Benutzernamens
     */
    public CompletableFuture<Void> findUserByUsernameExample(String username) {
        LOGGER.info("Suche Benutzer mit Benutzername '{}'", username);

        return identityClient.lookupUserByUsername(username)
            .thenRun(() -> LOGGER.info("Benutzer-Suche für '{}' erfolgreich gesendet", username))
            .exceptionally(throwable -> {
                LOGGER.error("Fehler bei der Benutzer-Suche für '{}': {}", username, throwable.getMessage());
                return null;
            });
    }

    /**
     * Beispiel: Sucht einen Benutzer anhand der E-Mail-Adresse
     */
    public CompletableFuture<Void> findUserByEmailExample(String email) {
        LOGGER.info("Suche Benutzer mit E-Mail '{}'", email);

        return identityClient.lookupUserByEmail(email)
            .thenRun(() -> LOGGER.info("Benutzer-Suche für E-Mail '{}' erfolgreich gesendet", email))
            .exceptionally(throwable -> {
                LOGGER.error("Fehler bei der Benutzer-Suche für E-Mail '{}': {}", email, throwable.getMessage());
                return null;
            });
    }

    /**
     * Beispiel: Erweiterte Benutzer-Suche mit mehreren Kriterien
     */
    public CompletableFuture<Void> advancedUserSearchExample(String username, String email, boolean includeInactive) {
        LOGGER.info("Starte erweiterte Benutzer-Suche (Username: '{}', E-Mail: '{}', Inaktive: {})",
                   username, email, includeInactive);

        return identityClient.lookupUser(null, username, email, includeInactive)
            .thenRun(() -> LOGGER.info("Erweiterte Benutzer-Suche erfolgreich gesendet"))
            .exceptionally(throwable -> {
                LOGGER.error("Fehler bei der erweiterten Benutzer-Suche: {}", throwable.getMessage());
                return null;
            });
    }

    /**
     * Beispiel: Sucht einen Charakter anhand der Charakter-ID
     */
    public CompletableFuture<Void> findCharacterByIdExample(Long characterId) {
        LOGGER.info("Suche Charakter mit ID {}", characterId);

        return identityClient.lookupCharacterById(characterId)
            .thenRun(() -> LOGGER.info("Charakter-Suche für ID {} erfolgreich gesendet", characterId))
            .exceptionally(throwable -> {
                LOGGER.error("Fehler bei der Charakter-Suche für ID {}: {}", characterId, throwable.getMessage());
                return null;
            });
    }

    /**
     * Beispiel: Sucht einen Charakter anhand des Charakternamens
     */
    public CompletableFuture<Void> findCharacterByNameExample(String characterName) {
        LOGGER.info("Suche Charakter mit Name '{}'", characterName);

        return identityClient.lookupCharacterByName(characterName)
            .thenRun(() -> LOGGER.info("Charakter-Suche für '{}' erfolgreich gesendet", characterName))
            .exceptionally(throwable -> {
                LOGGER.error("Fehler bei der Charakter-Suche für '{}': {}", characterName, throwable.getMessage());
                return null;
            });
    }

    /**
     * Beispiel: Sucht alle Charaktere eines Benutzers
     */
    public CompletableFuture<Void> findCharactersByUserExample(Long userId) {
        LOGGER.info("Suche alle Charaktere für Benutzer-ID {}", userId);

        return identityClient.lookupCharactersByUserId(userId)
            .thenRun(() -> LOGGER.info("Charakter-Suche für Benutzer {} erfolgreich gesendet", userId))
            .exceptionally(throwable -> {
                LOGGER.error("Fehler bei der Charakter-Suche für Benutzer {}: {}", userId, throwable.getMessage());
                return null;
            });
    }

    /**
     * Beispiel: Erweiterte Charakter-Suche mit Planeten-Filter
     */
    public CompletableFuture<Void> findCharactersOnPlanetExample(String planetName, boolean activeOnly) {
        LOGGER.info("Suche Charaktere auf Planet '{}' (Nur aktive: {})", planetName, activeOnly);

        return identityClient.lookupCharacter(null, null, null, planetName, null, activeOnly)
            .thenRun(() -> LOGGER.info("Charakter-Suche für Planet '{}' erfolgreich gesendet", planetName))
            .exceptionally(throwable -> {
                LOGGER.error("Fehler bei der Charakter-Suche für Planet '{}': {}", planetName, throwable.getMessage());
                return null;
            });
    }

    /**
     * Beispiel: Sucht Charaktere in einer bestimmten Welt
     */
    public CompletableFuture<Void> findCharactersInWorldExample(String worldId) {
        LOGGER.info("Suche Charaktere in Welt '{}'", worldId);

        return identityClient.lookupCharacter(null, null, null, null, worldId, true)
            .thenRun(() -> LOGGER.info("Charakter-Suche für Welt '{}' erfolgreich gesendet", worldId))
            .exceptionally(throwable -> {
                LOGGER.error("Fehler bei der Charakter-Suche für Welt '{}': {}", worldId, throwable.getMessage());
                return null;
            });
    }

    /**
     * Beispiel: Fordert den öffentlichen Schlüssel an
     */
    public CompletableFuture<Void> requestPublicKeyExample() {
        LOGGER.info("Fordere öffentlichen Schlüssel an");

        return identityClient.requestPublicKey()
            .thenRun(() -> LOGGER.info("Anfrage für öffentlichen Schlüssel erfolgreich gesendet"))
            .exceptionally(throwable -> {
                LOGGER.error("Fehler bei der Anfrage für öffentlichen Schlüssel: {}", throwable.getMessage());
                return null;
            });
    }

    /**
     * Beispiel: Kombinierte Operation - Login und dann Charakter-Suche
     */
    public CompletableFuture<Void> loginAndFindCharactersExample(String username, String password, Long userId) {
        LOGGER.info("Starte kombinierte Operation: Login für '{}' und Charakter-Suche", username);

        return identityClient.requestLogin(username, password)
            .thenCompose(result -> {
                LOGGER.info("Login erfolgreich, suche nun Charaktere für Benutzer {}", userId);
                return identityClient.lookupCharactersByUserId(userId);
            })
            .thenRun(() -> LOGGER.info("Kombinierte Operation erfolgreich abgeschlossen"))
            .exceptionally(throwable -> {
                LOGGER.error("Fehler bei der kombinierten Operation: {}", throwable.getMessage());
                return null;
            });
    }

    /**
     * Beispiel: Komplexe Suche - Benutzer und deren Charaktere auf einem bestimmten Planeten
     */
    public CompletableFuture<Void> complexSearchExample(String username, String planetName) {
        LOGGER.info("Starte komplexe Suche: Benutzer '{}' und deren Charaktere auf Planet '{}'", username, planetName);

        return identityClient.lookupUserByUsername(username)
            .thenCompose(result -> {
                LOGGER.info("Benutzer-Suche abgeschlossen, suche nun Charaktere auf Planet '{}'", planetName);
                return identityClient.lookupCharacter(null, null, null, planetName, null, true);
            })
            .thenRun(() -> LOGGER.info("Komplexe Suche erfolgreich abgeschlossen"))
            .exceptionally(throwable -> {
                LOGGER.error("Fehler bei der komplexen Suche: {}", throwable.getMessage());
                return null;
            });
    }

    /**
     * Beispiel: Batch-Operation für mehrere Benutzer-Lookups
     */
    public CompletableFuture<Void> batchUserLookupExample(String[] usernames) {
        LOGGER.info("Starte Batch-Lookup für {} Benutzer", usernames.length);

        CompletableFuture<Void>[] futures = new CompletableFuture[usernames.length];

        for (int i = 0; i < usernames.length; i++) {
            final String username = usernames[i];
            futures[i] = identityClient.lookupUserByUsername(username)
                .exceptionally(throwable -> {
                    LOGGER.warn("Fehler beim Lookup für Benutzer '{}': {}", username, throwable.getMessage());
                    return null;
                });
        }

        return CompletableFuture.allOf(futures)
            .thenRun(() -> LOGGER.info("Batch-Lookup für alle {} Benutzer abgeschlossen", usernames.length))
            .exceptionally(throwable -> {
                LOGGER.error("Fehler beim Batch-Lookup: {}", throwable.getMessage());
                return null;
            });
    }
}
