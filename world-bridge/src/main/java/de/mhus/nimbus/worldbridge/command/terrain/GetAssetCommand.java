package de.mhus.nimbus.worldbridge.command.terrain;

import de.mhus.nimbus.shared.dto.worldwebsocket.WorldWorldWebSocketResponse;
import de.mhus.nimbus.shared.dto.world.AssetDto;
import de.mhus.nimbus.worldbridge.command.*;
import de.mhus.nimbus.world.shared.client.TerrainServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class GetAssetCommand implements WebSocketCommand {

    private final TerrainServiceClient terrainServiceClient;

    @Override
    public WebSocketCommandInfo info() {
        return new WebSocketCommandInfo("terrain", "getAsset", "Get asset by world and name");
    }

    @Override
    public ExecuteResponse execute(ExecuteRequest request) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> params = (Map<String, Object>) request.getCommand().getData();

            String world = request.getSessionInfo().getWorldId();
            String name = (String) params.get("name");

            Optional<AssetDto> result = terrainServiceClient.getAsset(world, name);

            if (result.isPresent()) {
                WorldWebSocketResponse response = WorldWebSocketResponse.builder()
                        .service(request.getCommand().getService())
                        .command(request.getCommand().getCommand())
                        .requestId(request.getCommand().getRequestId())
                        .status("success")
                        .data(result.get())
                        .message("Asset retrieved successfully")
                        .build();

                return ExecuteResponse.success(response);
            } else {
                WorldWebSocketResponse response = WorldWebSocketResponse.builder()
                        .service(request.getCommand().getService())
                        .command(request.getCommand().getCommand())
                        .requestId(request.getCommand().getRequestId())
                        .status("error")
                        .errorCode("ASSET_NOT_FOUND")
                        .message("Asset not found")
                        .build();

                return ExecuteResponse.success(response);
            }

        } catch (Exception e) {
            log.error("Error getting asset", e);
            return ExecuteResponse.error("GET_ASSET_ERROR", "Failed to get asset: " + e.getMessage());
        }
    }
}
