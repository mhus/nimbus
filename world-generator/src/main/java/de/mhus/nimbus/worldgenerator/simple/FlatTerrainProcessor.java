package de.mhus.nimbus.worldgenerator.simple;

import de.mhus.nimbus.worldgenerator.model.PhaseInfo;
import de.mhus.nimbus.worldgenerator.processor.PhaseProcessor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Random;

/**
 * Simple Terrain-Generierung für flache Landschaften.
 * Erstellt ebene Flächen mit minimaler Höhenvariation.
 */
@Component
@Slf4j
public class FlatTerrainProcessor implements PhaseProcessor {

    private final Random random = new Random();

    @Override
    public void processPhase(PhaseInfo phase) throws Exception {
        log.info("Starte Flachland-Terrain-Generierung für Welt-Generator ID: {}", phase.getWorldGeneratorId());

        Map<String, Object> parameters = phase.getParameters();
        int worldSize = (Integer) parameters.getOrDefault("worldSize", 1000);
        int seed = (Integer) parameters.getOrDefault("seed", random.nextInt(1000000));

        Random seedRandom = new Random(seed);

        // Simuliere Terrain-Generierung
        Thread.sleep(1500 + random.nextInt(2500));

        // Generiere flaches Terrain mit minimaler Variation
        float baseHeight = 50.0f; // Basis-Höhe in Metern
        float maxVariation = 5.0f; // Maximale Höhenvariation

        int chunks = (worldSize / 100) * (worldSize / 100); // 100x100 Chunks

        for (int i = 0; i < chunks; i++) {
            float chunkHeight = baseHeight + (seedRandom.nextFloat() - 0.5f) * maxVariation;

            // Gelegentliche kleine Hügel für Abwechslung
            if (seedRandom.nextDouble() < 0.05) { // 5% Chance
                chunkHeight += 10 + seedRandom.nextFloat() * 15; // Kleine Hügel
                log.debug("Kleiner Hügel bei Chunk {} - Höhe: {:.1f}m", i, chunkHeight);
            }

            // Gelegentliche flache Täler
            if (seedRandom.nextDouble() < 0.03) { // 3% Chance
                chunkHeight -= 5 + seedRandom.nextFloat() * 10; // Flache Senken
                log.debug("Flaches Tal bei Chunk {} - Höhe: {:.1f}m", i, chunkHeight);
            }
        }

        // Generiere Flüsse und Bäche
        generateRivers(worldSize, seedRandom);

        log.info("Flachland-Terrain generiert - Basis-Höhe: {:.1f}m, Variation: ±{:.1f}m",
                baseHeight, maxVariation);
    }

    private void generateRivers(int worldSize, Random seedRandom) {
        int riverCount = 2 + seedRandom.nextInt(4); // 2-5 Flüsse

        for (int i = 0; i < riverCount; i++) {
            int startX = seedRandom.nextInt(worldSize);
            int startY = seedRandom.nextInt(worldSize);
            int riverLength = 100 + seedRandom.nextInt(300);

            log.debug("Fluss {} - Start: ({}, {}), Länge: {}m", i+1, startX, startY, riverLength);
        }
    }

    @Override
    public String getPhaseType() {
        return "TERRAIN_GENERATION";
    }

    @Override
    public String getProcessorName() {
        return "FlatTerrainProcessor";
    }
}
