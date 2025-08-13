package de.mhus.nimbus.worldbridge.command;

import de.mhus.nimbus.shared.dto.websocket.WebSocketResponse;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Builder;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ExecuteResponse {
    private WebSocketResponse response;
    private boolean success;
    private String errorCode;
    private String message;

    public static ExecuteResponse success(WebSocketResponse response) {
        return ExecuteResponse.builder()
                .response(response)
                .success(true)
                .build();
    }

    public static ExecuteResponse error(String errorCode, String message) {
        return ExecuteResponse.builder()
                .success(false)
                .errorCode(errorCode)
                .message(message)
                .build();
    }
}
