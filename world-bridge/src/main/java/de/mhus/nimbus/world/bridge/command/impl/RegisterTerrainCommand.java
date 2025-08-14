package de.mhus.nimbus.world.bridge.command.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.mhus.nimbus.shared.dto.worldwebsocket.RegisterTerrainCommandData;
import de.mhus.nimbus.shared.dto.worldwebsocket.WorldWebSocketResponse;
import de.mhus.nimbus.world.bridge.command.ExecuteRequest;
import de.mhus.nimbus.world.bridge.command.ExecuteResponse;
import de.mhus.nimbus.world.bridge.command.WebSocketCommand;
import de.mhus.nimbus.world.bridge.command.WebSocketCommandInfo;
import de.mhus.nimbus.world.bridge.command.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class RegisterTerrainCommand implements WebSocketCommand {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public WebSocketCommandInfo info() {
        return new WebSocketCommandInfo("bridge", "registerTerrain", "Register for terrain-specific events", true);
    }

    @Override
    public ExecuteResponse execute(ExecuteRequest request) {
        try {
            RegisterTerrainCommandData terrainData = objectMapper.convertValue(
                request.getCommand().getData(), RegisterTerrainCommandData.class);

            // Clear existing registrations and add new ones
            request.getSessionInfo().getRegisteredTerrainEvents().clear();
            request.getSessionInfo().getRegisteredTerrainEvents().addAll(terrainData.getEvents());

            WorldWebSocketResponse response = WorldWebSocketResponse.builder()
                    .service(request.getCommand().getService())
                    .command(request.getCommand().getCommand())
                    .requestId(request.getCommand().getRequestId())
                    .status("success")
                    .data(terrainData)
                    .message("Terrain event registration successful")
                    .build();

            return ExecuteResponse.success(response);

        } catch (Exception e) {
            log.error("Error during terrain registration", e);
            return ExecuteResponse.error("TERRAIN_REGISTRATION_ERROR", "Terrain event registration failed");
        }
    }
}
