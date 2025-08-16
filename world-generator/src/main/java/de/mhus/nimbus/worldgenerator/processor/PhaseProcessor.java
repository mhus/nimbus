package de.mhus.nimbus.worldgenerator.processor;

import de.mhus.nimbus.worldgenerator.model.PhaseInfo;

/**
 * Interface für Phasen-Prozessoren, die verschiedene Aspekte der Weltgenerierung behandeln.
 */
public interface PhaseProcessor {

    /**
     * Verarbeitet eine Phase der Weltgenerierung.
     *
     * @param phase Die Phaseninformationen
     * @throws Exception wenn ein Fehler während der Verarbeitung auftritt
     */
    void processPhase(PhaseInfo phase) throws Exception;

    /**
     * Gibt den Typ der Phase zurück, die dieser Prozessor behandelt.
     *
     * @return Der Phasentyp (z.B. "TERRAIN", "STRUCTURE", "ITEM")
     */
    String getPhaseType();

    /**
     * Gibt den Namen des Prozessors zurück.
     *
     * @return Der Prozessorname
     */
    String getProcessorName();
}
