package de.mhus.nimbus.world.bridge.command.world;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.mhus.nimbus.shared.dto.command.GetWorldCommandData;
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
public class GetWorldCommand implements WebSocketCommand {

    private final TerrainServiceClient terrainServiceClient;
    private final ObjectMapper objectMapper;

    @Override
    public WebSocketCommandInfo info() {
        return new WebSocketCommandInfo("terrain", "getWorld", "Get world by ID", true);
    }

    @Override
    public ExecuteResponse execute(ExecuteRequest request) {
        try {
            GetWorldCommandData data = objectMapper.convertValue(
                request.getCommand().getData(), GetWorldCommandData.class);

            if (data.getWorldId() == null || data.getWorldId().trim().isEmpty()) {
                return ExecuteResponse.error("error", "World ID is required");
            }

            Optional<WorldDto> world = terrainServiceClient.getWorld(data.getWorldId());

            if (world.isPresent()) {
                return ExecuteResponse.success(world.get());
            } else {
                return ExecuteResponse.error("not_found", "World not found");
            }
        } catch (Exception e) {
            log.error("Error getting world", e);
            return ExecuteResponse.error("error", "Failed to get world: " + e.getMessage());
        }
    }
}
