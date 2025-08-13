package de.mhus.nimbus.worldbridge.command.terrain;

import de.mhus.nimbus.shared.dto.worldwebsocket.WorldWorldWebSocketResponse;
import de.mhus.nimbus.shared.dto.world.GroupCreateRequest;
import de.mhus.nimbus.shared.dto.world.GroupDto;
import de.mhus.nimbus.worldbridge.command.*;
import de.mhus.nimbus.world.shared.client.TerrainServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class CreateGroupCommand implements WebSocketCommand {

    private final TerrainServiceClient terrainServiceClient;

    @Override
    public WebSocketCommandInfo info() {
        return new WebSocketCommandInfo("terrain", "createGroup", "Create a new terrain group");
    }

    @Override
    public ExecuteResponse execute(ExecuteRequest request) {
        try {
            GroupCreateRequest groupRequest = (GroupCreateRequest) request.getCommand().getData();
            GroupDto result = terrainServiceClient.createGroup(groupRequest);

            WorldWebSocketResponse response = WorldWebSocketResponse.builder()
                    .service(request.getCommand().getService())
                    .command(request.getCommand().getCommand())
                    .requestId(request.getCommand().getRequestId())
                    .status("success")
                    .data(result)
                    .message("Group created successfully")
                    .build();

            return ExecuteResponse.success(response);

        } catch (Exception e) {
            log.error("Error creating group", e);
            return ExecuteResponse.error("CREATE_GROUP_ERROR", "Failed to create group: " + e.getMessage());
        }
    }
}
