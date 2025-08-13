package de.mhus.nimbus.worldbridge.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.mhus.nimbus.shared.dto.websocket.*;
import de.mhus.nimbus.worldbridge.model.WebSocketSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class WorldBridgeService {

    private final AuthenticationService authenticationService;
    private final WorldService worldService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public WebSocketResponse processCommand(String sessionId, WebSocketSession sessionInfo, WebSocketCommand command) {
        try {
            return switch (command.getCommand()) {
                case "login" -> handleLogin(sessionInfo, command);
                case "use" -> handleUseWorld(sessionInfo, command);
                case "ping" -> handlePing(command);
                case "registerCluster" -> handleRegisterCluster(sessionInfo, command);
                case "registerTerrain" -> handleRegisterTerrain(sessionInfo, command);
                default -> WebSocketResponse.builder()
                        .service(command.getService())
                        .command(command.getCommand())
                        .requestId(command.getRequestId())
                        .status("error")
                        .errorCode("UNKNOWN_COMMAND")
                        .message("Unknown command: " + command.getCommand())
                        .build();
            };
        } catch (Exception e) {
            log.error("Error processing command: {}", command.getCommand(), e);
            return WebSocketResponse.builder()
                    .service(command.getService())
                    .command(command.getCommand())
                    .requestId(command.getRequestId())
                    .status("error")
                    .errorCode("INTERNAL_ERROR")
                    .message("Internal server error")
                    .build();
        }
    }

    private WebSocketResponse handleLogin(WebSocketSession sessionInfo, WebSocketCommand command) {
        try {
            LoginCommandData loginData = objectMapper.convertValue(command.getData(), LoginCommandData.class);

            // Validate token and get user information
            AuthenticationResult authResult = authenticationService.validateToken(loginData.getToken());

            if (!authResult.isValid()) {
                return WebSocketResponse.builder()
                        .service(command.getService())
                        .command(command.getCommand())
                        .requestId(command.getRequestId())
                        .status("error")
                        .errorCode("INVALID_TOKEN")
                        .message("Invalid authentication token")
                        .build();
            }

            // Update session info
            sessionInfo.setUserId(authResult.getUserId());
            sessionInfo.setRoles(authResult.getRoles());

            return WebSocketResponse.builder()
                    .service(command.getService())
                    .command(command.getCommand())
                    .requestId(command.getRequestId())
                    .status("success")
                    .data(authResult)
                    .message("Login successful")
                    .build();

        } catch (Exception e) {
            log.error("Error during login", e);
            return WebSocketResponse.builder()
                    .service(command.getService())
                    .command(command.getCommand())
                    .requestId(command.getRequestId())
                    .status("error")
                    .errorCode("LOGIN_ERROR")
                    .message("Login failed")
                    .build();
        }
    }

    private WebSocketResponse handleUseWorld(WebSocketSession sessionInfo, WebSocketCommand command) {
        try {
            UseWorldCommandData useWorldData = objectMapper.convertValue(command.getData(), UseWorldCommandData.class);

            // If no worldId provided, return current world
            if (useWorldData.getWorldId() == null || useWorldData.getWorldId().isEmpty()) {
                return WebSocketResponse.builder()
                        .service(command.getService())
                        .command(command.getCommand())
                        .requestId(command.getRequestId())
                        .status("success")
                        .data(sessionInfo.getWorldId())
                        .message("Current world ID")
                        .build();
            }

            // Validate world access
            boolean hasAccess = worldService.hasWorldAccess(sessionInfo.getUserId(), useWorldData.getWorldId());
            if (!hasAccess) {
                return WebSocketResponse.builder()
                        .service(command.getService())
                        .command(command.getCommand())
                        .requestId(command.getRequestId())
                        .status("error")
                        .errorCode("NO_WORLD_ACCESS")
                        .message("No access to specified world")
                        .build();
            }

            // Get world details
            Object worldDetails = worldService.getWorldDetails(useWorldData.getWorldId());

            // Update session info and clear registrations
            sessionInfo.setWorldId(useWorldData.getWorldId());
            sessionInfo.clearRegistrations();

            return WebSocketResponse.builder()
                    .service(command.getService())
                    .command(command.getCommand())
                    .requestId(command.getRequestId())
                    .status("success")
                    .data(worldDetails)
                    .message("World selected successfully")
                    .build();

        } catch (Exception e) {
            log.error("Error during world selection", e);
            return WebSocketResponse.builder()
                    .service(command.getService())
                    .command(command.getCommand())
                    .requestId(command.getRequestId())
                    .status("error")
                    .errorCode("WORLD_SELECTION_ERROR")
                    .message("World selection failed")
                    .build();
        }
    }

    private WebSocketResponse handlePing(WebSocketCommand command) {
        PingCommandData pingData = objectMapper.convertValue(command.getData(), PingCommandData.class);

        return WebSocketResponse.builder()
                .service(command.getService())
                .command("pong")
                .requestId(command.getRequestId())
                .status("success")
                .data(pingData)
                .message("Pong")
                .build();
    }

    private WebSocketResponse handleRegisterCluster(WebSocketSession sessionInfo, WebSocketCommand command) {
        try {
            RegisterClusterCommandData clusterData = objectMapper.convertValue(command.getData(), RegisterClusterCommandData.class);

            // Clear existing registrations and add new ones
            sessionInfo.getRegisteredClusters().clear();
            sessionInfo.getRegisteredClusters().addAll(clusterData.getClusters());

            return WebSocketResponse.builder()
                    .service(command.getService())
                    .command(command.getCommand())
                    .requestId(command.getRequestId())
                    .status("success")
                    .data(clusterData)
                    .message("Cluster registration successful")
                    .build();

        } catch (Exception e) {
            log.error("Error during cluster registration", e);
            return WebSocketResponse.builder()
                    .service(command.getService())
                    .command(command.getCommand())
                    .requestId(command.getRequestId())
                    .status("error")
                    .errorCode("CLUSTER_REGISTRATION_ERROR")
                    .message("Cluster registration failed")
                    .build();
        }
    }

    private WebSocketResponse handleRegisterTerrain(WebSocketSession sessionInfo, WebSocketCommand command) {
        try {
            RegisterTerrainCommandData terrainData = objectMapper.convertValue(command.getData(), RegisterTerrainCommandData.class);

            // Clear existing registrations and add new ones
            sessionInfo.getRegisteredTerrainEvents().clear();
            sessionInfo.getRegisteredTerrainEvents().addAll(terrainData.getEvents());

            return WebSocketResponse.builder()
                    .service(command.getService())
                    .command(command.getCommand())
                    .requestId(command.getRequestId())
                    .status("success")
                    .data(terrainData)
                    .message("Terrain event registration successful")
                    .build();

        } catch (Exception e) {
            log.error("Error during terrain registration", e);
            return WebSocketResponse.builder()
                    .service(command.getService())
                    .command(command.getCommand())
                    .requestId(command.getRequestId())
                    .status("error")
                    .errorCode("TERRAIN_REGISTRATION_ERROR")
                    .message("Terrain event registration failed")
                    .build();
        }
    }
}
