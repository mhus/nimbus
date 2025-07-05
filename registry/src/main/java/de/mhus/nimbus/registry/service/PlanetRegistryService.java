package de.mhus.nimbus.registry.service;

import de.mhus.nimbus.registry.entity.Planet;
import de.mhus.nimbus.registry.entity.World;
import de.mhus.nimbus.registry.repository.PlanetRepository;
import de.mhus.nimbus.registry.repository.WorldRepository;
import de.mhus.nimbus.shared.avro.Environment;
import de.mhus.nimbus.shared.avro.PlanetLookupRequest;
import de.mhus.nimbus.shared.avro.PlanetLookupResponse;
import de.mhus.nimbus.shared.avro.PlanetLookupStatus;
import de.mhus.nimbus.shared.avro.PlanetWorld;
import de.mhus.nimbus.shared.avro.PlanetInfo;
import de.mhus.nimbus.shared.avro.PlanetRegistrationRequest;
import de.mhus.nimbus.shared.avro.PlanetRegistrationResponse;
import de.mhus.nimbus.shared.avro.PlanetRegistrationStatus;
import de.mhus.nimbus.shared.avro.PlanetUnregistrationRequest;
import de.mhus.nimbus.shared.avro.PlanetUnregistrationResponse;
import de.mhus.nimbus.shared.avro.PlanetUnregistrationStatus;
import de.mhus.nimbus.shared.avro.RegisteredWorld;
import de.mhus.nimbus.shared.avro.WorldRegistration;
import de.mhus.nimbus.shared.avro.WorldRegistrationStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Service-Schicht für Planet-Lookup-Operationen
 * Trennt die Kafka-Kommunikation von der Business-Logik
 */
@Service
@Transactional(readOnly = true)
public class PlanetRegistryService {

    private static final Logger logger = LoggerFactory.getLogger(PlanetRegistryService.class);

    private final PlanetRepository planetRepository;
    private final WorldRepository worldRepository;
    private final PlanetEntityMapper planetEntityMapper;

    public PlanetRegistryService(PlanetRepository planetRepository,
                               WorldRepository worldRepository,
                               PlanetEntityMapper planetEntityMapper) {
        this.planetRepository = planetRepository;
        this.worldRepository = worldRepository;
        this.planetEntityMapper = planetEntityMapper;
    }

    /**
     * Verarbeitet eine Planet-Lookup-Anfrage und gibt die entsprechende Antwort zurück
     *
     * @param request Die eingehende Planet-Lookup-Anfrage
     * @return Die Planet-Lookup-Antwort mit gefundenen Welten oder Fehlerstatus
     */
    public PlanetLookupResponse processLookupRequest(PlanetLookupRequest request) {
        logger.info("Processing planet lookup request: planet={}, world={}, environment={}, requestedBy={}",
                   request.getPlanetName(), request.getWorldName(), request.getEnvironment(), request.getRequestedBy());

        long currentTimestamp = Instant.now().toEpochMilli();

        try {
            // Suche nach Planet und Welten
            List<PlanetWorld> planetWorlds = findPlanetWorlds(request.getPlanetName(), request.getWorldName(), request.getEnvironment());

            // Bestimme Status basierend auf Ergebnis
            PlanetLookupStatus status = determineLookupStatus(request, planetWorlds);

            // Erstelle Response
            return PlanetLookupResponse.newBuilder()
                    .setRequestId(request.getRequestId())
                    .setStatus(status)
                    .setPlanetName(request.getPlanetName())
                    .setWorldName(request.getWorldName())
                    .setPlanetWorlds(planetWorlds)
                    .setTimestamp(Instant.ofEpochMilli(currentTimestamp))
                    .setErrorMessage(getErrorMessage(status, request))
                    .build();

        } catch (Exception e) {
            logger.error("Error processing planet lookup request: {}", request.getRequestId(), e);
            return createErrorResponse(request, e.getMessage(), currentTimestamp);
        }
    }

