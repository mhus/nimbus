package de.mhus.nimbus.worldbridge.command.terrain;

import de.mhus.nimbus.shared.dto.worldwebsocket.WorldWorldWebSocketResponse;
import de.mhus.nimbus.shared.dto.world.GroupDto;
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
public class GetGroupsCommand implements WebSocketCommand {

    private final TerrainServiceClient terrainServiceClient;

    @Override
    public WebSocketCommandInfo info() {
        return new WebSocketCommandInfo("terrain", "getGroups", "Get paginated list of groups");
    }

    @Override
    public ExecuteResponse execute(ExecuteRequest request) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> params = (Map<String, Object>) request.getCommand().getData();

            String world = request.getSessionInfo().getWorldId();
            String type = (String) params.get("type");
            Integer page = (Integer) params.getOrDefault("page", 0);
            Integer size = (Integer) params.getOrDefault("size", 20);

            Page<GroupDto> result = terrainServiceClient.getGroups(world, type, page, size);

            WorldWebSocketResponse response = WorldWebSocketResponse.builder()
                    .service(request.getCommand().getService())
                    .command(request.getCommand().getCommand())
                    .requestId(request.getCommand().getRequestId())
                    .status("success")
                    .data(result)
                    .message("Groups retrieved successfully")
                    .build();

            return ExecuteResponse.success(response);

        } catch (Exception e) {
            log.error("Error getting groups", e);
            return ExecuteResponse.error("GET_GROUPS_ERROR", "Failed to get groups: " + e.getMessage());
        }
    }
}
