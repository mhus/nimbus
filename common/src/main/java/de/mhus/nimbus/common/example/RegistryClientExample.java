package de.mhus.nimbus.common.example;

import de.mhus.nimbus.common.client.RegistryClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * Beispiel-Service, der zeigt, wie der RegistryClient verwendet wird
 */
@Service
public class RegistryClientExample {

    private static final Logger LOGGER = LoggerFactory.getLogger(RegistryClientExample.class);

    private final RegistryClient registryClient;

    @Autowired
    public RegistryClientExample(RegistryClient registryClient) {
        this.registryClient = registryClient;
    }

    /**
     * Beispiel: Registriert einen neuen Planeten
     */
    public CompletableFuture<Void> registerPlanetExample(String planetName, String serverAddress, int port) {
        String planetId = "planet_" + planetName.toLowerCase().replaceAll("\\s+", "_");

        LOGGER.info("Registriere Planet '{}' mit ID '{}' auf {}:{}", planetName, planetId, serverAddress, port);

        return registryClient.registerPlanet(
                planetId,
                planetName,
                "Ein wunderschöner Planet für Abenteuer",
                serverAddress,
                port,
                "2.0.1",
                500,
                "ADVENTURE"
            )
            .thenRun(() -> LOGGER.info("Planet '{}' erfolgreich registriert", planetName))
            .exceptionally(throwable -> {
                LOGGER.error("Fehler bei der Planet-Registrierung für '{}': {}", planetName, throwable.getMessage());
                return null;
            });
    }

    /**
     * Beispiel: Einfache Planet-Registrierung
     */
    public CompletableFuture<Void> simpleRegisterPlanetExample(String planetId, String planetName) {
        LOGGER.info("Einfache Registrierung für Planet '{}' (ID: {})", planetName, planetId);

        return registryClient.registerPlanet(planetId, planetName, "localhost", 8080)
            .thenRun(() -> LOGGER.info("Planet '{}' einfach registriert", planetName))
            .exceptionally(throwable -> {
                LOGGER.error("Fehler bei der einfachen Planet-Registrierung: {}", throwable.getMessage());
                return null;
            });
    }

    /**
     * Beispiel: Sucht einen Planeten nach ID
     */
    public CompletableFuture<Void> findPlanetByIdExample(String planetId) {
        LOGGER.info("Suche Planet mit ID '{}'", planetId);

        return registryClient.lookupPlanetById(planetId)
            .thenRun(() -> LOGGER.info("Planet-Suche für ID '{}' erfolgreich gesendet", planetId))
            .exceptionally(throwable -> {
                LOGGER.error("Fehler bei der Planet-Suche für ID '{}': {}", planetId, throwable.getMessage());
                return null;
            });
    }

    /**
     * Beispiel: Sucht einen Planeten nach Name
     */
    public CompletableFuture<Void> findPlanetByNameExample(String planetName) {
        LOGGER.info("Suche Planet mit Name '{}'", planetName);

        return registryClient.lookupPlanetByName(planetName)
            .thenRun(() -> LOGGER.info("Planet-Suche für Name '{}' erfolgreich gesendet", planetName))
            .exceptionally(throwable -> {
                LOGGER.error("Fehler bei der Planet-Suche für Name '{}': {}", planetName, throwable.getMessage());
                return null;
            });
    }

    /**
     * Beispiel: Erweiterte Planet-Suche nach Typ
     */
    public CompletableFuture<Void> findPlanetsByTypeExample(String planetType, boolean includeInactive) {
        LOGGER.info("Suche Planeten vom Typ '{}' (Inaktive einschließen: {})", planetType, includeInactive);

        return registryClient.lookupPlanet(null, null, planetType, null, includeInactive)
            .thenRun(() -> LOGGER.info("Erweiterte Planet-Suche für Typ '{}' erfolgreich gesendet", planetType))
            .exceptionally(throwable -> {
                LOGGER.error("Fehler bei der erweiterten Planet-Suche: {}", throwable.getMessage());
                return null;
            });
    }

