package de.mhus.nimbus.worldgenerator.processor.impl;

import de.mhus.nimbus.worldgenerator.model.PhaseInfo;
import de.mhus.nimbus.worldgenerator.processor.PhaseProcessor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component("itemProcessor")
@Slf4j
public class ItemPhaseProcessor implements PhaseProcessor {

    @Override
    public void processPhase(PhaseInfo phase) throws Exception {
        log.info("Processing item generation phase: {}", phase.getName());

        // Simulation der Item-Generierung
        Thread.sleep(800); // Simuliert Verarbeitungszeit

        log.info("Item generation completed for phase: {}", phase.getName());
    }

    @Override
    public String getPhaseType() {
        return "ITEM";
    }

    @Override
    public String getProcessorName() {
        return "itemProcessor";
    }
}
