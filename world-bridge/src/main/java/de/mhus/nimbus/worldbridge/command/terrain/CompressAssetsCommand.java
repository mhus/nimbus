package de.mhus.nimbus.worldbridge.command.terrain;

import de.mhus.nimbus.shared.dto.worldwebsocket.WorldWorldWebSocketResponse;
import de.mhus.nimbus.worldbridge.command.*;
import de.mhus.nimbus.world.shared.client.TerrainServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class CompressAssetsCommand implements WebSocketCommand {

    private final TerrainServiceClient terrainServiceClient;

    @Override
    public WebSocketCommandInfo info() {
        return new WebSocketCommandInfo("terrain", "compressAssets", "Compress all assets in the world");
    }

    @Override
    public ExecuteResponse execute(ExecuteRequest request) {
        try {
            String world = request.getSessionInfo().getWorldId();
            terrainServiceClient.compressAssets(world);

            WorldWebSocketResponse response = WorldWebSocketResponse.builder()
                    .service(request.getCommand().getService())
                    .command(request.getCommand().getCommand())
                    .requestId(request.getCommand().getRequestId())
                    .status("success")
                    .message("Assets compression started successfully")
                    .build();

            return ExecuteResponse.success(response);

        } catch (Exception e) {
            log.error("Error compressing assets", e);
            return ExecuteResponse.error("COMPRESS_ASSETS_ERROR", "Failed to compress assets: " + e.getMessage());
        }
    }
}
