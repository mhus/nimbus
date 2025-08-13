package de.mhus.nimbus.world.bridge.command.impl;

import de.mhus.nimbus.shared.dto.worldwebsocket.LoginCommandData;
import de.mhus.nimbus.shared.dto.worldwebsocket.WorldWebSocketCommand;
import de.mhus.nimbus.world.bridge.command.ExecuteRequest;
import de.mhus.nimbus.world.bridge.command.ExecuteResponse;
import de.mhus.nimbus.world.bridge.command.WebSocketCommandInfo;
import de.mhus.nimbus.world.bridge.model.WebSocketSession;
import de.mhus.nimbus.world.bridge.service.AuthenticationResult;
import de.mhus.nimbus.world.bridge.service.AuthenticationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoginCommandTest {

    @Mock
    private AuthenticationService authenticationService;

    @InjectMocks
    private LoginCommand loginCommand;

    private WebSocketSession testSession;
    private WorldWebSocketCommand testCommand;

    @BeforeEach
    void setUp() {
        testSession = new WebSocketSession();
        testCommand = new WorldWebSocketCommand("bridge", "login", null, "req-1");
    }

    @Test
    void testInfo() {
        // When
        WebSocketCommandInfo info = loginCommand.info();

        // Then
        assertEquals("bridge", info.getService());
        assertEquals("login", info.getCommand());
        assertEquals("Authenticate user with token", info.getDescription());
    }

    @Test
    void testExecuteSuccess() {
        // Given
        LoginCommandData loginData = new LoginCommandData("valid-token");
        testCommand.setData(loginData);
        ExecuteRequest request = new ExecuteRequest("session-1", testSession, testCommand);

        AuthenticationResult authResult = new AuthenticationResult(true, "user-1", Set.of("USER"), "testuser");
        when(authenticationService.validateToken("valid-token")).thenReturn(authResult);

        // When
        ExecuteResponse response = loginCommand.execute(request);

        // Then
        assertTrue(response.isSuccess());
        assertEquals("success", response.getResponse().getStatus());
        assertEquals("user-1", testSession.getUserId());
        assertTrue(testSession.getRoles().contains("USER"));
    }

    @Test
    void testExecuteInvalidToken() {
        // Given
        LoginCommandData loginData = new LoginCommandData("invalid-token");
        testCommand.setData(loginData);
        ExecuteRequest request = new ExecuteRequest("session-1", testSession, testCommand);

        AuthenticationResult authResult = new AuthenticationResult(false, null, Set.of(), null);
        when(authenticationService.validateToken("invalid-token")).thenReturn(authResult);

        // When
        ExecuteResponse response = loginCommand.execute(request);

        // Then
        assertTrue(response.isSuccess()); // Success because response is returned
        assertEquals("error", response.getResponse().getStatus());
        assertEquals("INVALID_TOKEN", response.getResponse().getErrorCode());
        assertNull(testSession.getUserId());
    }

    @Test
    void testExecuteException() {
        // Given
        testCommand.setData("invalid-data");
        ExecuteRequest request = new ExecuteRequest("session-1", testSession, testCommand);

        when(authenticationService.validateToken(anyString())).thenThrow(new RuntimeException("Service error"));

        // When
        ExecuteResponse response = loginCommand.execute(request);

        // Then
        assertFalse(response.isSuccess());
        assertEquals("LOGIN_ERROR", response.getErrorCode());
        assertEquals("Login failed", response.getMessage());
    }
}
