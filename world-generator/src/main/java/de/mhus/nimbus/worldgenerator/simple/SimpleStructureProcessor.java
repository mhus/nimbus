package de.mhus.nimbus.worldgenerator.simple;

import de.mhus.nimbus.worldgenerator.model.PhaseInfo;
import de.mhus.nimbus.worldgenerator.processor.PhaseProcessor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Simple Structure Processor für die Platzierung von einfachen Strukturen.
 * Erstellt Pfade, Brücken und einfache Landschaftsmerkmale.
 */
@Component("simpleStructureProcessor")
@Slf4j
public class SimpleStructureProcessor implements PhaseProcessor {

    @Override
    public void processPhase(PhaseInfo phase) throws Exception {
        log.info("Processing simple structure generation phase: {}", phase.getName());

        // Grundlegende Strukturen erstellen
        generatePaths(phase);
        generateWaterfalls(phase);
        generateRivers(phase);
        generateNaturalFormations(phase);

        log.info("Simple structure generation completed for phase: {}", phase.getName());
    }

    private void generatePaths(PhaseInfo phase) throws InterruptedException {
        log.info("Generating paths connecting different biomes");
        Thread.sleep(200); // Simuliert Verarbeitungszeit
    }

    private void generateWaterfalls(PhaseInfo phase) throws InterruptedException {
        log.info("Placing waterfalls in mountain regions");
        Thread.sleep(150); // Simuliert Verarbeitungszeit
    }

    private void generateRivers(PhaseInfo phase) throws InterruptedException {
        log.info("Creating river systems from mountains to oceans");
        Thread.sleep(250); // Simuliert Verarbeitungszeit
    }

    private void generateNaturalFormations(PhaseInfo phase) throws InterruptedException {
        log.info("Adding natural rock formations and crystal deposits");
        Thread.sleep(200); // Simuliert Verarbeitungszeit
    }

    @Override
    public String getPhaseType() {
        return "SIMPLE_STRUCTURE";
    }

    @Override
    public String getProcessorName() {
        return "simpleStructureProcessor";
    }
}
