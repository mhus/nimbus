package de.mhus.nimbus.worldbridge.command.terrain;

import de.mhus.nimbus.shared.dto.worldwebsocket.WorldWorldWebSocketResponse;
import de.mhus.nimbus.worldbridge.command.*;
import de.mhus.nimbus.world.shared.client.TerrainServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class DeleteGroupCommand implements WebSocketCommand {

    private final TerrainServiceClient terrainServiceClient;

    @Override
    public WebSocketCommandInfo info() {
        return new WebSocketCommandInfo("terrain", "deleteGroup", "Delete a group by world and ID");
    }

    @Override
    public ExecuteResponse execute(ExecuteRequest request) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> params = (Map<String, Object>) request.getCommand().getData();

            String world = request.getSessionInfo().getWorldId();
            Long id = ((Number) params.get("id")).longValue();

            boolean result = terrainServiceClient.deleteGroup(world, id);

            WorldWebSocketResponse response = WorldWebSocketResponse.builder()
                    .service(request.getCommand().getService())
                    .command(request.getCommand().getCommand())
                    .requestId(request.getCommand().getRequestId())
                    .status(result ? "success" : "error")
                    .errorCode(result ? null : "DELETE_FAILED")
                    .message(result ? "Group deleted successfully" : "Failed to delete group")
                    .build();

            return ExecuteResponse.success(response);

        } catch (Exception e) {
            log.error("Error deleting group", e);
            return ExecuteResponse.error("DELETE_GROUP_ERROR", "Failed to delete group: " + e.getMessage());
        }
    }
}
