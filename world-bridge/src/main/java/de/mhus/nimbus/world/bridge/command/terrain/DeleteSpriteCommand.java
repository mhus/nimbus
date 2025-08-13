package de.mhus.nimbus.world.bridge.command.terrain;

import de.mhus.nimbus.shared.dto.worldwebsocket.WorldWebSocketResponse;
import de.mhus.nimbus.world.bridge.command.ExecuteRequest;
import de.mhus.nimbus.world.bridge.command.ExecuteResponse;
import de.mhus.nimbus.world.bridge.command.WebSocketCommand;
import de.mhus.nimbus.world.bridge.command.WebSocketCommandInfo;
import de.mhus.nimbus.world.bridge.command.*;
import de.mhus.nimbus.world.shared.client.TerrainServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DeleteSpriteCommand implements WebSocketCommand {

    private final TerrainServiceClient terrainServiceClient;

    @Override
    public WebSocketCommandInfo info() {
        return new WebSocketCommandInfo("terrain", "deleteSprite", "Delete a sprite by ID");
    }

    @Override
    public ExecuteResponse execute(ExecuteRequest request) {
        try {
            String spriteId = (String) request.getCommand().getData();
            boolean result = terrainServiceClient.deleteSprite(spriteId);

            WorldWebSocketResponse response = WorldWebSocketResponse.builder()
                    .service(request.getCommand().getService())
                    .command(request.getCommand().getCommand())
                    .requestId(request.getCommand().getRequestId())
                    .status(result ? "success" : "error")
                    .errorCode(result ? null : "DELETE_FAILED")
                    .message(result ? "Sprite deleted successfully" : "Failed to delete sprite")
                    .build();

            return ExecuteResponse.success(response);

        } catch (Exception e) {
            log.error("Error deleting sprite", e);
            return ExecuteResponse.error("DELETE_SPRITE_ERROR", "Failed to delete sprite: " + e.getMessage());
        }
    }
}
