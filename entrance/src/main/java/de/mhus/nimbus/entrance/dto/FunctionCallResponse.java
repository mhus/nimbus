package de.mhus.nimbus.shared.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO für Antworten auf Funktionsaufrufe
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FunctionCallResponse {

    /**
     * Ob der Funktionsaufruf erfolgreich war
     */
    private boolean success;

    /**
     * Ergebnis des Funktionsaufrufs (bei Erfolg)
     */
    private Object result;

    /**
     * Fehlermeldung (bei Misserfolg)
     */
    private String error;
}
