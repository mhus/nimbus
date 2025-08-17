package de.mhus.nimbus.worldgenerator.simple;

import de.mhus.nimbus.worldgenerator.processor.PhaseProcessor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Konfiguration für die Simple World Generator Prozessoren.
 * Registriert alle verfügbaren Simple-Implementierungen.
 */
@Configuration
@Slf4j
public class SimpleProcessorConfiguration {

    @Bean
    public Map<String, PhaseProcessor> simpleProcessorRegistry(
            @Autowired List<PhaseProcessor> allProcessors) {

        Map<String, PhaseProcessor> processorMap = new HashMap<>();

        for (PhaseProcessor processor : allProcessors) {
            if (processor.getClass().getPackage().getName().contains(".simple")) {
                String key = processor.getPhaseType() + "_" + processor.getProcessorName();
                processorMap.put(key, processor);
                log.info("Registriere Simple Prozessor: {} für Phase: {}",
                        processor.getProcessorName(), processor.getPhaseType());
            }
        }

        log.info("Simple Prozessor Registry initialisiert mit {} Prozessoren", processorMap.size());
        return processorMap;
    }

    /**
     * Hilfsmethode um einen spezifischen Simple Prozessor zu finden.
     */
    @Bean
    public SimpleProcessorService simpleProcessorService(
            Map<String, PhaseProcessor> simpleProcessorRegistry) {
        return new SimpleProcessorService(simpleProcessorRegistry);
    }
}
