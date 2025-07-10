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
import de.mhus.nimbus.shared.avro.WorldRegistration;
import de.mhus.nimbus.shared.avro.WorldRegistrationRequest;
import de.mhus.nimbus.shared.avro.WorldRegistrationResponse;
import de.mhus.nimbus.shared.avro.WorldRegistrationStatus;
import de.mhus.nimbus.shared.avro.WorldUnregistrationRequest;
import de.mhus.nimbus.shared.avro.WorldUnregistrationResponse;
import de.mhus.nimbus.shared.avro.WorldUnregistrationStatus;
import de.mhus.nimbus.shared.avro.RegisteredWorld;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
public class PlanetRegistryService {

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
        log.info("Processing planet lookup request: planet={}, world={}, environment={}, requestedBy={}",
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
            log.error("Error processing planet lookup request: {}", request.getRequestId(), e);
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
        log.debug("Searching for planet worlds: planet={}, world={}, environment={}", planetName, worldName, environment);

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

        log.debug("Planet lookup request validation passed: {}", request.getRequestId());
    }

    /**
     * Überprüft ob ein Planet in der Registry existiert
     *
     * @param planetName Name des Planeten
     * @param environment Umgebung
     * @return true wenn der Planet existiert
     */
    public boolean planetExists(String planetName, Environment environment) {
        log.debug("Checking if planet exists: planet={}, environment={}", planetName, environment);
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
        log.debug("Checking if world exists: planet={}, world={}, environment={}", planetName, worldName, environment);
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
        log.info("Processing planet registration request: planet={}, environment={}, worlds={}, registeredBy={}",
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
                log.info("Planet {} already exists in environment {}, updating worlds",
                           request.getPlanetName(), request.getEnvironment());
            } else {
                // Erstelle neuen Planet
                planet = createPlanetFromRequest(request);
                planet = planetRepository.save(planet);
                status = PlanetRegistrationStatus.SUCCESS;
                log.info("Created new planet: {} in environment {}",
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
            log.error("Error processing planet registration request: {}", request.getRequestId(), e);
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
                log.error("Error registering world {}: {}", worldReg.getWorldId(), e.getMessage());

                RegisteredWorld errorResult = RegisteredWorld.newBuilder()
                        .setWorldId(worldReg.getWorldId())
                        .setWorldName(worldReg.getWorldName())
                        .setStatus(WorldRegistrationStatus.ERROR)
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
            log.debug("Updated existing world: {}", worldReg.getWorldId());
        } else {
            // Erstelle neue Welt
            world = createWorldFromRegistration(planet, worldReg);
            status = WorldRegistrationStatus.SUCCESS;
            message = "World created successfully";
            log.debug("Created new world: {}", worldReg.getWorldId());
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

        log.debug("Planet registration request validation passed: {}", request.getRequestId());
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
        log.info("Processing planet unregistration request: planet={}, environment={}, unregisteredBy={}",
                   request.getPlanetName(), request.getEnvironment(), request.getUnregisteredBy());

        long currentTimestamp = Instant.now().toEpochMilli();

        try {
            // Prüfe ob Planet existiert
            Optional<Planet> existingPlanet = planetRepository.findByNameIgnoreCaseAndEnvironmentAndActiveTrue(
                request.getPlanetName(), request.getEnvironment());

            if (existingPlanet.isEmpty()) {
                log.warn("Planet {} not found for unregistration in environment {}",
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
                log.warn("Planet {} is already inactive in environment {}",
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

            log.info("Found {} worlds associated with planet {} in environment {}",
                       worlds.size(), request.getPlanetName(), request.getEnvironment());

            log.info("Successfully unregistered planet {} with {} worlds in environment {}",
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
            log.error("Error processing planet unregistration request: {}", request.getRequestId(), e);
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

        log.debug("Planet unregistration request validation passed: {}", request.getRequestId());
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

    /**
     * Verarbeitet eine World-Registrierungs-Anfrage
     *
     * @param request Die eingehende World-Registrierungs-Anfrage
     * @return Die World-Registrierungs-Antwort mit Ergebnis
     */
    @Transactional
    public WorldRegistrationResponse processWorldRegistrationRequest(WorldRegistrationRequest request) {
        log.info("Processing world registration request: world={}, planet={}, environment={}, registeredBy={}",
                   request.getWorldName(), request.getPlanetName(), request.getEnvironment(), request.getRegisteredBy());

        long currentTimestamp = Instant.now().toEpochMilli();

        try {
            // Prüfe ob Planet existiert
            Optional<Planet> existingPlanet = planetRepository.findByNameIgnoreCaseAndEnvironmentAndActiveTrue(
                request.getPlanetName(), request.getEnvironment());

            if (existingPlanet.isEmpty()) {
                log.warn("Planet {} not found for world registration in environment {}",
                           request.getPlanetName(), request.getEnvironment());

                return WorldRegistrationResponse.newBuilder()
                        .setRequestId(request.getRequestId())
                        .setStatus(WorldRegistrationStatus.PLANET_NOT_FOUND)
                        .setPlanetName(request.getPlanetName())
                        .setWorldId(request.getWorldId())
                        .setWorldName(request.getWorldName())
                        .setEnvironment(request.getEnvironment())
                        .setTimestamp(Instant.ofEpochMilli(currentTimestamp))
                        .setMessage("Planet not found for world registration")
                        .setErrorMessage("Planet '" + request.getPlanetName() + "' not found in registry")
                        .build();
            }

            Planet planet = existingPlanet.get();

            // Prüfe ob Welt bereits existiert
            Optional<World> existingWorld = worldRepository.findByWorldId(request.getWorldId());

            World world;
            WorldRegistrationStatus status;
            String message;

            if (existingWorld.isPresent()) {
                // Welt existiert bereits - prüfe ob sie zum selben Planeten gehört
                world = existingWorld.get();
                if (!world.getPlanet().getId().equals(planet.getId())) {
                    log.warn("World {} already exists on different planet", request.getWorldId());
                    return WorldRegistrationResponse.newBuilder()
                            .setRequestId(request.getRequestId())
                            .setStatus(WorldRegistrationStatus.WORLD_ALREADY_EXISTS)
                            .setPlanetName(request.getPlanetName())
                            .setWorldId(request.getWorldId())
                            .setWorldName(request.getWorldName())
                            .setEnvironment(request.getEnvironment())
                            .setTimestamp(Instant.ofEpochMilli(currentTimestamp))
                            .setMessage("World already exists on different planet")
                            .setErrorMessage("World '" + request.getWorldId() + "' already exists on planet '" + world.getPlanet().getName() + "'")
                            .build();
                }

                // Aktualisiere existierende Welt
                updateWorldFromWorldRegistrationRequest(world, request);
                status = WorldRegistrationStatus.SUCCESS;
                message = "World updated successfully";
                log.info("Updated existing world: {} on planet {}", request.getWorldId(), request.getPlanetName());
            } else {
                // Erstelle neue Welt
                world = createWorldFromWorldRegistrationRequest(planet, request);
                status = WorldRegistrationStatus.SUCCESS;
                message = "World created successfully";
                log.info("Created new world: {} on planet {}", request.getWorldId(), request.getPlanetName());
            }

            world = worldRepository.save(world);

            return WorldRegistrationResponse.newBuilder()
                    .setRequestId(request.getRequestId())
                    .setStatus(status)
                    .setPlanetName(request.getPlanetName())
                    .setWorldId(world.getWorldId())
                    .setWorldName(world.getName())
                    .setEnvironment(request.getEnvironment())
                    .setTimestamp(Instant.ofEpochMilli(currentTimestamp))
                    .setMessage(message)
                    .setErrorMessage(null)
                    .build();

        } catch (Exception e) {
            log.error("Error processing world registration request: {}", request.getRequestId(), e);
            return createWorldRegistrationErrorResponse(request, e.getMessage(), currentTimestamp);
        }
    }

    /**
     * Erstellt eine neue Welt aus einer World-Registrierungs-Anfrage
     */
    private World createWorldFromWorldRegistrationRequest(Planet planet, WorldRegistrationRequest request) {
        World world = new World(request.getWorldId(), request.getWorldName(), request.getManagementUrl());
        world.setPlanet(planet);
        world.setApiUrl(request.getApiUrl());
        world.setWebUrl(request.getWebUrl());
        world.setDescription(request.getDescription());
        world.setWorldType(request.getWorldType());
        world.setAccessLevel(request.getAccessLevel());
        world.setLastHealthCheck(Instant.now());

        if (request.getMetadata() != null) {
            world.setMetadata(request.getMetadata());
        }

        return world;
    }

    /**
     * Aktualisiert eine bestehende Welt mit Daten aus einer World-Registrierungs-Anfrage
     */
    private void updateWorldFromWorldRegistrationRequest(World world, WorldRegistrationRequest request) {
        world.setName(request.getWorldName());
        world.setManagementUrl(request.getManagementUrl());
        world.setApiUrl(request.getApiUrl());
        world.setWebUrl(request.getWebUrl());
        world.setDescription(request.getDescription());
        world.setWorldType(request.getWorldType());
        world.setAccessLevel(request.getAccessLevel());
        world.setLastHealthCheck(Instant.now());

        if (request.getMetadata() != null) {
            world.getMetadata().clear();
            world.getMetadata().putAll(request.getMetadata());
        }
    }

    /**
     * Validiert eine eingehende World-Registrierungs-Anfrage
     *
     * @param request Die zu validierende Anfrage
     * @throws IllegalArgumentException wenn die Anfrage ungültig ist
     */
    public void validateWorldRegistrationRequest(WorldRegistrationRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("World registration request cannot be null");
        }

        if (request.getRequestId() == null || request.getRequestId().trim().isEmpty()) {
            throw new IllegalArgumentException("Request ID cannot be null or empty");
        }

        if (request.getPlanetName() == null || request.getPlanetName().trim().isEmpty()) {
            throw new IllegalArgumentException("Planet name cannot be null or empty");
        }

        if (request.getWorldId() == null || request.getWorldId().trim().isEmpty()) {
            throw new IllegalArgumentException("World ID cannot be null or empty");
        }

        if (request.getWorldName() == null || request.getWorldName().trim().isEmpty()) {
            throw new IllegalArgumentException("World name cannot be null or empty");
        }

        if (request.getManagementUrl() == null || request.getManagementUrl().trim().isEmpty()) {
            throw new IllegalArgumentException("Management URL cannot be null or empty");
        }

        if (request.getEnvironment() == null) {
            throw new IllegalArgumentException("Environment cannot be null");
        }

        log.debug("World registration request validation passed: {}", request.getRequestId());
    }

    /**
     * Erstellt eine Error-Response für World-Registrierungs-Fehler
     *
     * @param request Die ursprüngliche Anfrage
     * @param errorMessage Die Fehlermeldung
     * @return Error-Response
     */
    public WorldRegistrationResponse createWorldRegistrationErrorResponse(WorldRegistrationRequest request, String errorMessage) {
        long currentTimestamp = Instant.now().toEpochMilli();
        return createWorldRegistrationErrorResponse(request, errorMessage, currentTimestamp);
    }

    /**
     * Erstellt eine Error-Response für World-Registrierungs-Fehler mit Timestamp
     *
     * @param request Die ursprüngliche Anfrage
     * @param errorMessage Die Fehlermeldung
     * @param timestamp Zeitstempel der Antwort
     * @return Error-Response
     */
    private WorldRegistrationResponse createWorldRegistrationErrorResponse(WorldRegistrationRequest request, String errorMessage, long timestamp) {
        return WorldRegistrationResponse.newBuilder()
                .setRequestId(request.getRequestId())
                .setStatus(WorldRegistrationStatus.ERROR)
                .setPlanetName(request.getPlanetName())
                .setWorldId(request.getWorldId())
                .setWorldName(request.getWorldName())
                .setEnvironment(request.getEnvironment())
                .setTimestamp(Instant.ofEpochMilli(timestamp))
                .setMessage(null)
                .setErrorMessage(errorMessage)
                .build();
    }

    /**
     * Validiert eine eingehende World-Deregistrierungs-Anfrage
     *
     * @param request Die zu validierende Anfrage
     * @throws IllegalArgumentException wenn die Anfrage ungültig ist
     */
    public void validateWorldUnregistrationRequest(WorldUnregistrationRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("World unregistration request cannot be null");
        }

        if (request.getRequestId() == null || request.getRequestId().trim().isEmpty()) {
            throw new IllegalArgumentException("Request ID cannot be null or empty");
        }

        if (request.getWorldId() == null || request.getWorldId().trim().isEmpty()) {
            throw new IllegalArgumentException("World ID cannot be null or empty");
        }

        if (request.getEnvironment() == null) {
            throw new IllegalArgumentException("Environment cannot be null");
        }

        log.debug("World unregistration request validation passed: {}", request.getRequestId());
    }

    /**
     * Verarbeitet eine World-Deregistrierungs-Anfrage
     *
     * @param request Die eingehende World-Deregistrierungs-Anfrage
     * @return Die World-Deregistrierungs-Antwort mit Ergebnis
     */
    @Transactional
    public WorldUnregistrationResponse processWorldUnregistrationRequest(WorldUnregistrationRequest request) {
        log.info("Processing world unregistration request: worldId={}, planetName={}, environment={}, unregisteredBy={}",
                   request.getWorldId(), request.getPlanetName(), request.getEnvironment(), request.getUnregisteredBy());

        long currentTimestamp = Instant.now().toEpochMilli();

        try {
            // Suche nach der Welt anhand der worldId
            Optional<World> existingWorld = worldRepository.findByWorldId(request.getWorldId());

            if (existingWorld.isEmpty()) {
                log.warn("World {} not found for unregistration in environment {}",
                           request.getWorldId(), request.getEnvironment());

                return WorldUnregistrationResponse.newBuilder()
                        .setRequestId(request.getRequestId())
                        .setStatus(WorldUnregistrationStatus.WORLD_NOT_FOUND)
                        .setWorldId(request.getWorldId())
                        .setWorldName(null)
                        .setPlanetName(request.getPlanetName())
                        .setEnvironment(request.getEnvironment())
                        .setTimestamp(Instant.ofEpochMilli(currentTimestamp))
                        .setMessage("World not found for unregistration")
                        .setErrorMessage("World '" + request.getWorldId() + "' not found in registry")
                        .build();
            }

            World world = existingWorld.get();

            // Validiere Environment - Welt muss im angegebenen Environment existieren
            if (!world.getPlanet().getEnvironment().equals(request.getEnvironment())) {
                log.warn("World {} exists but not in environment {}, found in environment {}",
                           request.getWorldId(), request.getEnvironment(), world.getPlanet().getEnvironment());

                return WorldUnregistrationResponse.newBuilder()
                        .setRequestId(request.getRequestId())
                        .setStatus(WorldUnregistrationStatus.WORLD_NOT_FOUND)
                        .setWorldId(request.getWorldId())
                        .setWorldName(world.getName())
                        .setPlanetName(world.getPlanet().getName())
                        .setEnvironment(request.getEnvironment())
                        .setTimestamp(Instant.ofEpochMilli(currentTimestamp))
                        .setMessage("World not found in specified environment")
                        .setErrorMessage("World '" + request.getWorldId() + "' not found in environment '" + request.getEnvironment() + "'")
                        .build();
            }

            // Optional: Validiere Planet-Name falls angegeben
            if (request.getPlanetName() != null && !request.getPlanetName().trim().isEmpty()) {
                if (!world.getPlanet().getName().equalsIgnoreCase(request.getPlanetName().trim())) {
                    log.warn("World {} belongs to planet {} but request specified planet {}",
                               request.getWorldId(), world.getPlanet().getName(), request.getPlanetName());

                    return WorldUnregistrationResponse.newBuilder()
                            .setRequestId(request.getRequestId())
                            .setStatus(WorldUnregistrationStatus.ERROR)
                            .setWorldId(request.getWorldId())
                            .setWorldName(world.getName())
                            .setPlanetName(world.getPlanet().getName())
                            .setEnvironment(request.getEnvironment())
                            .setTimestamp(Instant.ofEpochMilli(currentTimestamp))
                            .setMessage("Planet name mismatch")
                            .setErrorMessage("World '" + request.getWorldId() + "' belongs to planet '" +
                                           world.getPlanet().getName() + "' not '" + request.getPlanetName() + "'")
                            .build();
                }
            }

            // Entferne die Welt aus der Datenbank (hard delete)
            // Alternative: Implementiere soft delete falls ein "active" Feld in der World-Entity vorhanden ist
            worldRepository.delete(world);

            log.info("Successfully unregistered world {} from planet {} in environment {}",
                       world.getWorldId(), world.getPlanet().getName(), request.getEnvironment());

            return WorldUnregistrationResponse.newBuilder()
                    .setRequestId(request.getRequestId())
                    .setStatus(WorldUnregistrationStatus.SUCCESS)
                    .setWorldId(world.getWorldId())
                    .setWorldName(world.getName())
                    .setPlanetName(world.getPlanet().getName())
                    .setEnvironment(request.getEnvironment())
                    .setTimestamp(Instant.ofEpochMilli(currentTimestamp))
                    .setMessage("World unregistration completed successfully")
                    .setErrorMessage(null)
                    .build();

        } catch (Exception e) {
            log.error("Error processing world unregistration request: {}", request.getRequestId(), e);
            return createWorldUnregistrationErrorResponse(request, e.getMessage(), currentTimestamp);
        }
    }

    /**
     * Erstellt eine Error-Response für World-Deregistrierungs-Fehler
     *
     * @param request Die ursprüngliche Anfrage
     * @param errorMessage Die Fehlermeldung
     * @return Error-Response
     */
    public WorldUnregistrationResponse createWorldUnregistrationErrorResponse(WorldUnregistrationRequest request, String errorMessage) {
        long currentTimestamp = Instant.now().toEpochMilli();
        return createWorldUnregistrationErrorResponse(request, errorMessage, currentTimestamp);
    }

    /**
     * Erstellt eine Error-Response für World-Deregistrierungs-Fehler mit Timestamp
     *
     * @param request Die ursprüngliche Anfrage
     * @param errorMessage Die Fehlermeldung
     * @param timestamp Zeitstempel der Antwort
     * @return Error-Response
     */
    private WorldUnregistrationResponse createWorldUnregistrationErrorResponse(WorldUnregistrationRequest request, String errorMessage, long timestamp) {
        return WorldUnregistrationResponse.newBuilder()
                .setRequestId(request.getRequestId())
                .setStatus(WorldUnregistrationStatus.ERROR)
                .setWorldId(request.getWorldId())
                .setWorldName(null)
                .setPlanetName(request.getPlanetName())
                .setEnvironment(request.getEnvironment())
                .setTimestamp(Instant.ofEpochMilli(timestamp))
                .setMessage(null)
                .setErrorMessage(errorMessage)
                .build();
    }
}
