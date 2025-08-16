package de.mhus.nimbus.worldgenerator.processor.impl;

import de.mhus.nimbus.worldgenerator.model.PhaseInfo;
import de.mhus.nimbus.worldgenerator.processor.PhaseProcessor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component("terrainProcessor")
@Slf4j
public class TerrainPhaseProcessor implements PhaseProcessor {

    @Override
    public void processPhase(PhaseInfo phase) throws Exception {
        log.info("Processing terrain generation phase: {}", phase.getName());

        // Simulation der Terrain-Generierung
        Thread.sleep(1000); // Simuliert Verarbeitungszeit

        log.info("Terrain generation completed for phase: {}", phase.getName());
    }

    @Override
    public String getPhaseType() {
        return "TERRAIN";
    }

    @Override
    public String getProcessorName() {
        return "terrainProcessor";
    }
}