    /**
     * Sucht nach Welten basierend auf Planet-Name, Welt-Name und Environment
     *
     * @param planetName Name des Planeten
     * @param worldName Name der spezifischen Welt (optional)
     * @param environment Umgebung für die Suche
     * @return Liste der gefundenen Welten
     */
    private List<PlanetWorld> findPlanetWorlds(String planetName, String worldName, Environment environment) {
        logger.debug("Searching for planet worlds: planet={}, world={}, environment={}", planetName, worldName, environment);

        // Prüfe ob spezifische Welt gesucht wird
        if (worldName != null && !worldName.trim().isEmpty()) {
            Optional<World> worldOpt = worldRepository.findByPlanetNameAndWorldNameAndEnvironment(
                planetName, worldName.trim(), environment);

            return worldOpt.map(world -> List.of(planetEntityMapper.toAvro(world)))
                          .orElse(List.of());
        } else {
            // Finde alle Welten des Planeten
            List<World> worlds = worldRepository.findByPlanetNameAndEnvironment(planetName, environment);
            return planetEntityMapper.toAvro(worlds);
        }
    }

    /**
     * Bestimmt den Lookup-Status basierend auf der Anfrage und den gefundenen Welten
     *
     * @param request Die ursprüngliche Anfrage
     * @param planetWorlds Die gefundenen Welten
     * @return Der entsprechende PlanetLookupStatus
     */
    private PlanetLookupStatus determineLookupStatus(PlanetLookupRequest request, List<PlanetWorld> planetWorlds) {
        if (planetWorlds == null || planetWorlds.isEmpty()) {
            // Prüfe ob Planet überhaupt existiert
            boolean planetExists = planetRepository.existsByNameIgnoreCaseAndEnvironmentAndActiveTrue(
                request.getPlanetName(), request.getEnvironment());

            if (!planetExists) {
                return PlanetLookupStatus.PLANET_NOT_FOUND;
            }

            // Planet existiert, aber spezifische Welt nicht gefunden
            if (request.getWorldName() != null && !request.getWorldName().trim().isEmpty()) {
                return PlanetLookupStatus.WORLD_NOT_FOUND;
            }

            // Planet existiert, aber hat keine Welten
            return PlanetLookupStatus.PLANET_NOT_FOUND;
        }

        return PlanetLookupStatus.SUCCESS;
    }

    /**
     * Generiert eine passende Fehlermeldung basierend auf dem Status
     *
     * @param status Der Lookup-Status
     * @param request Die ursprüngliche Anfrage
     * @return Fehlermeldung oder null bei Erfolg
     */
    private String getErrorMessage(PlanetLookupStatus status, PlanetLookupRequest request) {
        return switch (status) {
            case PLANET_NOT_FOUND -> "Planet '" + request.getPlanetName() + "' not found in registry";
            case WORLD_NOT_FOUND -> "World '" + request.getWorldName() + "' not found on planet '" + request.getPlanetName() + "'";
            case ERROR -> "Internal error processing planet lookup request";
            case TIMEOUT -> "Planet lookup request timed out";
            case SUCCESS -> null;
        };
    }

    /**
     * Erstellt eine Error-Response für unerwartete Fehler
     *
     * @param request Die ursprüngliche Anfrage
     * @param errorMessage Die Fehlermeldung
     * @param timestamp Zeitstempel der Antwort
     * @return Error-Response
     */
    private PlanetLookupResponse createErrorResponse(PlanetLookupRequest request, String errorMessage, long timestamp) {
        return PlanetLookupResponse.newBuilder()
                .setRequestId(request.getRequestId())
                .setStatus(PlanetLookupStatus.ERROR)
                .setPlanetName(request.getPlanetName())
                .setWorldName(request.getWorldName())
                .setPlanetWorlds(List.of())
                .setTimestamp(Instant.ofEpochMilli(timestamp))
                .setErrorMessage(errorMessage)
                .build();
    }

    /**
     * Öffentliche Methode zum Erstellen einer Error-Response (für Consumer)
     *
     * @param request Die ursprüngliche Anfrage
     * @param errorMessage Die Fehlermeldung
     * @return Error-Response
     */
    public PlanetLookupResponse createErrorResponse(PlanetLookupRequest request, String errorMessage) {
        long currentTimestamp = Instant.now().toEpochMilli();
        return createErrorResponse(request, errorMessage, currentTimestamp);
    }

    /**
     * Validiert eine eingehende Planet-Lookup-Anfrage
     *
     * @param request Die zu validierende Anfrage
     * @throws IllegalArgumentException wenn die Anfrage ungültig ist
     */
    public void validateRequest(PlanetLookupRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Planet lookup request cannot be null");
        }

