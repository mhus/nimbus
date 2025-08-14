package de.mhus.nimbus.world.bridge.command.impl;

import de.mhus.nimbus.shared.dto.worldwebsocket.LoginCommandData;
import de.mhus.nimbus.shared.dto.worldwebsocket.WorldWebSocketCommand;
import de.mhus.nimbus.shared.util.IdentityServiceUtils;
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
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoginCommandTest {

    @Mock
    private AuthenticationService authenticationService;

    @Mock
    private IdentityServiceUtils identityServiceUtils;

    @InjectMocks
    private LoginCommand loginCommand;

    private WebSocketSession testSession;
    private WorldWebSocketCommand testCommand;

    @BeforeEach
    void setUp() {
        testSession = new WebSocketSession();
        testCommand = new WorldWebSocketCommand("bridge", "login", null, "req-1");
        // Set the identity service URL for testing
        ReflectionTestUtils.setField(loginCommand, "identityServiceUrl", "http://localhost:8080");
    }

    @Test
    void testInfo() {
        // When
        WebSocketCommandInfo info = loginCommand.info();

        // Then
        assertEquals("bridge", info.getService());
        assertEquals("login", info.getCommand());
        assertEquals("Authenticate user with token or username/password", info.getDescription());
    }

    @Test
    void testExecuteSuccessWithToken() {
        // Given
        LoginCommandData loginData = new LoginCommandData("valid-token", null, null);
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
    void testExecuteSuccessWithUsernamePassword() {
        // Given
        LoginCommandData loginData = new LoginCommandData(null, "testuser", "password123");
        testCommand.setData(loginData);
        ExecuteRequest request = new ExecuteRequest("session-1", testSession, testCommand);

        // Mock identity service login to return a token
        when(identityServiceUtils.login("http://localhost:8080", "testuser", "password123"))
                .thenReturn("generated-token");

        // Mock authentication service to validate the generated token
        AuthenticationResult authResult = new AuthenticationResult(true, "user-1", Set.of("USER"), "testuser");
        when(authenticationService.validateToken("generated-token")).thenReturn(authResult);

        // When
        ExecuteResponse response = loginCommand.execute(request);

        // Then
        assertTrue(response.isSuccess());
        assertEquals("success", response.getResponse().getStatus());
        assertEquals("user-1", testSession.getUserId());
        assertTrue(testSession.getRoles().contains("USER"));
    }

    @Test
    void testExecuteInvalidUsernamePassword() {
        // Given
        LoginCommandData loginData = new LoginCommandData(null, "testuser", "wrongpassword");
        testCommand.setData(loginData);
        ExecuteRequest request = new ExecuteRequest("session-1", testSession, testCommand);

        // Mock identity service to throw exception for invalid credentials
        when(identityServiceUtils.login("http://localhost:8080", "testuser", "wrongpassword"))
                .thenThrow(new RuntimeException("Login failed: Invalid username or password"));

        // When
        ExecuteResponse response = loginCommand.execute(request);

        // Then
        assertTrue(response.isSuccess()); // Success because response is returned
        assertEquals("error", response.getResponse().getStatus());
        assertEquals("INVALID_CREDENTIALS", response.getResponse().getErrorCode());
        assertEquals("Invalid username or password", response.getResponse().getMessage());
        assertNull(testSession.getUserId());
    }

    @Test
    void testExecuteMissingCredentials() {
        // Given - no token, username, or password
        LoginCommandData loginData = new LoginCommandData(null, null, null);
        testCommand.setData(loginData);
        ExecuteRequest request = new ExecuteRequest("session-1", testSession, testCommand);

        // When
        ExecuteResponse response = loginCommand.execute(request);

        // Then
        assertTrue(response.isSuccess()); // Success because response is returned
        assertEquals("error", response.getResponse().getStatus());
        assertEquals("MISSING_CREDENTIALS", response.getResponse().getErrorCode());
        assertEquals("Either token or username/password must be provided", response.getResponse().getMessage());
        assertNull(testSession.getUserId());
    }

    @Test
    void testExecuteMissingUsername() {
        // Given - no token, missing username
        LoginCommandData loginData = new LoginCommandData(null, null, "password123");
        testCommand.setData(loginData);
        ExecuteRequest request = new ExecuteRequest("session-1", testSession, testCommand);

        // When
        ExecuteResponse response = loginCommand.execute(request);

        // Then
        assertTrue(response.isSuccess()); // Success because response is returned
        assertEquals("error", response.getResponse().getStatus());
        assertEquals("MISSING_CREDENTIALS", response.getResponse().getErrorCode());
        assertNull(testSession.getUserId());
    }

    @Test
    void testExecuteMissingPassword() {
        // Given - no token, missing password
        LoginCommandData loginData = new LoginCommandData(null, "testuser", null);
        testCommand.setData(loginData);
        ExecuteRequest request = new ExecuteRequest("session-1", testSession, testCommand);

        // When
        ExecuteResponse response = loginCommand.execute(request);

        // Then
        assertTrue(response.isSuccess()); // Success because response is returned
        assertEquals("error", response.getResponse().getStatus());
        assertEquals("MISSING_CREDENTIALS", response.getResponse().getErrorCode());
        assertNull(testSession.getUserId());
    }

    @Test
    void testExecuteEmptyCredentials() {
        // Given - empty strings for credentials
        LoginCommandData loginData = new LoginCommandData("", "", "");
        testCommand.setData(loginData);
        ExecuteRequest request = new ExecuteRequest("session-1", testSession, testCommand);

        // When
        ExecuteResponse response = loginCommand.execute(request);

        // Then
        assertTrue(response.isSuccess()); // Success because response is returned
        assertEquals("error", response.getResponse().getStatus());
        assertEquals("MISSING_CREDENTIALS", response.getResponse().getErrorCode());
        assertNull(testSession.getUserId());
    }

    @Test
    void testExecuteInvalidToken() {
        // Given
        LoginCommandData loginData = new LoginCommandData("invalid-token", null, null);
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
        LoginCommandData invalidData = new LoginCommandData();
        invalidData.setToken("invalid");
        testCommand.setData(invalidData);
        ExecuteRequest request = new ExecuteRequest("session-1", testSession, testCommand);

        // Mock the authenticationService to throw an exception during token validation
        when(authenticationService.validateToken("invalid")).thenThrow(new RuntimeException("Service error"));

        // When
        ExecuteResponse response = loginCommand.execute(request);

        // Then
        assertFalse(response.isSuccess());
        assertEquals("LOGIN_ERROR", response.getErrorCode());
        assertEquals("Login failed", response.getMessage());
    }
}
