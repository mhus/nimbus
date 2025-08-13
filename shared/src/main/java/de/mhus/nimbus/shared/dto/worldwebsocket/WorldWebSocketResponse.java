package de.mhus.nimbus.shared.dto.worldwebsocket;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import com.fasterxml.jackson.annotation.JsonProperty;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorldWebSocketResponse {
    @JsonProperty("service")
    private String service;

    @JsonProperty("command")
    private String command;

    @JsonProperty("data")
    private Object data;

    @JsonProperty("requestId")
    private String requestId;

    @JsonProperty("status")
    private String status; // "success" or "error"

    @JsonProperty("errorCode")
    private String errorCode;

    @JsonProperty("message")
    private String message;
}
