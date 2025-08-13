package de.mhus.nimbus.worldbridge.command.terrain;

import de.mhus.nimbus.shared.dto.worldwebsocket.WorldWorldWebSocketResponse;
import de.mhus.nimbus.shared.dto.world.MapBatchRequest;
import de.mhus.nimbus.shared.dto.world.TerrainClusterDto;
import de.mhus.nimbus.worldbridge.command.*;
import de.mhus.nimbus.world.shared.client.TerrainServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class GetMapClustersCommand implements WebSocketCommand {

    private final TerrainServiceClient terrainServiceClient;

    @Override
    public WebSocketCommandInfo info() {
        return new WebSocketCommandInfo("terrain", "getMapClusters", "Get multiple map clusters in batch");
    }

    @Override
    public ExecuteResponse execute(ExecuteRequest request) {
        try {
            MapBatchRequest batchRequest = (MapBatchRequest) request.getCommand().getData();
            List<TerrainClusterDto> result = terrainServiceClient.getMapClusters(batchRequest);

            WorldWebSocketResponse response = WorldWebSocketResponse.builder()
                    .service(request.getCommand().getService())
                    .command(request.getCommand().getCommand())
                    .requestId(request.getCommand().getRequestId())
                    .status("success")
                    .data(result)
                    .message("Map clusters retrieved successfully")
                    .build();

            return ExecuteResponse.success(response);

        } catch (Exception e) {
            log.error("Error getting map clusters", e);
            return ExecuteResponse.error("GET_MAP_CLUSTERS_ERROR", "Failed to get map clusters: " + e.getMessage());
        }
    }
}
