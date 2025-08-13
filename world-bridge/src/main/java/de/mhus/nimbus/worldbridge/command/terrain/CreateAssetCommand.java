package de.mhus.nimbus.worldbridge.command.terrain;

import de.mhus.nimbus.shared.dto.worldwebsocket.WorldWorldWebSocketResponse;
import de.mhus.nimbus.shared.dto.world.AssetCreateRequest;
import de.mhus.nimbus.shared.dto.world.AssetDto;
import de.mhus.nimbus.worldbridge.command.*;
import de.mhus.nimbus.world.shared.client.TerrainServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class CreateAssetCommand implements WebSocketCommand {

    private final TerrainServiceClient terrainServiceClient;

    @Override
    public WebSocketCommandInfo info() {
        return new WebSocketCommandInfo("terrain", "createAsset", "Create a new asset");
    }

    @Override
    public ExecuteResponse execute(ExecuteRequest request) {
        try {
            AssetCreateRequest assetRequest = (AssetCreateRequest) request.getCommand().getData();
            AssetDto result = terrainServiceClient.createAsset(assetRequest);

            WorldWebSocketResponse response = WorldWebSocketResponse.builder()
                    .service(request.getCommand().getService())
                    .command(request.getCommand().getCommand())
                    .requestId(request.getCommand().getRequestId())
                    .status("success")
                    .data(result)
                    .message("Asset created successfully")
                    .build();

            return ExecuteResponse.success(response);

        } catch (Exception e) {
            log.error("Error creating asset", e);
            return ExecuteResponse.error("CREATE_ASSET_ERROR", "Failed to create asset: " + e.getMessage());
        }
    }
}
