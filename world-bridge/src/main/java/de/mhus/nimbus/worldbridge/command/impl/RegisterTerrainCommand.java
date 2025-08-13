package de.mhus.nimbus.worldbridge.command.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.mhus.nimbus.shared.dto.websocket.RegisterTerrainCommandData;
import de.mhus.nimbus.shared.dto.websocket.WebSocketResponse;
import de.mhus.nimbus.worldbridge.command.*;
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
        return new WebSocketCommandInfo("bridge", "registerTerrain", "Register for terrain-specific events");
    }

    @Override
    public ExecuteResponse execute(ExecuteRequest request) {
        try {
            RegisterTerrainCommandData terrainData = objectMapper.convertValue(
                request.getCommand().getData(), RegisterTerrainCommandData.class);

            // Clear existing registrations and add new ones
            request.getSessionInfo().getRegisteredTerrainEvents().clear();
            request.getSessionInfo().getRegisteredTerrainEvents().addAll(terrainData.getEvents());

            WebSocketResponse response = WebSocketResponse.builder()
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
