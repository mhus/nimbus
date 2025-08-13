package de.mhus.nimbus.world.bridge.command.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.mhus.nimbus.shared.dto.worldwebsocket.RegisterClusterCommandData;
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
public class RegisterClusterCommand implements WebSocketCommand {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public WebSocketCommandInfo info() {
        return new WebSocketCommandInfo("bridge", "registerCluster", "Register for cluster-specific events");
    }

    @Override
    public ExecuteResponse execute(ExecuteRequest request) {
        try {
            RegisterClusterCommandData clusterData = objectMapper.convertValue(
                request.getCommand().getData(), RegisterClusterCommandData.class);

            // Clear existing registrations and add new ones
            request.getSessionInfo().getRegisteredClusters().clear();
            request.getSessionInfo().getRegisteredClusters().addAll(clusterData.getClusters());

            WorldWebSocketResponse response = WorldWebSocketResponse.builder()
                    .service(request.getCommand().getService())
                    .command(request.getCommand().getCommand())
                    .requestId(request.getCommand().getRequestId())
                    .status("success")
                    .data(clusterData)
                    .message("Cluster registration successful")
                    .build();

            return ExecuteResponse.success(response);

        } catch (Exception e) {
            log.error("Error during cluster registration", e);
            return ExecuteResponse.error("CLUSTER_REGISTRATION_ERROR", "Cluster registration failed");
        }
    }
}
