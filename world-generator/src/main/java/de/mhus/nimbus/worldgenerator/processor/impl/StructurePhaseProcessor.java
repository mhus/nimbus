package de.mhus.nimbus.worldgenerator.processor.impl;

import de.mhus.nimbus.worldgenerator.model.PhaseInfo;
import de.mhus.nimbus.worldgenerator.processor.PhaseProcessor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component("structureProcessor")
@Slf4j
public class StructurePhaseProcessor implements PhaseProcessor {

    @Override
    public void processPhase(PhaseInfo phase) throws Exception {
        log.info("Processing structure generation phase: {}", phase.getName());

        // Simulation der Struktur-Generierung
        Thread.sleep(1500); // Simuliert Verarbeitungszeit

        log.info("Structure generation completed for phase: {}", phase.getName());
    }

    @Override
    public String getPhaseType() {
        return "STRUCTURE";
    }

    @Override
    public String getProcessorName() {
        return "structureProcessor";
    }
}
