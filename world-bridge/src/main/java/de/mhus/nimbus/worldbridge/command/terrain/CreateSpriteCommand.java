package de.mhus.nimbus.worldbridge.command.terrain;

import de.mhus.nimbus.shared.dto.worldwebsocket.WorldWebSocketResponse;
import de.mhus.nimbus.shared.dto.world.SpriteCreateRequest;
import de.mhus.nimbus.shared.dto.world.SpriteDto;
import de.mhus.nimbus.worldbridge.command.*;
import de.mhus.nimbus.world.shared.client.TerrainServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class CreateSpriteCommand implements WebSocketCommand {

    private final TerrainServiceClient terrainServiceClient;

    @Override
    public WebSocketCommandInfo info() {
        return new WebSocketCommandInfo("terrain", "createSprite", "Create a new sprite");
    }

    @Override
    public ExecuteResponse execute(ExecuteRequest request) {
        try {
            SpriteCreateRequest spriteRequest = (SpriteCreateRequest) request.getCommand().getData();
            SpriteDto result = terrainServiceClient.createSprite(spriteRequest);

            WorldWebSocketResponse response = WorldWebSocketResponse.builder()
                    .service(request.getCommand().getService())
                    .command(request.getCommand().getCommand())
                    .requestId(request.getCommand().getRequestId())
                    .status("success")
                    .data(result)
                    .message("Sprite created successfully")
                    .build();

            return ExecuteResponse.success(response);

        } catch (Exception e) {
            log.error("Error creating sprite", e);
            return ExecuteResponse.error("CREATE_SPRITE_ERROR", "Failed to create sprite: " + e.getMessage());
        }
    }
}
