package de.mhus.nimbus.worldbridge.command.terrain;

import de.mhus.nimbus.shared.dto.worldwebsocket.WorldWorldWebSocketResponse;
import de.mhus.nimbus.shared.dto.world.MapCreateRequest;
import de.mhus.nimbus.worldbridge.command.*;
import de.mhus.nimbus.world.shared.client.TerrainServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class CreateMapCommand implements WebSocketCommand {

    private final TerrainServiceClient terrainServiceClient;

    @Override
    public WebSocketCommandInfo info() {
        return new WebSocketCommandInfo("terrain", "createMap", "Create or update map clusters");
    }

    @Override
    public ExecuteResponse execute(ExecuteRequest request) {
        try {
            MapCreateRequest mapRequest = (MapCreateRequest) request.getCommand().getData();
            terrainServiceClient.createOrUpdateMap(mapRequest);

            WorldWebSocketResponse response = WorldWebSocketResponse.builder()
                    .service(request.getCommand().getService())
                    .command(request.getCommand().getCommand())
                    .requestId(request.getCommand().getRequestId())
                    .status("success")
                    .message("Map created/updated successfully")
                    .build();

            return ExecuteResponse.success(response);

        } catch (Exception e) {
            log.error("Error creating/updating map", e);
            return ExecuteResponse.error("CREATE_MAP_ERROR", "Failed to create/update map: " + e.getMessage());
        }
    }
}
