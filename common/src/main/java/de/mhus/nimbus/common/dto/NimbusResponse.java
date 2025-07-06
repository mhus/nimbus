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
     * Zusätzliche Metadaten (optional)
     */
    private ResponseMetadata metadata;

    /**
     * Erstellt eine erfolgreiche Response mit Daten
     *
     * @param requestId Die Request-ID
     * @param data Die Response-Daten
     * @param <T> Der Typ der Response-Daten
     * @return NimbusResponse mit Success-Status
     */
    public static <T> NimbusResponse<T> success(String requestId, T data) {
        NimbusResponse<T> response = new NimbusResponse<>();
        response.setRequestId(requestId);
        response.setSuccess(true);
        response.setData(data);
        response.setTimestamp(Instant.now());
        return response;
    }

    /**
     * Erstellt eine erfolgreiche Response ohne Daten
     *
     * @param requestId Die Request-ID
     * @return NimbusResponse mit Success-Status
     */
    public static NimbusResponse<Void> success(String requestId) {
        return success(requestId, null);
    }

    /**
     * Erstellt eine erfolgreiche Response mit Daten und Metadaten
     *
     * @param requestId Die Request-ID
     * @param data Die Response-Daten
     * @param metadata Zusätzliche Metadaten
     * @param <T> Der Typ der Response-Daten
     * @return NimbusResponse mit Success-Status
     */
    public static <T> NimbusResponse<T> success(String requestId, T data, ResponseMetadata metadata) {
        NimbusResponse<T> response = success(requestId, data);
        response.setMetadata(metadata);
        return response;
    }

    /**
     * Erstellt eine Error-Response
     *
     * @param requestId Die Request-ID
     * @param errorMessage Die Fehlermeldung
     * @param errorCode Der Fehlercode
     * @param serviceName Der Service-Name
     * @return NimbusResponse mit Error-Status
     */
    public static NimbusResponse<Void> error(String requestId, String errorMessage, String errorCode, String serviceName) {
        NimbusResponse<Void> response = new NimbusResponse<>();
        response.setRequestId(requestId);
        response.setSuccess(false);
        response.setErrorMessage(errorMessage);
        response.setErrorCode(errorCode);
        response.setServiceName(serviceName);
        response.setTimestamp(Instant.now());
        return response;
    }

    /**
     * Erstellt eine Error-Response ohne Service-Name
     *
     * @param requestId Die Request-ID
     * @param errorMessage Die Fehlermeldung
     * @param errorCode Der Fehlercode
     * @return NimbusResponse mit Error-Status
     */
    public static NimbusResponse<Void> error(String requestId, String errorMessage, String errorCode) {
        return error(requestId, errorMessage, errorCode, null);
    }

    /**
     * Erstellt eine Error-Response nur mit Fehlermeldung
     *
     * @param requestId Die Request-ID
     * @param errorMessage Die Fehlermeldung
     * @return NimbusResponse mit Error-Status
     */
    public static NimbusResponse<Void> error(String requestId, String errorMessage) {
        return error(requestId, errorMessage, "INTERNAL_ERROR", null);
    }

    /**
     * Prüft ob die Response einen Fehler darstellt
     *
     * @return true wenn es sich um einen Fehler handelt
     */
    public boolean isError() {
        return !success;
    }

    /**
     * Prüft ob die Response erfolgreich ist
     *
     * @return true wenn erfolgreich
     */
    public boolean isSuccess() {
        return success;
    }

    /**
     * Metadaten für zusätzliche Response-Informationen
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResponseMetadata {

        /**
         * Verarbeitungszeit in Millisekunden
         */
        private Long processingTimeMs;

        /**
         * Version der API
         */
        private String apiVersion;

        /**
         * Paging-Informationen für Listen-Responses
         */
        private PagingInfo pagingInfo;

        /**
         * Zusätzliche benutzerdefinierte Metadaten
         */
        private java.util.Map<String, Object> customData;

        /**
         * Erstellt Metadaten nur mit Verarbeitungszeit
         *
         * @param processingTimeMs Verarbeitungszeit in Millisekunden
         * @return ResponseMetadata Instanz
         */
        public static ResponseMetadata withProcessingTime(Long processingTimeMs) {
            ResponseMetadata metadata = new ResponseMetadata();
            metadata.setProcessingTimeMs(processingTimeMs);
            return metadata;
        }

        /**
         * Erstellt Metadaten mit API-Version
         *
         * @param apiVersion Version der API
         * @return ResponseMetadata Instanz
         */
        public static ResponseMetadata withApiVersion(String apiVersion) {
            ResponseMetadata metadata = new ResponseMetadata();
            metadata.setApiVersion(apiVersion);
            return metadata;
        }
    }

    /**
     * Paging-Informationen für Listen-Responses
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PagingInfo {

        /**
         * Aktuelle Seite (0-basiert)
         */
        private int currentPage;

        /**
         * Anzahl Elemente pro Seite
         */
        private int pageSize;

        /**
         * Gesamtanzahl Elemente
         */
        private long totalElements;

        /**
         * Gesamtanzahl Seiten
         */
        private int totalPages;

        /**
         * Gibt es eine nächste Seite
         */
        private boolean hasNext;

        /**
         * Gibt es eine vorherige Seite
         */
        private boolean hasPrevious;

        /**
         * Berechnet PagingInfo basierend auf den Eingabeparametern
         *
         * @param currentPage Aktuelle Seite (0-basiert)
         * @param pageSize Elemente pro Seite
         * @param totalElements Gesamtanzahl Elemente
         * @return PagingInfo Instanz
         */
        public static PagingInfo of(int currentPage, int pageSize, long totalElements) {
            int totalPages = (int) Math.ceil((double) totalElements / pageSize);
            boolean hasNext = currentPage < totalPages - 1;
            boolean hasPrevious = currentPage > 0;

            return new PagingInfo(currentPage, pageSize, totalElements, totalPages, hasNext, hasPrevious);
        }
    }

    /**
     * Builder-Pattern für komplexe Response-Erstellung
     */
    public static class Builder<T> {
        private final NimbusResponse<T> response = new NimbusResponse<>();

        public Builder(String requestId) {
            response.setRequestId(requestId);
            response.setTimestamp(Instant.now());
        }

        public Builder<T> success(T data) {
            response.setSuccess(true);
            response.setData(data);
            return this;
        }

        public Builder<T> error(String errorMessage, String errorCode, String serviceName) {
            response.setSuccess(false);
            response.setErrorMessage(errorMessage);
            response.setErrorCode(errorCode);
            response.setServiceName(serviceName);
            return this;
        }

        public Builder<T> withMetadata(ResponseMetadata metadata) {
            response.setMetadata(metadata);
            return this;
        }

        public Builder<T> withProcessingTime(Long processingTimeMs) {
            if (response.getMetadata() == null) {
                response.setMetadata(new ResponseMetadata());
            }
            response.getMetadata().setProcessingTimeMs(processingTimeMs);
            return this;
        }

        public Builder<T> withApiVersion(String apiVersion) {
            if (response.getMetadata() == null) {
                response.setMetadata(new ResponseMetadata());
            }
            response.getMetadata().setApiVersion(apiVersion);
            return this;
        }

        public Builder<T> withPaging(PagingInfo pagingInfo) {
            if (response.getMetadata() == null) {
                response.setMetadata(new ResponseMetadata());
            }
            response.getMetadata().setPagingInfo(pagingInfo);
            return this;
        }

        public NimbusResponse<T> build() {
            return response;
        }
    }

    /**
     * Erstellt einen neuen Builder
     *
     * @param requestId Die Request-ID
     * @param <T> Der Typ der Response-Daten
     * @return Builder Instanz
     */
    public static <T> Builder<T> builder(String requestId) {
        return new Builder<>(requestId);
    }
}