    /**
     * Beispiel: Registriert eine neue Welt
     */
    public CompletableFuture<Void> registerWorldExample(String worldName, String planetId) {
        String worldId = "world_" + worldName.toLowerCase().replaceAll("\\s+", "_");

        LOGGER.info("Registriere Welt '{}' mit ID '{}' auf Planet '{}'", worldName, worldId, planetId);

        return registryClient.registerWorld(
                worldId,
                worldName,
                planetId,
                "Eine aufregende Welt voller Möglichkeiten",
                "SURVIVAL",
                100,
                "{\"difficulty\":\"normal\",\"pvp\":true}"
            )
            .thenRun(() -> LOGGER.info("Welt '{}' erfolgreich registriert", worldName))
            .exceptionally(throwable -> {
                LOGGER.error("Fehler bei der Welt-Registrierung für '{}': {}", worldName, throwable.getMessage());
                return null;
            });
    }

    /**
     * Beispiel: Einfache Welt-Registrierung
     */
    public CompletableFuture<Void> simpleRegisterWorldExample(String worldId, String worldName, String planetId) {
        LOGGER.info("Einfache Registrierung für Welt '{}' auf Planet '{}'", worldName, planetId);

        return registryClient.registerWorld(worldId, worldName, planetId)
            .thenRun(() -> LOGGER.info("Welt '{}' einfach registriert", worldName))
            .exceptionally(throwable -> {
                LOGGER.error("Fehler bei der einfachen Welt-Registrierung: {}", throwable.getMessage());
                return null;
            });
    }

    /**
     * Beispiel: Aktualisiert die Spieleranzahl für einen Planeten
     */
    public CompletableFuture<Void> updatePlanetPlayersExample(String planetId, int playerCount) {
        LOGGER.info("Aktualisiere Spieleranzahl für Planet '{}' auf {}", planetId, playerCount);

        return registryClient.updatePlanetPlayerCount(planetId, playerCount)
            .thenRun(() -> LOGGER.info("Spieleranzahl für Planet '{}' erfolgreich aktualisiert", planetId))
            .exceptionally(throwable -> {
                LOGGER.error("Fehler beim Aktualisieren der Spieleranzahl für Planet '{}': {}",
                           planetId, throwable.getMessage());
                return null;
            });
    }

    /**
     * Beispiel: Aktualisiert die Spieleranzahl für eine Welt
     */
    public CompletableFuture<Void> updateWorldPlayersExample(String worldId, int playerCount) {
        LOGGER.info("Aktualisiere Spieleranzahl für Welt '{}' auf {}", worldId, playerCount);

        return registryClient.updateWorldPlayerCount(worldId, playerCount)
            .thenRun(() -> LOGGER.info("Spieleranzahl für Welt '{}' erfolgreich aktualisiert", worldId))
            .exceptionally(throwable -> {
                LOGGER.error("Fehler beim Aktualisieren der Spieleranzahl für Welt '{}': {}",
                           worldId, throwable.getMessage());
                return null;
            });
    }

    /**
     * Beispiel: Deregistriert einen Planeten
     */
    public CompletableFuture<Void> unregisterPlanetExample(String planetId) {
        LOGGER.info("Deregistriere Planet mit ID '{}'", planetId);

        return registryClient.unregisterPlanet(planetId)
            .thenRun(() -> LOGGER.info("Planet '{}' erfolgreich deregistriert", planetId))
            .exceptionally(throwable -> {
                LOGGER.error("Fehler bei der Planet-Deregistrierung für '{}': {}", planetId, throwable.getMessage());
                return null;
            });
    }

    /**
     * Beispiel: Deregistriert eine Welt
     */
    public CompletableFuture<Void> unregisterWorldExample(String worldId) {
        LOGGER.info("Deregistriere Welt mit ID '{}'", worldId);

        return registryClient.unregisterWorld(worldId)
            .thenRun(() -> LOGGER.info("Welt '{}' erfolgreich deregistriert", worldId))
            .exceptionally(throwable -> {
                LOGGER.error("Fehler bei der Welt-Deregistrierung für '{}': {}", worldId, throwable.getMessage());
                return null;
            });
    }