        if (request.getRequestId() == null || request.getRequestId().trim().isEmpty()) {
            throw new IllegalArgumentException("Request ID cannot be null or empty");
        }

        if (request.getPlanetName() == null || request.getPlanetName().trim().isEmpty()) {
            throw new IllegalArgumentException("Planet name cannot be null or empty");
        }

        if (request.getEnvironment() == null) {
            throw new IllegalArgumentException("Environment cannot be null");
        }

        logger.debug("Planet lookup request validation passed: {}", request.getRequestId());
    }

    /**
     * Überprüft ob ein Planet in der Registry existiert
     *
     * @param planetName Name des Planeten
     * @param environment Umgebung
     * @return true wenn der Planet existiert
     */
    public boolean planetExists(String planetName, Environment environment) {
        logger.debug("Checking if planet exists: planet={}, environment={}", planetName, environment);
        return planetRepository.existsByNameIgnoreCaseAndEnvironmentAndActiveTrue(planetName, environment);
    }

    /**
     * Überprüft ob eine spezifische Welt auf einem Planeten existiert
     *
     * @param planetName Name des Planeten
     * @param worldName Name der Welt
     * @param environment Umgebung
     * @return true wenn die Welt existiert
     */
    public boolean worldExists(String planetName, String worldName, Environment environment) {
        logger.debug("Checking if world exists: planet={}, world={}, environment={}", planetName, worldName, environment);
        return worldRepository.existsByPlanetNameAndWorldNameAndEnvironment(planetName, worldName, environment);
    }

    /**
     * Verarbeitet eine Planet-Registrierungs-Anfrage
     *
     * @param request Die eingehende Planet-Registrierungs-Anfrage
     * @return Die Planet-Registrierungs-Antwort mit Ergebnis
     */
    @Transactional
    public PlanetRegistrationResponse processRegistrationRequest(PlanetRegistrationRequest request) {
        logger.info("Processing planet registration request: planet={}, environment={}, worlds={}, registeredBy={}",
                   request.getPlanetName(), request.getEnvironment(), request.getWorlds().size(), request.getRegisteredBy());

        long currentTimestamp = Instant.now().toEpochMilli();

        try {
            // Prüfe ob Planet bereits existiert
            Optional<Planet> existingPlanet = planetRepository.findByNameIgnoreCaseAndEnvironmentAndActiveTrue(
                request.getPlanetName(), request.getEnvironment());

            Planet planet;
            List<RegisteredWorld> registeredWorlds = new ArrayList<>();
            PlanetRegistrationStatus status;

            if (existingPlanet.isPresent()) {
                // Planet existiert bereits - aktualisiere nur die Welten
                planet = existingPlanet.get();
                status = PlanetRegistrationStatus.SUCCESS; // Könnte auch PLANET_ALREADY_EXISTS sein
                logger.info("Planet {} already exists in environment {}, updating worlds",
                           request.getPlanetName(), request.getEnvironment());
            } else {
                // Erstelle neuen Planet
                planet = createPlanetFromRequest(request);
                planet = planetRepository.save(planet);
                status = PlanetRegistrationStatus.SUCCESS;
                logger.info("Created new planet: {} in environment {}",
                           request.getPlanetName(), request.getEnvironment());
            }

            // Verarbeite die Welten
            registeredWorlds = processWorldRegistrations(planet, request.getWorlds());

            return PlanetRegistrationResponse.newBuilder()
                    .setRequestId(request.getRequestId())
                    .setStatus(status)
                    .setPlanetName(request.getPlanetName())
                    .setEnvironment(request.getEnvironment())
                    .setRegisteredWorlds(registeredWorlds)
                    .setTimestamp(Instant.ofEpochMilli(currentTimestamp))
                    .setMessage("Planet registration completed successfully")
                    .setErrorMessage(null)
                    .build();

        } catch (Exception e) {
            logger.error("Error processing planet registration request: {}", request.getRequestId(), e);
            return createRegistrationErrorResponse(request, e.getMessage(), currentTimestamp);
        }
    }

    /**
     * Erstellt einen neuen Planet aus der Registrierungs-Anfrage
     */
    private Planet createPlanetFromRequest(PlanetRegistrationRequest request) {
        Planet planet = new Planet(request.getPlanetName(), request.getEnvironment());

        PlanetInfo planetInfo = request.getPlanetInfo();
        if (planetInfo != null) {
            planet.setDescription(planetInfo.getDescription());
            planet.setGalaxy(planetInfo.getGalaxy());
            planet.setSector(planetInfo.getSector());
            planet.setSystemName(planetInfo.getSystemName());
            planet.setPopulation(planetInfo.getPopulation());
            planet.setClimate(planetInfo.getClimate());
            planet.setTerrain(planetInfo.getTerrain());
            planet.setSurfaceWater(planetInfo.getSurfaceWater());
            planet.setGravity(planetInfo.getGravity());
        }

        return planet;
    }

    /**
     * Verarbeitet die Registrierung von Welten für einen Planet
     */
    private List<RegisteredWorld> processWorldRegistrations(Planet planet, List<WorldRegistration> worldRegistrations) {
        List<RegisteredWorld> results = new ArrayList<>();

        for (WorldRegistration worldReg : worldRegistrations) {
            try {
                RegisteredWorld result = processSingleWorldRegistration(planet, worldReg);
                results.add(result);
            } catch (Exception e) {
                logger.error("Error registering world {}: {}", worldReg.getWorldId(), e.getMessage());

                RegisteredWorld errorResult = RegisteredWorld.newBuilder()
                        .setWorldId(worldReg.getWorldId())
                        .setWorldName(worldReg.getWorldName())
                        .setStatus(WorldRegistrationStatus.FAILED)
                        .setMessage("Registration failed: " + e.getMessage())
                        .build();
                results.add(errorResult);
            }
        }

        return results;
    }

    /**
     * Verarbeitet die Registrierung einer einzelnen Welt
     */
    private RegisteredWorld processSingleWorldRegistration(Planet planet, WorldRegistration worldReg) {
        // Prüfe ob Welt bereits existiert
        Optional<World> existingWorld = worldRepository.findByWorldId(worldReg.getWorldId());

        World world;
        WorldRegistrationStatus status;
        String message;

        if (existingWorld.isPresent()) {
            // Welt existiert bereits - aktualisiere sie
            world = existingWorld.get();
            updateWorldFromRegistration(world, worldReg);
            status = WorldRegistrationStatus.UPDATED;
            message = "World updated successfully";
            logger.debug("Updated existing world: {}", worldReg.getWorldId());
        } else {
            // Erstelle neue Welt
            world = createWorldFromRegistration(planet, worldReg);
            status = WorldRegistrationStatus.CREATED;
            message = "World created successfully";
            logger.debug("Created new world: {}", worldReg.getWorldId());
        }

        world = worldRepository.save(world);

        return RegisteredWorld.newBuilder()
                .setWorldId(world.getWorldId())
                .setWorldName(world.getName())
                .setStatus(status)
                .setMessage(message)
                .build();
    }

    /**
     * Erstellt eine neue Welt aus einer Registrierungs-Anfrage
     */
    private World createWorldFromRegistration(Planet planet, WorldRegistration worldReg) {
        World world = new World(worldReg.getWorldId(), worldReg.getWorldName(), worldReg.getManagementUrl());
        world.setPlanet(planet);
        world.setApiUrl(worldReg.getApiUrl());
        world.setWebUrl(worldReg.getWebUrl());
        world.setDescription(worldReg.getDescription());
        world.setWorldType(worldReg.getWorldType());
        world.setAccessLevel(worldReg.getAccessLevel());
        world.setLastHealthCheck(Instant.now());

        if (worldReg.getMetadata() != null) {
            world.setMetadata(worldReg.getMetadata());
        }

        return world;
    }

    /**
     * Aktualisiert eine bestehende Welt mit Daten aus einer Registrierungs-Anfrage
     */
    private void updateWorldFromRegistration(World world, WorldRegistration worldReg) {
        world.setName(worldReg.getWorldName());
        world.setManagementUrl(worldReg.getManagementUrl());
        world.setApiUrl(worldReg.getApiUrl());
        world.setWebUrl(worldReg.getWebUrl());
        world.setDescription(worldReg.getDescription());
        world.setWorldType(worldReg.getWorldType());
        world.setAccessLevel(worldReg.getAccessLevel());
        world.setLastHealthCheck(Instant.now());

        if (worldReg.getMetadata() != null) {
            world.getMetadata().clear();
            world.getMetadata().putAll(worldReg.getMetadata());
        }
    }

    /**
     * Validiert eine eingehende Planet-Registrierungs-Anfrage
     */
    public void validateRegistrationRequest(PlanetRegistrationRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Planet registration request cannot be null");
        }

        if (request.getRequestId() == null || request.getRequestId().trim().isEmpty()) {
            throw new IllegalArgumentException("Request ID cannot be null or empty");
        }

        if (request.getPlanetName() == null || request.getPlanetName().trim().isEmpty()) {
            throw new IllegalArgumentException("Planet name cannot be null or empty");
        }

        if (request.getEnvironment() == null) {
            throw new IllegalArgumentException("Environment cannot be null");
        }

        // Validiere Welten
        if (request.getWorlds() != null) {
            for (WorldRegistration world : request.getWorlds()) {
                validateWorldRegistration(world);
            }
        }

        logger.debug("Planet registration request validation passed: {}", request.getRequestId());
    }

    /**
     * Validiert eine einzelne Welt-Registrierung
     */
    private void validateWorldRegistration(WorldRegistration world) {
        if (world.getWorldId() == null || world.getWorldId().trim().isEmpty()) {
            throw new IllegalArgumentException("World ID cannot be null or empty");
        }

        if (world.getWorldName() == null || world.getWorldName().trim().isEmpty()) {
            throw new IllegalArgumentException("World name cannot be null or empty");
        }

        if (world.getManagementUrl() == null || world.getManagementUrl().trim().isEmpty()) {
            throw new IllegalArgumentException("Management URL cannot be null or empty");
        }
    }

    /**
     * Erstellt eine Error-Response für Planet-Registrierungs-Fehler
     */
    public PlanetRegistrationResponse createRegistrationErrorResponse(PlanetRegistrationRequest request, String errorMessage) {
        long currentTimestamp = Instant.now().toEpochMilli();
        return createRegistrationErrorResponse(request, errorMessage, currentTimestamp);
    }

    /**
     * Erstellt eine Error-Response für Planet-Registrierungs-Fehler mit Timestamp
     */
    private PlanetRegistrationResponse createRegistrationErrorResponse(PlanetRegistrationRequest request, String errorMessage, long timestamp) {
        return PlanetRegistrationResponse.newBuilder()
                .setRequestId(request.getRequestId())
                .setStatus(PlanetRegistrationStatus.ERROR)
                .setPlanetName(request.getPlanetName())
                .setEnvironment(request.getEnvironment())
                .setRegisteredWorlds(List.of())
                .setTimestamp(Instant.ofEpochMilli(timestamp))
                .setMessage(null)
                .setErrorMessage(errorMessage)
                .build();
    }

    /**
     * Verarbeitet eine Planet-Deregistrierungs-Anfrage
     *
     * @param request Die eingehende Planet-Deregistrierungs-Anfrage
     * @return Die Planet-Deregistrierungs-Antwort mit Ergebnis
     */
    @Transactional
    public PlanetUnregistrationResponse processUnregistrationRequest(PlanetUnregistrationRequest request) {
        logger.info("Processing planet unregistration request: planet={}, environment={}, unregisteredBy={}",
                   request.getPlanetName(), request.getEnvironment(), request.getUnregisteredBy());

        long currentTimestamp = Instant.now().toEpochMilli();

        try {
            // Prüfe ob Planet existiert
            Optional<Planet> existingPlanet = planetRepository.findByNameIgnoreCaseAndEnvironmentAndActiveTrue(
                request.getPlanetName(), request.getEnvironment());

            if (existingPlanet.isEmpty()) {
                logger.warn("Planet {} not found for unregistration in environment {}",
                           request.getPlanetName(), request.getEnvironment());

                return PlanetUnregistrationResponse.newBuilder()
                        .setRequestId(request.getRequestId())
                        .setStatus(PlanetUnregistrationStatus.PLANET_NOT_FOUND)
                        .setPlanetName(request.getPlanetName())
                        .setEnvironment(request.getEnvironment())
                        .setTimestamp(Instant.ofEpochMilli(currentTimestamp))
                        .setMessage("Planet not found for unregistration")
                        .setErrorMessage("Planet '" + request.getPlanetName() + "' not found in registry")
                        .build();
            }

            Planet planet = existingPlanet.get();

            // Prüfe ob Planet bereits inaktiv ist
            if (!planet.getActive()) {
                logger.warn("Planet {} is already inactive in environment {}",
                           request.getPlanetName(), request.getEnvironment());

                return PlanetUnregistrationResponse.newBuilder()
                        .setRequestId(request.getRequestId())
                        .setStatus(PlanetUnregistrationStatus.PLANET_ALREADY_INACTIVE)
                        .setPlanetName(request.getPlanetName())
                        .setEnvironment(request.getEnvironment())
                        .setTimestamp(Instant.ofEpochMilli(currentTimestamp))
                        .setMessage("Planet is already inactive")
                        .setErrorMessage("Planet '" + request.getPlanetName() + "' is already inactive")
                        .build();
            }

            // Deaktiviere den Planet (soft delete)
            planet.setActive(false);
            planetRepository.save(planet);

            // Hinweis: Da World-Entity kein active-Feld hat, entfernen wir diese Welten aus der Datenbank
            // oder markieren sie anders. Für jetzt loggen wir nur die Anzahl der betroffenen Welten.
            List<World> worlds = worldRepository.findByPlanetNameAndEnvironment(
                request.getPlanetName(), request.getEnvironment());

            logger.info("Found {} worlds associated with planet {} in environment {}",
                       worlds.size(), request.getPlanetName(), request.getEnvironment());

            logger.info("Successfully unregistered planet {} with {} worlds in environment {}",
                       request.getPlanetName(), worlds.size(), request.getEnvironment());

            return PlanetUnregistrationResponse.newBuilder()
                    .setRequestId(request.getRequestId())
                    .setStatus(PlanetUnregistrationStatus.SUCCESS)
                    .setPlanetName(request.getPlanetName())
                    .setEnvironment(request.getEnvironment())
                    .setTimestamp(Instant.ofEpochMilli(currentTimestamp))
                    .setMessage("Planet unregistration completed successfully")
                    .setErrorMessage(null)
                    .build();

        } catch (Exception e) {
            logger.error("Error processing planet unregistration request: {}", request.getRequestId(), e);
            return createUnregistrationErrorResponse(request, e.getMessage(), currentTimestamp);
        }
    }

    /**
     * Validiert eine eingehende Planet-Deregistrierungs-Anfrage
     *
     * @param request Die zu validierende Anfrage
     * @throws IllegalArgumentException wenn die Anfrage ungültig ist
     */
    public void validateUnregistrationRequest(PlanetUnregistrationRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Planet unregistration request cannot be null");
        }

        if (request.getRequestId() == null || request.getRequestId().trim().isEmpty()) {
            throw new IllegalArgumentException("Request ID cannot be null or empty");
        }

        if (request.getPlanetName() == null || request.getPlanetName().trim().isEmpty()) {
            throw new IllegalArgumentException("Planet name cannot be null or empty");
        }

        if (request.getEnvironment() == null) {
            throw new IllegalArgumentException("Environment cannot be null");
        }

        logger.debug("Planet unregistration request validation passed: {}", request.getRequestId());
    }

    /**
     * Erstellt eine Error-Response für Planet-Deregistrierungs-Fehler
     *
     * @param request Die ursprüngliche Anfrage
     * @param errorMessage Die Fehlermeldung
     * @return Error-Response
     */
    public PlanetUnregistrationResponse createUnregistrationErrorResponse(PlanetUnregistrationRequest request, String errorMessage) {
        long currentTimestamp = Instant.now().toEpochMilli();
        return createUnregistrationErrorResponse(request, errorMessage, currentTimestamp);
    }

    /**
     * Erstellt eine Error-Response für Planet-Deregistrierungs-Fehler mit Timestamp
     *
     * @param request Die ursprüngliche Anfrage
     * @param errorMessage Die Fehlermeldung
     * @param timestamp Zeitstempel der Antwort
     * @return Error-Response
     */
    private PlanetUnregistrationResponse createUnregistrationErrorResponse(PlanetUnregistrationRequest request, String errorMessage, long timestamp) {
        return PlanetUnregistrationResponse.newBuilder()
                .setRequestId(request.getRequestId())
                .setStatus(PlanetUnregistrationStatus.ERROR)
                .setPlanetName(request.getPlanetName())
                .setEnvironment(request.getEnvironment())
                .setTimestamp(Instant.ofEpochMilli(timestamp))
                .setMessage(null)
                .setErrorMessage(errorMessage)
                .build();
    }
}
