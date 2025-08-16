package de.mhus.nimbus.worldgenerator.simple;

import de.mhus.nimbus.worldgenerator.model.PhaseInfo;
import de.mhus.nimbus.worldgenerator.processor.PhaseProcessor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Simple Biome Processor für die Generierung der Biome in einfachen Welten.
 * Verteilt die verschiedenen Biome (Wald, Wüste, Ozean, Berge) auf den Kontinenten.
 */
@Component("simpleBiomeProcessor")
@Slf4j
public class SimpleBiomeProcessor implements PhaseProcessor {

    @Override
    public void processPhase(PhaseInfo phase) throws Exception {
        log.info("Processing simple biome generation phase: {}", phase.getName());

        // Biome-Verteilung basierend auf Kontinenttypen
        generateForestBiomes(phase);
        generateDesertBiomes(phase);
        generateOceanBiomes(phase);
        generateMountainBiomes(phase);
        generateSwampBiomes(phase);

        // Biome-Übergänge erstellen
        createBiomeTransitions(phase);

        log.info("Simple biome generation completed for phase: {}", phase.getName());
    }

    private void generateForestBiomes(PhaseInfo phase) throws InterruptedException {
        log.info("Generating forest biomes with trees, grass, and flowers");
        Thread.sleep(300); // Simuliert Verarbeitungszeit
    }

    private void generateDesertBiomes(PhaseInfo phase) throws InterruptedException {
        log.info("Generating desert biomes with sand and rocks");
        Thread.sleep(250); // Simuliert Verarbeitungszeit
    }

    private void generateOceanBiomes(PhaseInfo phase) throws InterruptedException {
        log.info("Generating ocean biomes with water, coral, and sea life");
        Thread.sleep(200); // Simuliert Verarbeitungszeit
    }

    private void generateMountainBiomes(PhaseInfo phase) throws InterruptedException {
        log.info("Generating mountain biomes with rocks, snow, and crystals");
        Thread.sleep(350); // Simuliert Verarbeitungszeit
    }

    private void generateSwampBiomes(PhaseInfo phase) throws InterruptedException {
        log.info("Generating swamp biomes with marsh vegetation and water");
        Thread.sleep(300); // Simuliert Verarbeitungszeit
    }

    private void createBiomeTransitions(PhaseInfo phase) throws InterruptedException {
        log.info("Creating smooth transitions between biomes");
        Thread.sleep(200); // Simuliert Verarbeitungszeit
    }

    @Override
    public String getPhaseType() {
        return "SIMPLE_BIOME";
    }

    @Override
    public String getProcessorName() {
        return "simpleBiomeProcessor";
    }
}
