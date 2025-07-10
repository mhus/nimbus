package de.mhus.nimbus.common.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Standard Response DTO für alle Nimbus API-Antworten
 * Bietet einheitliche Struktur für Success- und Error-Responses
 *
 * @param <T> Der Typ der Response-Daten
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NimbusResponse<T> {

    /**
     * Eindeutige Request-ID für Tracing und Debugging
     */
    private String requestId;

    /**
     * Erfolgs-Flag der Operation
     */
    private boolean success;

    /**
     * Die eigentlichen Response-Daten (nur bei Erfolg)
     */
    private T data;

    /**
     * Fehlermeldung (nur bei Fehlern)
     */
    private String errorMessage;

    /**
     * Fehlercode für programmatische Behandlung (nur bei Fehlern)
     */
    private String errorCode;

    /**
     * Service-Name der den Fehler verursacht hat (nur bei Fehlern)
     */
    private String serviceName;

    /**
     * Timestamp der Response-Erstellung
     */
    private Instant timestamp;

    /**
     * Zusätzliche Nachricht (optional)
     */
    private String message;

    // Factory-Methoden für Success-Responses

    /**
     * Erstellt eine erfolgreiche Response
     */
    public static <T> NimbusResponse<T> success(String requestId, T data) {
        return success(requestId, data, null, null);
    }

    /**
     * Erstellt eine erfolgreiche Response mit Nachricht
     */
    public static <T> NimbusResponse<T> success(String requestId, T data, String message) {
        return success(requestId, data, message, null);
    }

    /**
     * Erstellt eine erfolgreiche Response mit Nachricht und Service-Name
     */
    public static <T> NimbusResponse<T> success(String requestId, T data, String message, String serviceName) {
        NimbusResponse<T> response = new NimbusResponse<>();
        response.setRequestId(requestId);
        response.setSuccess(true);
        response.setData(data);
        response.setMessage(message);
        response.setServiceName(serviceName);
        response.setTimestamp(Instant.now());
        return response;
    }

    // Factory-Methoden für Error-Responses

    /**
     * Erstellt eine Fehler-Response
     */
    public static <T> NimbusResponse<T> error(String requestId, String errorCode, String errorMessage) {
        return error(requestId, errorCode, errorMessage, null);
    }

    /**
     * Erstellt eine Fehler-Response mit Service-Name
     */
    public static <T> NimbusResponse<T> error(String requestId, String errorCode, String errorMessage, String serviceName) {
        NimbusResponse<T> response = new NimbusResponse<>();
        response.setRequestId(requestId);
        response.setSuccess(false);
        response.setErrorCode(errorCode);
        response.setErrorMessage(errorMessage);
        response.setServiceName(serviceName);
        response.setTimestamp(Instant.now());
        return response;
    }

    // Convenience-Methoden

    /**
     * Prüft ob die Response erfolgreich war
     */
    public boolean isSuccess() {
        return success;
    }

    /**
     * Prüft ob die Response einen Fehler enthält
     */
    public boolean isError() {
        return !success;
    }

    /**
     * Gibt true zurück wenn Daten vorhanden sind
     */
    public boolean hasData() {
        return data != null;
    }

    /**
     * Gibt true zurück wenn ein Fehlercode vorhanden ist
     */
    public boolean hasErrorCode() {
        return errorCode != null && !errorCode.trim().isEmpty();
    }
}
