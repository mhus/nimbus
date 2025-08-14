package de.mhus.nimbus.world.bridge.command.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.mhus.nimbus.shared.dto.worldwebsocket.PingCommandData;
import de.mhus.nimbus.shared.dto.worldwebsocket.WorldWebSocketResponse;
import de.mhus.nimbus.world.bridge.command.ExecuteRequest;
import de.mhus.nimbus.world.bridge.command.ExecuteResponse;
import de.mhus.nimbus.world.bridge.command.WebSocketCommand;
import de.mhus.nimbus.world.bridge.command.WebSocketCommandInfo;
import de.mhus.nimbus.world.bridge.command.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PingCommand implements WebSocketCommand {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public WebSocketCommandInfo info() {
        return new WebSocketCommandInfo("bridge", "ping", "Test connection with pong response", false);
    }

    @Override
    public ExecuteResponse execute(ExecuteRequest request) {
        PingCommandData pingData = objectMapper.convertValue(
            request.getCommand().getData(), PingCommandData.class);

        WorldWebSocketResponse response = WorldWebSocketResponse.builder()
                .service(request.getCommand().getService())
                .command("pong")
                .requestId(request.getCommand().getRequestId())
                .status("success")
                .data(pingData)
                .message("Pong")
                .build();

        return ExecuteResponse.success(response);
    }
}