    /**
     * Beispiel: Kombinierte Operation - Registriert Planet und Welt
     */
    public CompletableFuture<Void> registerPlanetAndWorldExample(String planetName, String worldName,
                                                                String serverAddress, int port) {
        String planetId = "planet_" + planetName.toLowerCase().replaceAll("\\s+", "_");
        String worldId = "world_" + worldName.toLowerCase().replaceAll("\\s+", "_");

        LOGGER.info("Starte kombinierte Registrierung: Planet '{}' und Welt '{}'", planetName, worldName);

        return registryClient.registerPlanet(planetId, planetName, serverAddress, port)
            .thenCompose(result -> {
                LOGGER.info("Planet registriert, registriere nun Welt...");
                return registryClient.registerWorld(worldId, worldName, planetId);
            })
            .thenRun(() -> LOGGER.info("Planet '{}' und Welt '{}' erfolgreich registriert", planetName, worldName))
            .exceptionally(throwable -> {
                LOGGER.error("Fehler bei der kombinierten Registrierung: {}", throwable.getMessage());
                return null;
            });
    }

    /**
     * Beispiel: Server-Startup-Prozess
     */
    public CompletableFuture<Void> serverStartupExample(String serverName) {
        String planetId = "planet_" + serverName;
        String worldId1 = "world_survival_" + serverName;
        String worldId2 = "world_creative_" + serverName;

        LOGGER.info("Starte Server-Startup-Prozess für '{}'", serverName);

        return registryClient.registerPlanet(planetId, serverName + " Planet", "localhost", 25565)
            .thenCompose(result -> {
                LOGGER.info("Planet registriert, registriere Survival-Welt...");
                return registryClient.registerWorld(worldId1, "Survival World", planetId,
                                                   "Survival Welt", "SURVIVAL", 50,
                                                   "{\"difficulty\":\"hard\",\"pvp\":true}");
            })
            .thenCompose(result -> {
                LOGGER.info("Survival-Welt registriert, registriere Creative-Welt...");
                return registryClient.registerWorld(worldId2, "Creative World", planetId,
                                                   "Creative Welt", "CREATIVE", 30,
                                                   "{\"difficulty\":\"peaceful\",\"pvp\":false}");
            })
            .thenRun(() -> LOGGER.info("Server-Startup für '{}' erfolgreich abgeschlossen", serverName))
            .exceptionally(throwable -> {
                LOGGER.error("Fehler beim Server-Startup für '{}': {}", serverName, throwable.getMessage());
                return null;
            });
    }

    /**
     * Beispiel: Batch-Operation für mehrere Planeten
     */
    public CompletableFuture<Void> batchRegisterPlanetsExample(String[] planetNames, String baseAddress, int basePort) {
        LOGGER.info("Starte Batch-Registrierung für {} Planeten", planetNames.length);

        CompletableFuture<Void>[] futures = new CompletableFuture[planetNames.length];

        for (int i = 0; i < planetNames.length; i++) {
            final String planetName = planetNames[i];
            final int port = basePort + i;
            final String planetId = "batch_planet_" + i;

            futures[i] = registryClient.registerPlanet(planetId, planetName, baseAddress, port)
                .exceptionally(throwable -> {
                    LOGGER.warn("Fehler bei der Registrierung von Planet '{}': {}", planetName, throwable.getMessage());
                    return null;
                });
        }

        return CompletableFuture.allOf(futures)
            .thenRun(() -> LOGGER.info("Batch-Registrierung für alle {} Planeten abgeschlossen", planetNames.length))
            .exceptionally(throwable -> {
                LOGGER.error("Fehler bei der Batch-Registrierung: {}", throwable.getMessage());
                return null;
            });
    }

    /**
     * Beispiel: Simuliert Player-Join-Event
     */
    public CompletableFuture<Void> playerJoinExample(String planetId, String worldId) {
        LOGGER.info("Simuliere Player-Join für Planet '{}' und Welt '{}'", planetId, worldId);

        return registryClient.updatePlanetPlayerCount(planetId, 1)
            .thenCompose(result -> {
                LOGGER.info("Planet-Spieleranzahl aktualisiert, aktualisiere Welt...");
                return registryClient.updateWorldPlayerCount(worldId, 1);
            })
            .thenRun(() -> LOGGER.info("Player-Join erfolgreich verarbeitet"))
            .exceptionally(throwable -> {
                LOGGER.error("Fehler beim Player-Join: {}", throwable.getMessage());
                return null;
            });
    }
}
