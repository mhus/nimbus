package de.mhus.nimbus.common.client;

import de.mhus.nimbus.shared.avro.PlanetRegistrationRequest;
import de.mhus.nimbus.shared.avro.PlanetUnregistrationRequest;
import de.mhus.nimbus.shared.avro.PlanetLookupRequest;
import de.mhus.nimbus.shared.avro.WorldRegistrationRequest;
import de.mhus.nimbus.shared.avro.WorldUnregistrationRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
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
     * @param planetName     Der Planetenname
     * @param description    Beschreibung des Planeten
     * @param galaxy         Galaxie-Name
     * @param sector         Sektor
     * @return CompletableFuture für asynchrone Verarbeitung
     */
    public CompletableFuture<Void> registerPlanet(String planetName, String description, String galaxy, String sector) {
        String requestId = UUID.randomUUID().toString();

        // Erstelle PlanetInfo-Objekt
        de.mhus.nimbus.shared.avro.PlanetInfo planetInfo = de.mhus.nimbus.shared.avro.PlanetInfo.newBuilder()
                .setDescription(description)
                .setGalaxy(galaxy)
                .setSector(sector)
                .build();

        PlanetRegistrationRequest message = PlanetRegistrationRequest.newBuilder()
                .setRequestId(requestId)
                .setPlanetName(planetName)
                .setEnvironment(de.mhus.nimbus.shared.avro.Environment.DEV)
                .setPlanetInfo(planetInfo)
                .setTimestamp(Instant.now())
                .build();

        LOGGER.info("Registering planet '{}' with requestId {}", planetName, requestId);

        return sendMessage(PLANET_REGISTRATION_TOPIC, requestId, message);
    }

    /**
     * Registriert einen Planeten mit minimalen Informationen
     *
     * @param planetName Der Planetenname
     * @return CompletableFuture für asynchrone Verarbeitung
     */
    public CompletableFuture<Void> registerPlanet(String planetName) {
        return registerPlanet(planetName, null, null, null);
    }

    /**
     * Deregistriert einen Planeten
     *
     * @param planetName Der Planetenname
     * @param reason Grund für die Deregistrierung
     * @return CompletableFuture für asynchrone Verarbeitung
     */
    public CompletableFuture<Void> unregisterPlanet(String planetName, String reason) {
        String requestId = UUID.randomUUID().toString();

        PlanetUnregistrationRequest message = PlanetUnregistrationRequest.newBuilder()
                .setRequestId(requestId)
                .setPlanetName(planetName)
                .setEnvironment(de.mhus.nimbus.shared.avro.Environment.DEV)
                .setTimestamp(Instant.now())
                .setUnregisteredBy("RegistryClient")
                .setReason(reason)
                .build();

        LOGGER.info("Unregistering planet '{}' with requestId {}", planetName, requestId);

        return sendMessage(PLANET_UNREGISTRATION_TOPIC, requestId, message);
    }

    /**
     * Deregistriert einen Planeten ohne Grund
     *
     * @param planetName Der Planetenname
     * @return CompletableFuture für asynchrone Verarbeitung
     */
    public CompletableFuture<Void> unregisterPlanet(String planetName) {
        return unregisterPlanet(planetName, null);
    }

    /**
     * Sucht nach einem Planeten anhand des Namens
     *
     * @param planetName Der Planetenname
     * @return CompletableFuture für asynchrone Verarbeitung
     */
    public CompletableFuture<Void> lookupPlanetByName(String planetName) {
        String requestId = UUID.randomUUID().toString();

        PlanetLookupRequest message = PlanetLookupRequest.newBuilder()
                .setRequestId(requestId)
                .setPlanetName(planetName)
                .setEnvironment(de.mhus.nimbus.shared.avro.Environment.DEV)
                .setTimestamp(Instant.now())
                .build();

        LOGGER.info("Looking up planet by name '{}' with requestId {}", planetName, requestId);

        return sendMessage(PLANET_LOOKUP_TOPIC, requestId, message);
    }

    /**
     * Sucht nach einem Planeten mit optionaler Welt-Spezifikation
     *
     * @param planetName Der Planetenname
     * @param worldName  Der Weltname (optional)
     * @return CompletableFuture für asynchrone Verarbeitung
     */
    public CompletableFuture<Void> lookupPlanet(String planetName, String worldName) {
        String requestId = UUID.randomUUID().toString();

        PlanetLookupRequest.Builder builder = PlanetLookupRequest.newBuilder()
                .setRequestId(requestId)
                .setPlanetName(planetName)
                .setEnvironment(de.mhus.nimbus.shared.avro.Environment.DEV)
                .setTimestamp(Instant.now());

        if (worldName != null) {
            builder.setWorldName(worldName);
        }

        PlanetLookupRequest message = builder.build();

        LOGGER.info("Looking up planet '{}' {} with requestId {}",
                   planetName, worldName != null ? "with world '" + worldName + "'" : "", requestId);

        return sendMessage(PLANET_LOOKUP_TOPIC, requestId, message);
    }

    /**
     * Registriert eine neue Welt
     *
     * @param worldId       Die Welt-ID
     * @param worldName     Der Weltname
     * @param planetName    Der Planetenname (nicht ID)
     * @param managementUrl Management-URL für die Welt
     * @param apiUrl        API-URL (optional)
     * @param webUrl        Web-URL (optional)
     * @return CompletableFuture für asynchrone Verarbeitung
     */
    public CompletableFuture<Void> registerWorld(String worldId, String worldName, String planetName,
                                                 String managementUrl, String apiUrl, String webUrl) {
        String requestId = UUID.randomUUID().toString();

        WorldRegistrationRequest.Builder builder = WorldRegistrationRequest.newBuilder()
                .setRequestId(requestId)
                .setWorldId(worldId)
                .setWorldName(worldName)
                .setPlanetName(planetName)
                .setEnvironment(de.mhus.nimbus.shared.avro.Environment.DEV)
                .setManagementUrl(managementUrl)
                .setTimestamp(Instant.now());

        if (apiUrl != null) {
            builder.setApiUrl(apiUrl);
        }
        if (webUrl != null) {
            builder.setWebUrl(webUrl);
        }

        WorldRegistrationRequest message = builder.build();

        LOGGER.info("Registering world '{}' (ID: {}) on planet '{}' with requestId {}",
                   worldName, worldId, planetName, requestId);

        return sendMessage(WORLD_REGISTRATION_TOPIC, requestId, message);
    }

    /**
     * Registriert eine Welt mit minimalen Informationen
     *
     * @param worldId   Die Welt-ID
     * @param worldName Der Weltname
     * @param planetName Der Planetenname
     * @param managementUrl Management-URL
     * @return CompletableFuture für asynchrone Verarbeitung
     */
    public CompletableFuture<Void> registerWorld(String worldId, String worldName, String planetName, String managementUrl) {
        return registerWorld(worldId, worldName, planetName, managementUrl, null, null);
    }

    /**
     * Deregistriert eine Welt
     *
     * @param worldId Die Welt-ID
     * @param planetName Der Planetenname (optional für Validierung)
     * @param reason Grund für die Deregistrierung
     * @return CompletableFuture für asynchrone Verarbeitung
     */
    public CompletableFuture<Void> unregisterWorld(String worldId, String planetName, String reason) {
        String requestId = UUID.randomUUID().toString();

        WorldUnregistrationRequest.Builder builder = WorldUnregistrationRequest.newBuilder()
                .setRequestId(requestId)
                .setWorldId(worldId)
                .setEnvironment(de.mhus.nimbus.shared.avro.Environment.DEV)
                .setTimestamp(Instant.now())
                .setUnregisteredBy("RegistryClient");

        if (planetName != null) {
            builder.setPlanetName(planetName);
        }
        if (reason != null) {
            builder.setReason(reason);
        }

        WorldUnregistrationRequest message = builder.build();

        LOGGER.info("Unregistering world with ID '{}' {} with requestId {}",
                   worldId, planetName != null ? "on planet '" + planetName + "'" : "", requestId);

        return sendMessage(WORLD_UNREGISTRATION_TOPIC, requestId, message);
    }

    /**
     * Deregistriert eine Welt mit minimalen Informationen
     *
     * @param worldId Die Welt-ID
     * @return CompletableFuture für asynchrone Verarbeitung
     */
    public CompletableFuture<Void> unregisterWorld(String worldId) {
        return unregisterWorld(worldId, null, null);
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
