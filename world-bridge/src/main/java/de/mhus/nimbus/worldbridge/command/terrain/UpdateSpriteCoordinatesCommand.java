package de.mhus.nimbus.worldbridge.command.terrain;

import de.mhus.nimbus.shared.dto.worldwebsocket.WorldWorldWebSocketResponse;
import de.mhus.nimbus.shared.dto.world.SpriteCoordinatesDto;
import de.mhus.nimbus.shared.dto.world.SpriteDto;
import de.mhus.nimbus.worldbridge.command.*;
import de.mhus.nimbus.world.shared.client.TerrainServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class UpdateSpriteCoordinatesCommand implements WebSocketCommand {

    private final TerrainServiceClient terrainServiceClient;

    @Override
    public WebSocketCommandInfo info() {
        return new WebSocketCommandInfo("terrain", "updateSpriteCoordinates", "Update sprite coordinates");
    }

    @Override
    public ExecuteResponse execute(ExecuteRequest request) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> params = (Map<String, Object>) request.getCommand().getData();

            String spriteId = (String) params.get("id");
            SpriteCoordinatesDto coordinates = (SpriteCoordinatesDto) params.get("coordinates");

            Optional<SpriteDto> result = terrainServiceClient.updateSpriteCoordinates(spriteId, coordinates);

            if (result.isPresent()) {
                WorldWebSocketResponse response = WorldWebSocketResponse.builder()
                        .service(request.getCommand().getService())
                        .command(request.getCommand().getCommand())
                        .requestId(request.getCommand().getRequestId())
                        .status("success")
                        .data(result.get())
                        .message("Sprite coordinates updated successfully")
                        .build();

                return ExecuteResponse.success(response);
            } else {
                WorldWebSocketResponse response = WorldWebSocketResponse.builder()
                        .service(request.getCommand().getService())
                        .command(request.getCommand().getCommand())
                        .requestId(request.getCommand().getRequestId())
                        .status("error")
                        .errorCode("SPRITE_NOT_FOUND")
                        .message("Sprite not found or coordinate update failed")
                        .build();

                return ExecuteResponse.success(response);
            }

        } catch (Exception e) {
            log.error("Error updating sprite coordinates", e);
            return ExecuteResponse.error("UPDATE_SPRITE_COORDINATES_ERROR", "Failed to update sprite coordinates: " + e.getMessage());
        }
    }
}
