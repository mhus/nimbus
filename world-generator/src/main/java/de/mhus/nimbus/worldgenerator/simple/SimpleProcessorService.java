package de.mhus.nimbus.worldgenerator.simple;

import de.mhus.nimbus.worldgenerator.processor.PhaseProcessor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;

/**
 * Service für die Verwaltung der Simple World Generator Prozessoren.
 * Bietet einfachen Zugriff auf verfügbare Simple-Implementierungen.
 */
@Service
@Slf4j
public class SimpleProcessorService {

    private final Map<String, PhaseProcessor> processorRegistry;

    public SimpleProcessorService(Map<String, PhaseProcessor> processorRegistry) {
        this.processorRegistry = processorRegistry;
    }

    /**
     * Findet einen Simple Prozessor basierend auf Phase und Prozessorname.
     */
    public Optional<PhaseProcessor> findProcessor(String phaseType, String processorName) {
        String key = phaseType + "_" + processorName;
        PhaseProcessor processor = processorRegistry.get(key);

        if (processor != null) {
            log.debug("Simple Prozessor gefunden: {} für Phase: {}", processorName, phaseType);
            return Optional.of(processor);
        }

        log.warn("Kein Simple Prozessor gefunden für Phase: {}, Prozessor: {}", phaseType, processorName);
        return Optional.empty();
    }

    /**
     * Gibt alle verfügbaren Simple Prozessoren zurück.
     */
    public Map<String, PhaseProcessor> getAllProcessors() {
        return processorRegistry;
    }

    /**
     * Überprüft ob ein Simple Prozessor für eine Phase verfügbar ist.
     */
    public boolean hasProcessorForPhase(String phaseType) {
        return processorRegistry.values().stream()
                .anyMatch(processor -> processor.getPhaseType().equals(phaseType));
    }

    /**
     * Gibt die Namen aller verfügbaren Simple Prozessoren für eine Phase zurück.
     */
    public java.util.List<String> getProcessorNamesForPhase(String phaseType) {
        return processorRegistry.values().stream()
                .filter(processor -> processor.getPhaseType().equals(phaseType))
                .map(PhaseProcessor::getProcessorName)
                .collect(java.util.stream.Collectors.toList());
    }
}
