package de.mhus.nimbus.world.bridge.command.world;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.mhus.nimbus.shared.dto.command.CreateWorldCommandData;
import de.mhus.nimbus.shared.dto.world.WorldDto;
import de.mhus.nimbus.shared.dto.worldwebsocket.WorldWebSocketResponse;
import de.mhus.nimbus.world.bridge.command.ExecuteRequest;
import de.mhus.nimbus.world.bridge.command.ExecuteResponse;
import de.mhus.nimbus.world.bridge.command.WebSocketCommand;
import de.mhus.nimbus.world.bridge.command.WebSocketCommandInfo;
import de.mhus.nimbus.world.shared.client.TerrainServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class CreateWorldCommand implements WebSocketCommand {

    private final TerrainServiceClient terrainServiceClient;
    private final ObjectMapper objectMapper;

    @Override
    public WebSocketCommandInfo info() {
        return new WebSocketCommandInfo("terrain", "createWorld", "Create a new world", false);
    }

    @Override
    public ExecuteResponse execute(ExecuteRequest request) {
        try {
            CreateWorldCommandData data = objectMapper.convertValue(
                request.getCommand().getData(), CreateWorldCommandData.class);

            if (data.getWorld() == null) {
                WorldWebSocketResponse errorResponse = WorldWebSocketResponse.builder()
                        .service(request.getCommand().getService())
                        .command(request.getCommand().getCommand())
                        .requestId(request.getCommand().getRequestId())
                        .status("error")
                        .errorCode("error")
                        .message("World data is required")
                        .build();
                return ExecuteResponse.success(errorResponse);
            }

            WorldDto createdWorld = terrainServiceClient.createWorld(data.getWorld());

            WorldWebSocketResponse response = WorldWebSocketResponse.builder()
                    .service(request.getCommand().getService())
                    .command(request.getCommand().getCommand())
                    .requestId(request.getCommand().getRequestId())
                    .status("success")
                    .data(createdWorld)
                    .build();

            return ExecuteResponse.success(response);
        } catch (Exception e) {
            log.error("Error creating world", e);
            WorldWebSocketResponse errorResponse = WorldWebSocketResponse.builder()
                    .service(request.getCommand().getService())
                    .command(request.getCommand().getCommand())
                    .requestId(request.getCommand().getRequestId())
                    .status("error")
                    .errorCode("error")
                    .message("Failed to create world: " + e.getMessage())
                    .build();
            return ExecuteResponse.success(errorResponse);
        }
    }
}
