package de.mhus.nimbus.worldbridge.command.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.mhus.nimbus.shared.dto.websocket.LoginCommandData;
import de.mhus.nimbus.shared.dto.websocket.WebSocketResponse;
import de.mhus.nimbus.worldbridge.command.*;
import de.mhus.nimbus.worldbridge.service.AuthenticationResult;
import de.mhus.nimbus.worldbridge.service.AuthenticationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class LoginCommand implements WebSocketCommand {

    private final AuthenticationService authenticationService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public WebSocketCommandInfo info() {
        return new WebSocketCommandInfo("bridge", "login", "Authenticate user with token");
    }

    @Override
    public ExecuteResponse execute(ExecuteRequest request) {
        try {
            LoginCommandData loginData = objectMapper.convertValue(
                request.getCommand().getData(), LoginCommandData.class);

            // Validate token and get user information
            AuthenticationResult authResult = authenticationService.validateToken(loginData.getToken());

            if (!authResult.isValid()) {
                WebSocketResponse errorResponse = WebSocketResponse.builder()
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

            WebSocketResponse response = WebSocketResponse.builder()
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
