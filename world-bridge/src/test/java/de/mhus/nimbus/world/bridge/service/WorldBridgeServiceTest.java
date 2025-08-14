package de.mhus.nimbus.world.bridge.service;

import de.mhus.nimbus.shared.dto.worldwebsocket.*;
import de.mhus.nimbus.world.bridge.command.ExecuteRequest;
import de.mhus.nimbus.world.bridge.command.ExecuteResponse;
import de.mhus.nimbus.world.bridge.command.WebSocketCommand;
import de.mhus.nimbus.world.bridge.command.WebSocketCommandInfo;
import de.mhus.nimbus.world.bridge.command.*;
import de.mhus.nimbus.world.bridge.model.WebSocketSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WorldBridgeServiceTest {

    @Mock
    private WebSocketCommand mockCommand1;

    @Mock
    private WebSocketCommand mockCommand2;

    @InjectMocks
    private WorldBridgeService worldBridgeService;

    private WebSocketSession testSession;

    @BeforeEach
    void setUp() {
        testSession = new WebSocketSession();

        // Mock command infos
        when(mockCommand1.info()).thenReturn(new WebSocketCommandInfo("bridge", "login", "Login command"));
        when(mockCommand2.info()).thenReturn(new WebSocketCommandInfo("bridge", "ping", "Ping command"));

        // Initialize service with mocked commands
        List<WebSocketCommand> commands = Arrays.asList(mockCommand1, mockCommand2);
        worldBridgeService = new WorldBridgeService(commands);
        worldBridgeService.initializeCommands();
    }

    @Test
    void testInitializeCommands() {
        // Given - setup already done in @BeforeEach

        // When
        List<WebSocketCommandInfo> availableCommands = worldBridgeService.getAvailableCommands();

        // Then
        assertEquals(2, availableCommands.size());
        assertTrue(availableCommands.stream().anyMatch(cmd -> "login".equals(cmd.getCommand())));
        assertTrue(availableCommands.stream().anyMatch(cmd -> "ping".equals(cmd.getCommand())));
    }

    @Test
    void testProcessCommandSuccess() {
        // Given
        String sessionId = "test-session";
        WorldWebSocketCommand command = new WorldWebSocketCommand("bridge", "login", new LoginCommandData("token", null, null), "req-1");

        WorldWebSocketResponse expectedResponse = WorldWebSocketResponse.builder()
                .service("bridge")
                .command("login")
                .requestId("req-1")
                .status("success")
                .build();

        ExecuteResponse executeResponse = ExecuteResponse.success(expectedResponse);
        when(mockCommand1.execute(any(ExecuteRequest.class))).thenReturn(executeResponse);

        // When
        WorldWebSocketResponse response = worldBridgeService.processCommand(sessionId, testSession, command);

        // Then
        assertEquals("success", response.getStatus());
        assertEquals("bridge", response.getService());
        assertEquals("login", response.getCommand());
        assertEquals("req-1", response.getRequestId());
        verify(mockCommand1).execute(any(ExecuteRequest.class));
    }

    @Test
    void testProcessCommandError() {
        // Given
        String sessionId = "test-session";
        WorldWebSocketCommand command = new WorldWebSocketCommand("bridge", "login", new LoginCommandData("token", null, null), "req-1");

        ExecuteResponse executeResponse = ExecuteResponse.error("AUTH_ERROR", "Authentication failed");
        when(mockCommand1.execute(any(ExecuteRequest.class))).thenReturn(executeResponse);

        // When
        WorldWebSocketResponse response = worldBridgeService.processCommand(sessionId, testSession, command);

        // Then
        assertEquals("error", response.getStatus());
        assertEquals("AUTH_ERROR", response.getErrorCode());
        assertEquals("Authentication failed", response.getMessage());
    }

    @Test
    void testExecuteCommandUnknown() {
        // Given
        String sessionId = "test-session";
        WorldWebSocketCommand command = new WorldWebSocketCommand("bridge", "unknown", null, "req-1");

        // When
        ExecuteResponse response = worldBridgeService.executeCommand(sessionId, testSession, command);

        // Then
        assertFalse(response.isSuccess());
        assertEquals("UNKNOWN_COMMAND", response.getErrorCode());
        assertEquals("Unknown command: unknown", response.getMessage());
    }

    @Test
    void testProcessCommandException() {
        // Given
        String sessionId = "test-session";
        WorldWebSocketCommand command = new WorldWebSocketCommand("bridge", "login", new LoginCommandData("token", null, null), "req-1");

        when(mockCommand1.execute(any(ExecuteRequest.class))).thenThrow(new RuntimeException("Test exception"));

        // When
        WorldWebSocketResponse response = worldBridgeService.processCommand(sessionId, testSession, command);

        // Then
        assertEquals("error", response.getStatus());
        assertEquals("INTERNAL_ERROR", response.getErrorCode());
        assertEquals("Internal server error", response.getMessage());
    }
}
