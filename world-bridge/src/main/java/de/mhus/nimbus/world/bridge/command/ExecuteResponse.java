package de.mhus.nimbus.world.bridge.command;

import de.mhus.nimbus.shared.dto.worldwebsocket.WorldWebSocketResponse;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Builder;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ExecuteResponse {
    private WorldWebSocketResponse response;
    private boolean success;
    private String errorCode;
    private String message;

    public static ExecuteResponse success(WorldWebSocketResponse response) {
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
