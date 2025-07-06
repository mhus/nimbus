package de.mhus.nimbus.common.client;

import de.mhus.nimbus.shared.dto.PlanetLookupMessage;
import de.mhus.nimbus.shared.dto.PlanetRegistrationMessage;
import de.mhus.nimbus.shared.dto.WorldRegistrationMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Client for communicating with registry module via Kafka
 */
@Component
public class RegistryClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(RegistryClient.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;

    // Kafka Topics
    private static final String PLANET_REGISTRATION_TOPIC = "planet-registration";
    private static final String PLANET_UNREGISTRATION_TOPIC = "planet-unregistration";
    private static final String PLANET_LOOKUP_TOPIC = "planet-lookup";
    private static final String WORLD_REGISTRATION_TOPIC = "world-registration";
    private static final String WORLD_UNREGISTRATION_TOPIC = "world-unregistration";

    @Autowired
    public RegistryClient(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * Registriert einen neuen Planeten
     *
     * @param planetId       Die Planet-ID
     * @param planetName     Der Planetenname
     * @param description    Beschreibung des Planeten
     * @param serverAddress  Server-Adresse
     * @param serverPort     Server-Port
     * @param version        Server-Version
     * @param maxPlayers     Maximale Spieleranzahl
     * @param planetType     Typ des Planeten
     * @return CompletableFuture für asynchrone Verarbeitung
     */
    public CompletableFuture<Void> registerPlanet(String planetId, String planetName, String description,
                                                   String serverAddress, Integer serverPort, String version,
                                                   Integer maxPlayers, String planetType) {
        String requestId = UUID.randomUUID().toString();

        PlanetRegistrationMessage message = new PlanetRegistrationMessage();
        message.setRequestId(requestId);
        message.setPlanetId(planetId);
        message.setPlanetName(planetName);
        message.setDescription(description);
        message.setServerAddress(serverAddress);
        message.setServerPort(serverPort);
        message.setVersion(version);
        message.setMaxPlayers(maxPlayers);
        message.setCurrentPlayers(0);
        message.setPlanetType(planetType);
        message.setStatus("ACTIVE");
        message.setTimestamp(System.currentTimeMillis());

        LOGGER.info("Registering planet '{}' (ID: {}) at {}:{} with requestId {}",
                   planetName, planetId, serverAddress, serverPort, requestId);

        return sendMessage(PLANET_REGISTRATION_TOPIC, requestId, message);
    }

    /**
     * Registriert einen Planeten mit minimalen Informationen
     *
     * @param planetId      Die Planet-ID
     * @param planetName    Der Planetenname
     * @param serverAddress Server-Adresse
     * @param serverPort    Server-Port
     * @return CompletableFuture für asynchrone Verarbeitung
     */
    public CompletableFuture<Void> registerPlanet(String planetId, String planetName, String serverAddress, Integer serverPort) {
        return registerPlanet(planetId, planetName, null, serverAddress, serverPort, "1.0", 100, "DEFAULT");
    }

    /**
     * Deregistriert einen Planeten
     *
     * @param planetId Die Planet-ID
     * @return CompletableFuture für asynchrone Verarbeitung
     */
    public CompletableFuture<Void> unregisterPlanet(String planetId) {
        String requestId = UUID.randomUUID().toString();

        // Für Unregistrierung wird nur die Planet-ID benötigt
        PlanetRegistrationMessage message = new PlanetRegistrationMessage();
        message.setRequestId(requestId);
        message.setPlanetId(planetId);
        message.setTimestamp(System.currentTimeMillis());

        LOGGER.info("Unregistering planet with ID '{}' with requestId {}", planetId, requestId);

        return sendMessage(PLANET_UNREGISTRATION_TOPIC, requestId, message);
    }

    /**
     * Sucht nach einem Planeten anhand der ID
     *
     * @param planetId Die Planet-ID
     * @return CompletableFuture für asynchrone Verarbeitung
     */
    public CompletableFuture<Void> lookupPlanetById(String planetId) {
        String requestId = UUID.randomUUID().toString();

        PlanetLookupMessage message = new PlanetLookupMessage();
        message.setRequestId(requestId);
        message.setPlanetId(planetId);
        message.setIncludeInactive(false);

        LOGGER.info("Looking up planet by ID '{}' with requestId {}", planetId, requestId);

        return sendMessage(PLANET_LOOKUP_TOPIC, requestId, message);
    }

    /**
     * Sucht nach einem Planeten anhand des Namens
     *
     * @param planetName Der Planetenname
     * @return CompletableFuture für asynchrone Verarbeitung
     */
    public CompletableFuture<Void> lookupPlanetByName(String planetName) {
        String requestId = UUID.randomUUID().toString();

        PlanetLookupMessage message = new PlanetLookupMessage();
        message.setRequestId(requestId);
        message.setPlanetName(planetName);
        message.setIncludeInactive(false);

        LOGGER.info("Looking up planet by name '{}' with requestId {}", planetName, requestId);

        return sendMessage(PLANET_LOOKUP_TOPIC, requestId, message);
    }

    /**
     * Erweiterte Planeten-Suche
     *
     * @param planetId        Die Planet-ID (optional)
     * @param planetName      Der Planetenname (optional)
     * @param planetType      Der Planeten-Typ (optional)
     * @param status          Der Status (optional)
     * @param includeInactive Sollen inaktive Planeten eingeschlossen werden
     * @return CompletableFuture für asynchrone Verarbeitung
     */
    public CompletableFuture<Void> lookupPlanet(String planetId, String planetName, String planetType,
                                                String status, boolean includeInactive) {
        String requestId = UUID.randomUUID().toString();

        PlanetLookupMessage message = new PlanetLookupMessage();
        message.setRequestId(requestId);
        message.setPlanetId(planetId);
        message.setPlanetName(planetName);
        message.setPlanetType(planetType);
        message.setStatus(status);
        message.setIncludeInactive(includeInactive);

        LOGGER.info("Extended planet lookup with requestId {}", requestId);

        return sendMessage(PLANET_LOOKUP_TOPIC, requestId, message);
    }

    /**
     * Registriert eine neue Welt
     *
     * @param worldId       Die Welt-ID
     * @param worldName     Der Weltname
     * @param planetId      Die Planet-ID, zu der die Welt gehört
     * @param description   Beschreibung der Welt
     * @param worldType     Typ der Welt
     * @param maxPlayers    Maximale Spieleranzahl
     * @param configuration Welt-Konfiguration
     * @return CompletableFuture für asynchrone Verarbeitung
     */
    public CompletableFuture<Void> registerWorld(String worldId, String worldName, String planetId,
                                                 String description, String worldType, Integer maxPlayers,
                                                 String configuration) {
        String requestId = UUID.randomUUID().toString();

        WorldRegistrationMessage message = new WorldRegistrationMessage();
        message.setRequestId(requestId);
        message.setWorldId(worldId);
        message.setWorldName(worldName);
        message.setPlanetId(planetId);
        message.setDescription(description);
        message.setWorldType(worldType);
        message.setStatus("ACTIVE");
        message.setMaxPlayers(maxPlayers);
        message.setCurrentPlayers(0);
        message.setConfiguration(configuration);
        message.setTimestamp(System.currentTimeMillis());

        LOGGER.info("Registering world '{}' (ID: {}) on planet '{}' with requestId {}",
                   worldName, worldId, planetId, requestId);

        return sendMessage(WORLD_REGISTRATION_TOPIC, requestId, message);
    }

    /**
     * Registriert eine Welt mit minimalen Informationen
     *
     * @param worldId   Die Welt-ID
     * @param worldName Der Weltname
     * @param planetId  Die Planet-ID
     * @return CompletableFuture für asynchrone Verarbeitung
     */
    public CompletableFuture<Void> registerWorld(String worldId, String worldName, String planetId) {
        return registerWorld(worldId, worldName, planetId, null, "DEFAULT", 50, null);
    }

    /**
     * Deregistriert eine Welt
     *
     * @param worldId Die Welt-ID
     * @return CompletableFuture für asynchrone Verarbeitung
     */
    public CompletableFuture<Void> unregisterWorld(String worldId) {
        String requestId = UUID.randomUUID().toString();

        WorldRegistrationMessage message = new WorldRegistrationMessage();
        message.setRequestId(requestId);
        message.setWorldId(worldId);
        message.setTimestamp(System.currentTimeMillis());

        LOGGER.info("Unregistering world with ID '{}' with requestId {}", worldId, requestId);

        return sendMessage(WORLD_UNREGISTRATION_TOPIC, requestId, message);
    }

    /**
     * Aktualisiert die Spieleranzahl für einen Planeten
     *
     * @param planetId      Die Planet-ID
     * @param currentPlayers Aktuelle Spieleranzahl
     * @return CompletableFuture für asynchrone Verarbeitung
     */
    public CompletableFuture<Void> updatePlanetPlayerCount(String planetId, Integer currentPlayers) {
        String requestId = UUID.randomUUID().toString();

        PlanetRegistrationMessage message = new PlanetRegistrationMessage();
        message.setRequestId(requestId);
        message.setPlanetId(planetId);
        message.setCurrentPlayers(currentPlayers);
        message.setTimestamp(System.currentTimeMillis());

        LOGGER.info("Updating player count for planet '{}' to {} with requestId {}",
                   planetId, currentPlayers, requestId);

        return sendMessage(PLANET_REGISTRATION_TOPIC, requestId, message);
    }

    /**
     * Aktualisiert die Spieleranzahl für eine Welt
     *
     * @param worldId       Die Welt-ID
     * @param currentPlayers Aktuelle Spieleranzahl
     * @return CompletableFuture für asynchrone Verarbeitung
     */
    public CompletableFuture<Void> updateWorldPlayerCount(String worldId, Integer currentPlayers) {
        String requestId = UUID.randomUUID().toString();

        WorldRegistrationMessage message = new WorldRegistrationMessage();
        message.setRequestId(requestId);
        message.setWorldId(worldId);
        message.setCurrentPlayers(currentPlayers);
        message.setTimestamp(System.currentTimeMillis());

        LOGGER.info("Updating player count for world '{}' to {} with requestId {}",
                   worldId, currentPlayers, requestId);

        return sendMessage(WORLD_REGISTRATION_TOPIC, requestId, message);
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
