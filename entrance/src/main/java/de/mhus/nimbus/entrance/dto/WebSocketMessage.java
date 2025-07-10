package de.mhus.nimbus.shared.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Basis-Nachrichtenformat für WebSocket-Kommunikation
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebSocketMessage {

    /**
     * Nachrichtentyp (z.B. "authenticate", "function_call", "response")
     */
    private String type;

    /**
     * Eindeutige Request-ID für Request-Response-Mapping
     */
    private String requestId;

    /**
     * Nutzdaten der Nachricht
     */
    private Object data;

    /**
     * Zeitstempel der Nachricht
     */
    private Long timestamp;

    public WebSocketMessage(String type, String requestId, Object data) {
        this.type = type;
        this.requestId = requestId;
        this.data = data;
        this.timestamp = System.currentTimeMillis();
    }
}
