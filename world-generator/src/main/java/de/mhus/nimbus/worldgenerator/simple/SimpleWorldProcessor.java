package de.mhus.nimbus.worldgenerator.simple;

import de.mhus.nimbus.worldgenerator.model.PhaseInfo;
import de.mhus.nimbus.worldgenerator.processor.PhaseProcessor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Simple World Processor für die finale Initialisierung der einfachen Welt.
 * Führt abschließende Optimierungen und Validierungen durch.
 */
@Component("simpleWorldProcessor")
@Slf4j
public class SimpleWorldProcessor implements PhaseProcessor {

    @Override
    public void processPhase(PhaseInfo phase) throws Exception {
        log.info("Processing simple world finalization phase: {}", phase.getName());

        // Weltparameter validieren
        validateWorldParameters(phase);

        // Biome-Konsistenz prüfen
        validateBiomeConsistency(phase);

        // Asset-Integrität überprüfen
        validateAssetIntegrity(phase);

        // Finale Optimierungen
        performFinalOptimizations(phase);

        log.info("Simple world finalization completed for phase: {}", phase.getName());
    }

    private void validateWorldParameters(PhaseInfo phase) throws InterruptedException {
        log.info("Validating world parameters for simple world");
        Thread.sleep(150); // Simuliert Validierungszeit
    }

    private void validateBiomeConsistency(PhaseInfo phase) throws InterruptedException {
        log.info("Checking biome consistency and transitions");
        Thread.sleep(200); // Simuliert Validierungszeit
    }

    private void validateAssetIntegrity(PhaseInfo phase) throws InterruptedException {
        log.info("Validating asset integrity and material assignments");
        Thread.sleep(100); // Simuliert Validierungszeit
    }

    private void performFinalOptimizations(PhaseInfo phase) throws InterruptedException {
        log.info("Performing final world optimizations");
        Thread.sleep(250); // Simuliert Optimierungszeit
    }

    @Override
    public String getPhaseType() {
        return "SIMPLE_WORLD";
    }

    @Override
    public String getProcessorName() {
        return "simpleWorldProcessor";
    }
}
