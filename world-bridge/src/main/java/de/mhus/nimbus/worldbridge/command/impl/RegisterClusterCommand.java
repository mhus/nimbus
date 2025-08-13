package de.mhus.nimbus.worldbridge.command.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.mhus.nimbus.shared.dto.websocket.RegisterClusterCommandData;
import de.mhus.nimbus.shared.dto.websocket.WebSocketResponse;
import de.mhus.nimbus.worldbridge.command.*;
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

            WebSocketResponse response = WebSocketResponse.builder()
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
