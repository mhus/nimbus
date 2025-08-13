package de.mhus.nimbus.worldbridge.command.terrain;

import de.mhus.nimbus.shared.dto.worldwebsocket.WorldWorldWebSocketResponse;
import de.mhus.nimbus.shared.dto.world.MaterialDto;
import de.mhus.nimbus.worldbridge.command.*;
import de.mhus.nimbus.world.shared.client.TerrainServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class GetMaterialCommand implements WebSocketCommand {

    private final TerrainServiceClient terrainServiceClient;

    @Override
    public WebSocketCommandInfo info() {
        return new WebSocketCommandInfo("terrain", "getMaterial", "Get material by ID");
    }

    @Override
    public ExecuteResponse execute(ExecuteRequest request) {
        try {
            Integer materialId = (Integer) request.getCommand().getData();
            Optional<MaterialDto> result = terrainServiceClient.getMaterial(materialId);

            if (result.isPresent()) {
                WorldWebSocketResponse response = WorldWebSocketResponse.builder()
                        .service(request.getCommand().getService())
                        .command(request.getCommand().getCommand())
                        .requestId(request.getCommand().getRequestId())
                        .status("success")
                        .data(result.get())
                        .message("Material retrieved successfully")
                        .build();

                return ExecuteResponse.success(response);
            } else {
                WorldWebSocketResponse response = WorldWebSocketResponse.builder()
                        .service(request.getCommand().getService())
                        .command(request.getCommand().getCommand())
                        .requestId(request.getCommand().getRequestId())
                        .status("error")
                        .errorCode("MATERIAL_NOT_FOUND")
                        .message("Material not found")
                        .build();

                return ExecuteResponse.success(response);
            }

        } catch (Exception e) {
            log.error("Error getting material", e);
            return ExecuteResponse.error("GET_MATERIAL_ERROR", "Failed to get material: " + e.getMessage());
        }
    }
}
