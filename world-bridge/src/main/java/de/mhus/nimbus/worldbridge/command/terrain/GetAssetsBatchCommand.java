package de.mhus.nimbus.worldbridge.command.terrain;

import de.mhus.nimbus.shared.dto.worldwebsocket.WorldWebSocketResponse;
import de.mhus.nimbus.shared.dto.world.AssetBatchRequest;
import de.mhus.nimbus.shared.dto.world.AssetDto;
import de.mhus.nimbus.worldbridge.command.*;
import de.mhus.nimbus.world.shared.client.TerrainServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class GetAssetsBatchCommand implements WebSocketCommand {

    private final TerrainServiceClient terrainServiceClient;

    @Override
    public WebSocketCommandInfo info() {
        return new WebSocketCommandInfo("terrain", "getAssetsBatch", "Get multiple assets in batch");
    }

    @Override
    public ExecuteResponse execute(ExecuteRequest request) {
        try {
            AssetBatchRequest batchRequest = (AssetBatchRequest) request.getCommand().getData();
            List<AssetDto> result = terrainServiceClient.getAssetsBatch(batchRequest);

            WorldWebSocketResponse response = WorldWebSocketResponse.builder()
                    .service(request.getCommand().getService())
                    .command(request.getCommand().getCommand())
                    .requestId(request.getCommand().getRequestId())
                    .status("success")
                    .data(result)
                    .message("Assets retrieved successfully")
                    .build();

            return ExecuteResponse.success(response);

        } catch (Exception e) {
            log.error("Error getting assets batch", e);
            return ExecuteResponse.error("GET_ASSETS_BATCH_ERROR", "Failed to get assets batch: " + e.getMessage());
        }
    }
}
