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
public class DeleteMaterialCommand implements WebSocketCommand {

    private final TerrainServiceClient terrainServiceClient;

    @Override
    public WebSocketCommandInfo info() {
        return new WebSocketCommandInfo("terrain", "deleteMaterial", "Delete a material by ID");
    }

    @Override
    public ExecuteResponse execute(ExecuteRequest request) {
        try {
            Integer materialId = (Integer) request.getCommand().getData();
            boolean result = terrainServiceClient.deleteMaterial(materialId);

            WorldWebSocketResponse response = WorldWebSocketResponse.builder()
                    .service(request.getCommand().getService())
                    .command(request.getCommand().getCommand())
                    .requestId(request.getCommand().getRequestId())
                    .status(result ? "success" : "error")
                    .errorCode(result ? null : "DELETE_FAILED")
                    .message(result ? "Material deleted successfully" : "Failed to delete material")
                    .build();

            return ExecuteResponse.success(response);

        } catch (Exception e) {
            log.error("Error deleting material", e);
            return ExecuteResponse.error("DELETE_MATERIAL_ERROR", "Failed to delete material: " + e.getMessage());
        }
    }
}
