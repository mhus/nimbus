package de.mhus.nimbus.world.bridge.command.world;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.mhus.nimbus.shared.dto.command.UpdateWorldCommandData;
import de.mhus.nimbus.shared.dto.world.WorldDto;
import de.mhus.nimbus.world.bridge.command.ExecuteRequest;
import de.mhus.nimbus.world.bridge.command.ExecuteResponse;
import de.mhus.nimbus.world.bridge.command.WebSocketCommand;
import de.mhus.nimbus.world.bridge.command.WebSocketCommandInfo;
import de.mhus.nimbus.world.shared.client.TerrainServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class UpdateWorldCommand implements WebSocketCommand {

    private final TerrainServiceClient terrainServiceClient;
    private final ObjectMapper objectMapper;

    @Override
    public WebSocketCommandInfo info() {
        return new WebSocketCommandInfo("terrain", "updateWorld", "Update an existing world", true);
    }

    @Override
    public ExecuteResponse execute(ExecuteRequest request) {
        try {
            UpdateWorldCommandData data = objectMapper.convertValue(
                request.getCommand().getData(), UpdateWorldCommandData.class);

            if (data.getWorldId() == null || data.getWorldId().trim().isEmpty()) {
                return ExecuteResponse.error("error", "World ID is required");
            }

            if (data.getWorld() == null) {
                return ExecuteResponse.error("error", "World data is required");
            }

            Optional<WorldDto> updatedWorld = terrainServiceClient.updateWorld(data.getWorldId(), data.getWorld());

            if (updatedWorld.isPresent()) {
                return ExecuteResponse.success(updatedWorld.get());
            } else {
                return ExecuteResponse.error("not_found", "World not found");
            }
        } catch (Exception e) {
            log.error("Error updating world", e);
            return ExecuteResponse.error("error", "Failed to update world: " + e.getMessage());
        }
    }
}
