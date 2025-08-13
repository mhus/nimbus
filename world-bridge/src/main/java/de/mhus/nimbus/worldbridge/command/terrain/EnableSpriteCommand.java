package de.mhus.nimbus.worldbridge.command.terrain;

import de.mhus.nimbus.shared.dto.worldwebsocket.WorldWorldWebSocketResponse;
import de.mhus.nimbus.shared.dto.world.SpriteDto;
import de.mhus.nimbus.worldbridge.command.*;
import de.mhus.nimbus.world.shared.client.TerrainServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class EnableSpriteCommand implements WebSocketCommand {

    private final TerrainServiceClient terrainServiceClient;

    @Override
    public WebSocketCommandInfo info() {
        return new WebSocketCommandInfo("terrain", "enableSprite", "Enable a sprite");
    }

    @Override
    public ExecuteResponse execute(ExecuteRequest request) {
        try {
            String spriteId = (String) request.getCommand().getData();
            Optional<SpriteDto> result = terrainServiceClient.enableSprite(spriteId);

            if (result.isPresent()) {
                WorldWebSocketResponse response = WorldWebSocketResponse.builder()
                        .service(request.getCommand().getService())
                        .command(request.getCommand().getCommand())
                        .requestId(request.getCommand().getRequestId())
                        .status("success")
                        .data(result.get())
                        .message("Sprite enabled successfully")
                        .build();

                return ExecuteResponse.success(response);
            } else {
                WorldWebSocketResponse response = WorldWebSocketResponse.builder()
                        .service(request.getCommand().getService())
                        .command(request.getCommand().getCommand())
                        .requestId(request.getCommand().getRequestId())
                        .status("error")
                        .errorCode("SPRITE_NOT_FOUND")
                        .message("Sprite not found or enable failed")
                        .build();

                return ExecuteResponse.success(response);
            }

        } catch (Exception e) {
            log.error("Error enabling sprite", e);
            return ExecuteResponse.error("ENABLE_SPRITE_ERROR", "Failed to enable sprite: " + e.getMessage());
        }
    }
}
