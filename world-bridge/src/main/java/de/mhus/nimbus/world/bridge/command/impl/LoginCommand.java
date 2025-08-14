package de.mhus.nimbus.world.bridge.command.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.mhus.nimbus.shared.dto.worldwebsocket.LoginCommandData;
import de.mhus.nimbus.shared.dto.worldwebsocket.WorldWebSocketResponse;
import de.mhus.nimbus.shared.util.IdentityServiceUtils;
import de.mhus.nimbus.world.bridge.command.ExecuteRequest;
import de.mhus.nimbus.world.bridge.command.ExecuteResponse;
import de.mhus.nimbus.world.bridge.command.WebSocketCommand;
import de.mhus.nimbus.world.bridge.command.WebSocketCommandInfo;
import de.mhus.nimbus.world.bridge.command.*;
import de.mhus.nimbus.world.bridge.service.AuthenticationResult;
import de.mhus.nimbus.world.bridge.service.AuthenticationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class LoginCommand implements WebSocketCommand {

    private final AuthenticationService authenticationService;
    private final IdentityServiceUtils identityServiceUtils;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${nimbus.identity.service.url}")
    private String identityServiceUrl;

    @Override
    public WebSocketCommandInfo info() {
        return new WebSocketCommandInfo("bridge", "login", "Authenticate user with token or username/password");
    }

    @Override
    public ExecuteResponse execute(ExecuteRequest request) {
        try {
            LoginCommandData loginData = objectMapper.convertValue(
                request.getCommand().getData(), LoginCommandData.class);

            String token = loginData.getToken();

            // If no token provided, try username/password authentication
            if (token == null || token.trim().isEmpty()) {
                if (loginData.getUsername() == null || loginData.getPassword() == null ||
                    loginData.getUsername().trim().isEmpty() || loginData.getPassword().trim().isEmpty()) {

                    WorldWebSocketResponse errorResponse = WorldWebSocketResponse.builder()
                            .service(request.getCommand().getService())
                            .command(request.getCommand().getCommand())
                            .requestId(request.getCommand().getRequestId())
                            .status("error")
                            .errorCode("MISSING_CREDENTIALS")
                            .message("Either token or username/password must be provided")
                            .build();
                    return ExecuteResponse.success(errorResponse);
                }

                try {
                    // Authenticate using username/password via Identity Service
                    token = identityServiceUtils.login(identityServiceUrl,
                                                     loginData.getUsername(),
                                                     loginData.getPassword());
                    log.info("Successfully obtained token for user: {}", loginData.getUsername());
                } catch (RuntimeException e) {
                    log.warn("Username/password authentication failed for user: {}", loginData.getUsername());
                    WorldWebSocketResponse errorResponse = WorldWebSocketResponse.builder()
                            .service(request.getCommand().getService())
                            .command(request.getCommand().getCommand())
                            .requestId(request.getCommand().getRequestId())
                            .status("error")
                            .errorCode("INVALID_CREDENTIALS")
                            .message("Invalid username or password")
                            .build();
                    return ExecuteResponse.success(errorResponse);
                }
            }

            // Validate token and get user information
            AuthenticationResult authResult = authenticationService.validateToken(token);

            if (!authResult.isValid()) {
                WorldWebSocketResponse errorResponse = WorldWebSocketResponse.builder()
                        .service(request.getCommand().getService())
                        .command(request.getCommand().getCommand())
                        .requestId(request.getCommand().getRequestId())
                        .status("error")
                        .errorCode("INVALID_TOKEN")
                        .message("Invalid authentication token")
                        .build();
                return ExecuteResponse.success(errorResponse);
            }

            // Update session info
            request.getSessionInfo().setUserId(authResult.getUserId());
            request.getSessionInfo().setRoles(authResult.getRoles());

            WorldWebSocketResponse response = WorldWebSocketResponse.builder()
                    .service(request.getCommand().getService())
                    .command(request.getCommand().getCommand())
                    .requestId(request.getCommand().getRequestId())
                    .status("success")
                    .data(authResult)
                    .message("Login successful")
                    .build();

            return ExecuteResponse.success(response);

        } catch (Exception e) {
            log.error("Error during login", e);
            return ExecuteResponse.error("LOGIN_ERROR", "Login failed");
        }
    }
}
