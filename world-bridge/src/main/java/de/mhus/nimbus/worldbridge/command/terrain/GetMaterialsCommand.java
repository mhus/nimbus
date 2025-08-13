package de.mhus.nimbus.worldbridge.command.terrain;

import de.mhus.nimbus.shared.dto.worldwebsocket.WorldWorldWebSocketResponse;
import de.mhus.nimbus.shared.dto.world.MaterialDto;
import de.mhus.nimbus.worldbridge.command.*;
import de.mhus.nimbus.world.shared.client.TerrainServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class GetMaterialsCommand implements WebSocketCommand {

    private final TerrainServiceClient terrainServiceClient;

    @Override
    public WebSocketCommandInfo info() {
        return new WebSocketCommandInfo("terrain", "getMaterials", "Get paginated list of materials");
    }

    @Override
    public ExecuteResponse execute(ExecuteRequest request) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> params = (Map<String, Object>) request.getCommand().getData();

            String name = (String) params.get("name");
            Integer page = (Integer) params.getOrDefault("page", 0);
            Integer size = (Integer) params.getOrDefault("size", 20);

            Page<MaterialDto> result = terrainServiceClient.getMaterials(name, page, size);

            WorldWebSocketResponse response = WorldWebSocketResponse.builder()
                    .service(request.getCommand().getService())
                    .command(request.getCommand().getCommand())
                    .requestId(request.getCommand().getRequestId())
                    .status("success")
                    .data(result)
                    .message("Materials retrieved successfully")
                    .build();

            return ExecuteResponse.success(response);

        } catch (Exception e) {
            log.error("Error getting materials", e);
            return ExecuteResponse.error("GET_MATERIALS_ERROR", "Failed to get materials: " + e.getMessage());
        }
    }
}
