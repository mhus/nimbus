package de.mhus.nimbus.common.client;

import de.mhus.nimbus.shared.avro.PlanetRegistrationRequest;
import de.mhus.nimbus.shared.avro.PlanetRegistrationResponse;
import de.mhus.nimbus.shared.avro.PlanetUnregistrationRequest;
import de.mhus.nimbus.shared.avro.PlanetUnregistrationResponse;
import de.mhus.nimbus.shared.avro.PlanetLookupRequest;
import de.mhus.nimbus.shared.avro.PlanetLookupResponse;
import de.mhus.nimbus.shared.avro.WorldRegistrationRequest;
import de.mhus.nimbus.shared.avro.WorldRegistrationResponse;
import de.mhus.nimbus.shared.avro.WorldUnregistrationRequest;
import de.mhus.nimbus.shared.avro.WorldUnregistrationResponse;
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
 * Client for communicating with registry module via Kafka
 */
@Component
public class RegistryClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(RegistryClient.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;

    // Maps für pending Requests
    private final ConcurrentHashMap<String, CompletableFuture<PlanetRegistrationResponse>> pendingPlanetRegistrations = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, CompletableFuture<PlanetUnregistrationResponse>> pendingPlanetUnregistrations = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, CompletableFuture<PlanetLookupResponse>> pendingPlanetLookups = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, CompletableFuture<WorldRegistrationResponse>> pendingWorldRegistrations = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, CompletableFuture<WorldUnregistrationResponse>> pendingWorldUnregistrations = new ConcurrentHashMap<>();

    // Kafka Topics
    private static final String PLANET_REGISTRATION_TOPIC = "planet-registration";
    private static final String PLANET_UNREGISTRATION_TOPIC = "planet-unregistration";
    private static final String PLANET_LOOKUP_TOPIC = "planet-lookup";
    private static final String WORLD_REGISTRATION_TOPIC = "world-registration";
    private static final String WORLD_UNREGISTRATION_TOPIC = "world-unregistration";

    // Default Timeout für Responses in Sekunden
    private static final int DEFAULT_TIMEOUT_SECONDS = 30;

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
     * Registriert einen neuen Planeten mit Response-Handling
     *
     * @param planetName     Der Planetenname
     * @param description    Beschreibung des Planeten
     * @param galaxy         Galaxie-Name
     * @param sector         Sektor
     * @return CompletableFuture mit PlanetRegistrationResponse
     */
    public CompletableFuture<PlanetRegistrationResponse> registerPlanetWithResponse(String planetName, String description, String galaxy, String sector) {
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

        LOGGER.info("Registering planet '{}' with requestId {} (with response)", planetName, requestId);

        CompletableFuture<PlanetRegistrationResponse> future = new CompletableFuture<>();
        pendingPlanetRegistrations.put(requestId, future);

        sendMessage(PLANET_REGISTRATION_TOPIC, requestId, message)
            .orTimeout(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .whenComplete((result, throwable) -> {
                if (throwable != null) {
                    future.completeExceptionally(throwable);
                }
            });

        return future;
    }

    /**
     * Registriert einen Planeten mit minimalen Informationen und Response-Handling
     *
     * @param planetName Der Planetenname
     * @return CompletableFuture mit PlanetRegistrationResponse
     */
    public CompletableFuture<PlanetRegistrationResponse> registerPlanetWithResponse(String planetName) {
        return registerPlanetWithResponse(planetName, null, null, null);
    }

    /**
     * Deregistriert einen Planeten mit Response-Handling
     *
     * @param planetName Der Planetenname
     * @param reason Grund für die Deregistrierung
     * @return CompletableFuture mit PlanetUnregistrationResponse
     */
    public CompletableFuture<PlanetUnregistrationResponse> unregisterPlanetWithResponse(String planetName, String reason) {
        String requestId = UUID.randomUUID().toString();

        PlanetUnregistrationRequest message = PlanetUnregistrationRequest.newBuilder()
                .setRequestId(requestId)
                .setPlanetName(planetName)
                .setEnvironment(de.mhus.nimbus.shared.avro.Environment.DEV)
                .setTimestamp(Instant.now())
                .setUnregisteredBy("RegistryClient")
                .setReason(reason)
                .build();

        LOGGER.info("Unregistering planet '{}' with requestId {} (with response)", planetName, requestId);

        CompletableFuture<PlanetUnregistrationResponse> future = new CompletableFuture<>();
        pendingPlanetUnregistrations.put(requestId, future);

        sendMessage(PLANET_UNREGISTRATION_TOPIC, requestId, message)
            .orTimeout(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .whenComplete((result, throwable) -> {
                if (throwable != null) {
                    future.completeExceptionally(throwable);
                }
            });

        return future;
    }

    /**
     * Deregistriert einen Planeten ohne Grund mit Response-Handling
     *
     * @param planetName Der Planetenname
     * @return CompletableFuture mit PlanetUnregistrationResponse
     */
    public CompletableFuture<PlanetUnregistrationResponse> unregisterPlanetWithResponse(String planetName) {
        return unregisterPlanetWithResponse(planetName, null);
    }

    /**
     * Sucht nach einem Planeten anhand des Namens mit Response-Handling
     *
     * @param planetName Der Planetenname
     * @return CompletableFuture mit PlanetLookupResponse
     */
    public CompletableFuture<PlanetLookupResponse> lookupPlanetByNameWithResponse(String planetName) {
        String requestId = UUID.randomUUID().toString();

        PlanetLookupRequest message = PlanetLookupRequest.newBuilder()
                .setRequestId(requestId)
                .setPlanetName(planetName)
                .setEnvironment(de.mhus.nimbus.shared.avro.Environment.DEV)
                .setTimestamp(Instant.now())
                .build();

        LOGGER.info("Looking up planet by name '{}' with requestId {} (with response)", planetName, requestId);

        CompletableFuture<PlanetLookupResponse> future = new CompletableFuture<>();
        pendingPlanetLookups.put(requestId, future);

        sendMessage(PLANET_LOOKUP_TOPIC, requestId, message)
            .orTimeout(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .whenComplete((result, throwable) -> {
                if (throwable != null) {
                    future.completeExceptionally(throwable);
                }
            });

        return future;
    }

    /**
     * Sucht nach einem Planeten mit optionaler Welt-Spezifikation und Response-Handling
     *
     * @param planetName Der Planetenname
     * @param worldName  Der Weltname (optional)
     * @return CompletableFuture mit PlanetLookupResponse
     */
    public CompletableFuture<PlanetLookupResponse> lookupPlanetWithResponse(String planetName, String worldName) {
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

        LOGGER.info("Looking up planet '{}' {} with requestId {} (with response)",
                   planetName, worldName != null ? "with world '" + worldName + "'" : "", requestId);

        CompletableFuture<PlanetLookupResponse> future = new CompletableFuture<>();
        pendingPlanetLookups.put(requestId, future);

        sendMessage(PLANET_LOOKUP_TOPIC, requestId, message)
            .orTimeout(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .whenComplete((result, throwable) -> {
                if (throwable != null) {
                    future.completeExceptionally(throwable);
                }
            });

        return future;
    }

    /**
     * Registriert eine neue Welt mit Response-Handling
     *
     * @param worldId       Die Welt-ID
     * @param worldName     Der Weltname
     * @param planetName    Der Planetenname (nicht ID)
     * @param managementUrl Management-URL für die Welt
     * @param apiUrl        API-URL (optional)
     * @param webUrl        Web-URL (optional)
     * @return CompletableFuture mit WorldRegistrationResponse
     */
    public CompletableFuture<WorldRegistrationResponse> registerWorldWithResponse(String worldId, String worldName, String planetName,
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

        LOGGER.info("Registering world '{}' (ID: {}) on planet '{}' with requestId {} (with response)",
                   worldName, worldId, planetName, requestId);

        CompletableFuture<WorldRegistrationResponse> future = new CompletableFuture<>();
        pendingWorldRegistrations.put(requestId, future);

        sendMessage(WORLD_REGISTRATION_TOPIC, requestId, message)
            .orTimeout(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .whenComplete((result, throwable) -> {
                if (throwable != null) {
                    future.completeExceptionally(throwable);
                }
            });

        return future;
    }

    /**
     * Registriert eine Welt mit minimalen Informationen und Response-Handling
     *
     * @param worldId   Die Welt-ID
     * @param worldName Der Weltname
     * @param planetName Der Planetenname
     * @param managementUrl Management-URL
     * @return CompletableFuture mit WorldRegistrationResponse
     */
    public CompletableFuture<WorldRegistrationResponse> registerWorldWithResponse(String worldId, String worldName, String planetName, String managementUrl) {
        return registerWorldWithResponse(worldId, worldName, planetName, managementUrl, null, null);
    }

    /**
     * Deregistriert eine Welt mit Response-Handling
     *
     * @param worldId Die Welt-ID
     * @param planetName Der Planetenname (optional für Validierung)
     * @param reason Grund für die Deregistrierung
     * @return CompletableFuture mit WorldUnregistrationResponse
     */
    public CompletableFuture<WorldUnregistrationResponse> unregisterWorldWithResponse(String worldId, String planetName, String reason) {
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

        LOGGER.info("Unregistering world with ID '{}' {} with requestId {} (with response)",
                   worldId, planetName != null ? "on planet '" + planetName + "'" : "", requestId);

        CompletableFuture<WorldUnregistrationResponse> future = new CompletableFuture<>();
        pendingWorldUnregistrations.put(requestId, future);

        sendMessage(WORLD_UNREGISTRATION_TOPIC, requestId, message)
            .orTimeout(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .whenComplete((result, throwable) -> {
                if (throwable != null) {
                    future.completeExceptionally(throwable);
                }
            });

        return future;
    }

    /**
     * Deregistriert eine Welt mit minimalen Informationen und Response-Handling
     *
     * @param worldId Die Welt-ID
     * @return CompletableFuture mit WorldUnregistrationResponse
     */
    public CompletableFuture<WorldUnregistrationResponse> unregisterWorldWithResponse(String worldId) {
        return unregisterWorldWithResponse(worldId, null, null);
    }

    /**
     * Handler für eingehende PlanetRegistration-Responses
     * Diese Methode sollte von einem Kafka-Consumer aufgerufen werden
     *
     * @param response Die PlanetRegistrationResponse
     */
    public void handlePlanetRegistrationResponse(PlanetRegistrationResponse response) {
        String requestId = response.getRequestId();
        CompletableFuture<PlanetRegistrationResponse> future = pendingPlanetRegistrations.remove(requestId);

        if (future != null) {
            LOGGER.debug("Completing planet registration request {} with status {}", requestId, response.getStatus());
            future.complete(response);
        } else {
            LOGGER.warn("Received planet registration response for unknown request ID: {}", requestId);
        }
    }

    /**
     * Handler für eingehende PlanetUnregistration-Responses
     * Diese Methode sollte von einem Kafka-Consumer aufgerufen werden
     *
     * @param response Die PlanetUnregistrationResponse
     */
    public void handlePlanetUnregistrationResponse(PlanetUnregistrationResponse response) {
        String requestId = response.getRequestId();
        CompletableFuture<PlanetUnregistrationResponse> future = pendingPlanetUnregistrations.remove(requestId);

        if (future != null) {
            LOGGER.debug("Completing planet unregistration request {} with status {}", requestId, response.getStatus());
            future.complete(response);
        } else {
            LOGGER.warn("Received planet unregistration response for unknown request ID: {}", requestId);
        }
    }

    /**
     * Handler für eingehende PlanetLookup-Responses
     * Diese Methode sollte von einem Kafka-Consumer aufgerufen werden
     *
     * @param response Die PlanetLookupResponse
     */
    public void handlePlanetLookupResponse(PlanetLookupResponse response) {
        String requestId = response.getRequestId();
        CompletableFuture<PlanetLookupResponse> future = pendingPlanetLookups.remove(requestId);

        if (future != null) {
            LOGGER.debug("Completing planet lookup request {} with status {}", requestId, response.getStatus());
            future.complete(response);
        } else {
            LOGGER.warn("Received planet lookup response for unknown request ID: {}", requestId);
        }
    }

    /**
     * Handler für eingehende WorldRegistration-Responses
     * Diese Methode sollte von einem Kafka-Consumer aufgerufen werden
     *
     * @param response Die WorldRegistrationResponse
     */
    public void handleWorldRegistrationResponse(WorldRegistrationResponse response) {
        String requestId = response.getRequestId();
        CompletableFuture<WorldRegistrationResponse> future = pendingWorldRegistrations.remove(requestId);

        if (future != null) {
            LOGGER.debug("Completing world registration request {} with status {}", requestId, response.getStatus());
            future.complete(response);
        } else {
            LOGGER.warn("Received world registration response for unknown request ID: {}", requestId);
        }
    }

    /**
     * Handler für eingehende WorldUnregistration-Responses
     * Diese Methode sollte von einem Kafka-Consumer aufgerufen werden
     *
     * @param response Die WorldUnregistrationResponse
     */
    public void handleWorldUnregistrationResponse(WorldUnregistrationResponse response) {
        String requestId = response.getRequestId();
        CompletableFuture<WorldUnregistrationResponse> future = pendingWorldUnregistrations.remove(requestId);

        if (future != null) {
            LOGGER.debug("Completing world unregistration request {} with status {}", requestId, response.getStatus());
            future.complete(response);
        } else {
            LOGGER.warn("Received world unregistration response for unknown request ID: {}", requestId);
        }
    }

    /**
     * Bereinigt abgelaufene Requests aus den pending Maps
     * Diese Methode sollte periodisch aufgerufen werden
     */
    public void cleanupExpiredRequests() {
        long expiredCount = 0;

        // Cleanup für Planet Registration Requests
        expiredCount += cleanupExpiredRequests(pendingPlanetRegistrations, "planet registration");

        // Cleanup für Planet Unregistration Requests
        expiredCount += cleanupExpiredRequests(pendingPlanetUnregistrations, "planet unregistration");

        // Cleanup für Planet Lookup Requests
        expiredCount += cleanupExpiredRequests(pendingPlanetLookups, "planet lookup");

        // Cleanup für World Registration Requests
        expiredCount += cleanupExpiredRequests(pendingWorldRegistrations, "world registration");

        // Cleanup für World Unregistration Requests
        expiredCount += cleanupExpiredRequests(pendingWorldUnregistrations, "world unregistration");

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
        return pendingPlanetRegistrations.size() +
               pendingPlanetUnregistrations.size() +
               pendingPlanetLookups.size() +
               pendingWorldRegistrations.size() +
               pendingWorldUnregistrations.size();
    }

    /**
     * Gibt Statistiken über wartende Requests zurück
     */
    public String getPendingRequestStats() {
        return String.format("Pending requests - PlanetReg: %d, PlanetUnreg: %d, PlanetLookup: %d, WorldReg: %d, WorldUnreg: %d",
                pendingPlanetRegistrations.size(),
                pendingPlanetUnregistrations.size(),
                pendingPlanetLookups.size(),
                pendingWorldRegistrations.size(),
                pendingWorldUnregistrations.size());
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
