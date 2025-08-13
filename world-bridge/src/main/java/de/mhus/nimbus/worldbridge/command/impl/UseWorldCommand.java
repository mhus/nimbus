package de.mhus.nimbus.worldbridge.command.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.mhus.nimbus.shared.dto.worldwebsocket.UseWorldCommandData;
import de.mhus.nimbus.shared.dto.worldwebsocket.WorldWebSocketResponse;
import de.mhus.nimbus.worldbridge.command.*;
import de.mhus.nimbus.worldbridge.service.WorldService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class UseWorldCommand implements WebSocketCommand {

    private final WorldService worldService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public WebSocketCommandInfo info() {
        return new WebSocketCommandInfo("bridge", "use", "Select a world for the session");
    }

    @Override
    public ExecuteResponse execute(ExecuteRequest request) {
        try {
            UseWorldCommandData useWorldData = objectMapper.convertValue(
                request.getCommand().getData(), UseWorldCommandData.class);

            // If no worldId provided, return current world
            if (useWorldData.getWorldId() == null || useWorldData.getWorldId().isEmpty()) {
                WorldWebSocketResponse response = WorldWebSocketResponse.builder()
                        .service(request.getCommand().getService())
                        .command(request.getCommand().getCommand())
                        .requestId(request.getCommand().getRequestId())
                        .status("success")
                        .data(request.getSessionInfo().getWorldId())
                        .message("Current world ID")
                        .build();
                return ExecuteResponse.success(response);
            }

            // Validate world access
            boolean hasAccess = worldService.hasWorldAccess(
                request.getSessionInfo().getUserId(), useWorldData.getWorldId());
            if (!hasAccess) {
                WorldWebSocketResponse errorResponse = WorldWebSocketResponse.builder()
                        .service(request.getCommand().getService())
                        .command(request.getCommand().getCommand())
                        .requestId(request.getCommand().getRequestId())
                        .status("error")
                        .errorCode("NO_WORLD_ACCESS")
                        .message("No access to specified world")
                        .build();
                return ExecuteResponse.success(errorResponse);
            }

            // Get world details
            Object worldDetails = worldService.getWorldDetails(useWorldData.getWorldId());

            // Update session info and clear registrations
            request.getSessionInfo().setWorldId(useWorldData.getWorldId());
            request.getSessionInfo().clearRegistrations();

            WorldWebSocketResponse response = WorldWebSocketResponse.builder()
                    .service(request.getCommand().getService())
                    .command(request.getCommand().getCommand())
                    .requestId(request.getCommand().getRequestId())
                    .status("success")
                    .data(worldDetails)
                    .message("World selected successfully")
                    .build();

            return ExecuteResponse.success(response);

        } catch (Exception e) {
            log.error("Error during world selection", e);
            return ExecuteResponse.error("WORLD_SELECTION_ERROR", "World selection failed");
        }
    }
}
