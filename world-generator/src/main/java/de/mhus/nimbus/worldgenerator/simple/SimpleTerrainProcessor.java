package de.mhus.nimbus.worldgenerator.simple;

import de.mhus.nimbus.worldgenerator.model.PhaseInfo;
import de.mhus.nimbus.worldgenerator.processor.PhaseProcessor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Simple Terrain Processor für die Generierung einfacher Landschaften.
 * Erstellt die Grundstruktur für Kontinente mit Wald, Wüste, Ozean und Bergen.
 */
@Component("simpleTerrainProcessor")
@Slf4j
public class SimpleTerrainProcessor implements PhaseProcessor {

    @Override
    public void processPhase(PhaseInfo phase) throws Exception {
        log.info("Processing simple terrain generation phase: {}", phase.getName());

        // Kontinenttypen generieren
        generateContinents(phase);

        // Grundlegende Topografie erstellen
        generateTopography(phase);

        log.info("Simple terrain generation completed for phase: {}", phase.getName());
    }

    private void generateContinents(PhaseInfo phase) throws InterruptedException {
        log.info("Generating continents: Wald, Wüste, Ozean, Berge");
        Thread.sleep(500); // Simuliert Verarbeitungszeit
    }

    private void generateTopography(PhaseInfo phase) throws InterruptedException {
        log.info("Generating basic topography for simple world");
        Thread.sleep(300); // Simuliert Verarbeitungszeit
    }

    @Override
    public String getPhaseType() {
        return "SIMPLE_TERRAIN";
    }

    @Override
    public String getProcessorName() {
        return "simpleTerrainProcessor";
    }
}
