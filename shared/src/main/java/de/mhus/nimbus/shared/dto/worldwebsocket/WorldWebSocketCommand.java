package de.mhus.nimbus.shared.dto.worldwebsocket;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import com.fasterxml.jackson.annotation.JsonProperty;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WorldWebSocketCommand {
    @JsonProperty("service")
    private String service;

    @JsonProperty("command")
    private String command;

    @JsonProperty("data")
    private Object data;

    @JsonProperty("requestId")
    private String requestId;
}
