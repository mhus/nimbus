package de.mhus.nimbus.world.bridge.command.world;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.mhus.nimbus.shared.dto.command.CreateWorldCommandData;
import de.mhus.nimbus.shared.dto.world.WorldDto;
import de.mhus.nimbus.world.bridge.command.ExecuteRequest;
import de.mhus.nimbus.world.bridge.command.ExecuteResponse;
import de.mhus.nimbus.world.bridge.command.WebSocketCommand;
import de.mhus.nimbus.world.bridge.command.WebSocketCommandInfo;
import de.mhus.nimbus.world.shared.client.TerrainServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class CreateWorldCommand implements WebSocketCommand {

    private final TerrainServiceClient terrainServiceClient;
    private final ObjectMapper objectMapper;

    @Override
    public WebSocketCommandInfo info() {
        return new WebSocketCommandInfo("terrain", "createWorld", "Create a new world", false);
    }

    @Override
    public ExecuteResponse execute(ExecuteRequest request) {
        try {
            CreateWorldCommandData data = objectMapper.convertValue(
                request.getCommand().getData(), CreateWorldCommandData.class);

            if (data.getWorld() == null) {
                return ExecuteResponse.error("error", "World data is required");
            }

            WorldDto createdWorld = terrainServiceClient.createWorld(data.getWorld());

            return ExecuteResponse.success(createdWorld);
        } catch (Exception e) {
            log.error("Error creating world", e);
            return ExecuteResponse.error("error", "Failed to create world: " + e.getMessage());
        }
    }
}
