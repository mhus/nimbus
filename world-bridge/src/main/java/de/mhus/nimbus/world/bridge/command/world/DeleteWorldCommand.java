package de.mhus.nimbus.world.bridge.command.world;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.mhus.nimbus.shared.dto.command.DeleteWorldCommandData;
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
public class DeleteWorldCommand implements WebSocketCommand {

    private final TerrainServiceClient terrainServiceClient;
    private final ObjectMapper objectMapper;

    @Override
    public WebSocketCommandInfo info() {
        return new WebSocketCommandInfo("terrain", "deleteWorld", "Delete a world", true);
    }

    @Override
    public ExecuteResponse execute(ExecuteRequest request) {
        try {
            DeleteWorldCommandData data = objectMapper.convertValue(
                request.getCommand().getData(), DeleteWorldCommandData.class);

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

            boolean deleted = terrainServiceClient.deleteWorld(data.getWorldId());

            if (deleted) {
                WorldWebSocketResponse response = WorldWebSocketResponse.builder()
                        .service(request.getCommand().getService())
                        .command(request.getCommand().getCommand())
                        .requestId(request.getCommand().getRequestId())
                        .status("success")
                        .data("World deleted successfully")
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
            log.error("Error deleting world", e);
            WorldWebSocketResponse errorResponse = WorldWebSocketResponse.builder()
                    .service(request.getCommand().getService())
                    .command(request.getCommand().getCommand())
                    .requestId(request.getCommand().getRequestId())
                    .status("error")
                    .errorCode("error")
                    .message("Failed to delete world: " + e.getMessage())
                    .build();
            return ExecuteResponse.success(errorResponse);
        }
    }
}
