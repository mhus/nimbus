package de.mhus.nimbus.worldgenerator.simple;

import de.mhus.nimbus.worldgenerator.model.PhaseInfo;
import de.mhus.nimbus.worldgenerator.processor.PhaseProcessor;
import de.mhus.nimbus.worldgenerator.service.GeneratorService;
import de.mhus.nimbus.world.shared.client.WorldTerrainClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * Simple Initialisierungs-Prozessor für die Weltgenerierung.
 * Setzt grundlegende Parameter und Welteinstellungen.
 */
@Component
@Slf4j
public class SimpleInitializationProcessor implements PhaseProcessor {

    private final Random random = new Random();

    @Autowired
    private WorldTerrainClient worldTerrainClient;

    @Autowired
    private GeneratorService generatorService;

    @Override
    public void processPhase(PhaseInfo phase) throws Exception {
        log.info("Starte Initialisierungs-Phase für Welt-Generator ID: {}", phase.getWorldGeneratorId());

        Map<String, Object> parameters = phase.getParameters();

        // Simuliere Initialisierungszeit
        Thread.sleep(1000 + random.nextInt(2000));

        // Setze grundlegende Weltparameter
        int worldSize = (Integer) parameters.getOrDefault("worldSize", 1000);
        String biome = (String) parameters.getOrDefault("primaryBiome", "forest");
        int seed = (Integer) parameters.getOrDefault("seed", random.nextInt(1000000));

        log.info("Welt initialisiert - Größe: {}x{}, Primäres Biom: {}, Seed: {}",
                worldSize, worldSize, biome, seed);

        // Berechne Kontinentverteilung basierend auf Seed
        Random seedRandom = new Random(seed);
        int forestPercent = 20 + seedRandom.nextInt(30);
        int desertPercent = 15 + seedRandom.nextInt(25);
        int oceanPercent = 25 + seedRandom.nextInt(20);
        int mountainPercent = 100 - forestPercent - desertPercent - oceanPercent;

        log.info("Kontinentverteilung: Wald {}%, Wüste {}%, Ozean {}%, Berge {}%",
                forestPercent, desertPercent, oceanPercent, mountainPercent);

        // Generiere eindeutige WorldId
        String worldId = "world-" + UUID.randomUUID().toString();
        String worldName = (String) parameters.getOrDefault("worldName", "Generated World " + phase.getWorldGeneratorId());
        String worldDescription = "Generierte Welt mit " + biome + " Biom (Seed: " + seed + ")";

        // Erstelle Welt-Properties für den World Terrain Service
        Map<String, Object> worldProperties = new HashMap<>();
        worldProperties.put("seed", seed);
        worldProperties.put("primaryBiome", biome);
        worldProperties.put("forestPercent", forestPercent);
        worldProperties.put("desertPercent", desertPercent);
        worldProperties.put("oceanPercent", oceanPercent);
        worldProperties.put("mountainPercent", mountainPercent);
        worldProperties.put("generatorId", phase.getWorldGeneratorId());
        worldProperties.put("createdBy", "SimpleInitializationProcessor");

        try {
            // Lege die Welt im World Terrain Service an
            log.info("Erstelle Welt im World Terrain Service: {}", worldName);
            String createdWorldId = worldTerrainClient.createWorld(
                    worldId,
                    worldName,
                    worldDescription,
                    worldSize,
                    worldSize,
                    worldProperties
            );

            // Speichere die WorldId und andere Properties in der WorldGenerator Entität
            Map<String, String> generatorProperties = new HashMap<>();
            generatorProperties.put("worldId", createdWorldId);
            generatorProperties.put("worldName", worldName);
            generatorProperties.put("worldSize", String.valueOf(worldSize));
            generatorProperties.put("seed", String.valueOf(seed));
            generatorProperties.put("primaryBiome", biome);
            generatorProperties.put("forestPercent", String.valueOf(forestPercent));
            generatorProperties.put("desertPercent", String.valueOf(desertPercent));
            generatorProperties.put("oceanPercent", String.valueOf(oceanPercent));
            generatorProperties.put("mountainPercent", String.valueOf(mountainPercent));

            generatorService.updateWorldGeneratorProperties(phase.getWorldGeneratorId(), generatorProperties);

            log.info("Welt erfolgreich im World Terrain Service angelegt - WorldId: {}", createdWorldId);

        } catch (Exception e) {
            log.error("Fehler beim Anlegen der Welt im World Terrain Service: {}", e.getMessage(), e);
            throw new RuntimeException("Fehler beim Anlegen der Welt im World Terrain Service: " + e.getMessage(), e);
        }
    }

    @Override
    public String getPhaseType() {
        return "INITIALIZATION";
    }

    @Override
    public String getProcessorName() {
        return "SimpleInitializationProcessor";
    }
}
