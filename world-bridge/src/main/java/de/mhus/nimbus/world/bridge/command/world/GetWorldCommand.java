package de.mhus.nimbus.world.bridge.command.world;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.mhus.nimbus.shared.dto.command.GetWorldCommandData;
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

import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class GetWorldCommand implements WebSocketCommand {

    private final TerrainServiceClient terrainServiceClient;
    private final ObjectMapper objectMapper;

    @Override
    public WebSocketCommandInfo info() {
        return new WebSocketCommandInfo("terrain", "getWorld", "Get world by ID", true);
    }

    @Override
    public ExecuteResponse execute(ExecuteRequest request) {
        try {
            GetWorldCommandData data = objectMapper.convertValue(
                request.getCommand().getData(), GetWorldCommandData.class);

            if (data.getWorldId() == null || data.getWorldId().trim().isEmpty()) {
                WorldWebSocketResponse errorResponse = WorldWebSocketResponse.builder()
                        .service(request.getCommand().getService())
                        .command(request.getCommand().getCommand())
                        .requestId(request.getCommand().getRequestId())
                        .status("error")
                        .errorCode("error")
                        .message("World ID is required")
                        .build();
                return ExecuteResponse.success(errorResponse);
            }

            Optional<WorldDto> world = terrainServiceClient.getWorld(data.getWorldId());

            if (world.isPresent()) {
                WorldWebSocketResponse response = WorldWebSocketResponse.builder()
                        .service(request.getCommand().getService())
                        .command(request.getCommand().getCommand())
                        .requestId(request.getCommand().getRequestId())
                        .status("success")
                        .data(world.get())
                        .build();
                return ExecuteResponse.success(response);
            } else {
                WorldWebSocketResponse errorResponse = WorldWebSocketResponse.builder()
                        .service(request.getCommand().getService())
                        .command(request.getCommand().getCommand())
                        .requestId(request.getCommand().getRequestId())
                        .status("error")
                        .errorCode("not_found")
                        .message("World not found")
                        .build();
                return ExecuteResponse.success(errorResponse);
            }
        } catch (Exception e) {
            log.error("Error getting world", e);
            WorldWebSocketResponse errorResponse = WorldWebSocketResponse.builder()
                    .service(request.getCommand().getService())
                    .command(request.getCommand().getCommand())
                    .requestId(request.getCommand().getRequestId())
                    .status("error")
                    .errorCode("error")
                    .message("Failed to get world: " + e.getMessage())
                    .build();
            return ExecuteResponse.success(errorResponse);
        }
    }
}
