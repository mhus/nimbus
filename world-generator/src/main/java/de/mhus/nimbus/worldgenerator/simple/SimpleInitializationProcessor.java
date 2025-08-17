package de.mhus.nimbus.worldgenerator.simple;

import de.mhus.nimbus.worldgenerator.model.PhaseInfo;
import de.mhus.nimbus.worldgenerator.processor.PhaseProcessor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Random;

/**
 * Simple Initialisierungs-Prozessor für die Weltgenerierung.
 * Setzt grundlegende Parameter und Welteinstellungen.
 */
@Component
@Slf4j
public class SimpleInitializationProcessor implements PhaseProcessor {

    private final Random random = new Random();

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
