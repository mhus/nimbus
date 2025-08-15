package de.mhus.nimbus.world.bridge.service;

import de.mhus.nimbus.shared.dto.worldwebsocket.WorldWebSocketCommand;
import de.mhus.nimbus.world.bridge.command.ExecuteRequest;
import de.mhus.nimbus.world.bridge.command.ExecuteResponse;
import de.mhus.nimbus.world.bridge.command.WebSocketCommand;
import de.mhus.nimbus.world.bridge.command.WebSocketCommandInfo;
import de.mhus.nimbus.world.bridge.model.WebSocketSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WorldBridgeServiceAuthenticationTest {

    @Mock
    private WebSocketCommand mockCommand;

    private WorldBridgeService worldBridgeService;
    private WebSocketSession testSession;
    private WorldWebSocketCommand testWebSocketCommand;

    @BeforeEach
    void setUp() {
        // WICHTIG: Mock-Command MUSS vor initializeCommands() konfiguriert werden
        when(mockCommand.info()).thenReturn(new WebSocketCommandInfo("bridge", "test", "Test command", true, true));

        worldBridgeService = new WorldBridgeService(List.of(mockCommand));
        worldBridgeService.initializeCommands();

        testSession = new WebSocketSession();
        testWebSocketCommand = new WorldWebSocketCommand("bridge", "test", null, "req-1");
    }

    @Test
    void testExecuteCommand_withAuthenticationRequired_userNotLoggedIn_shouldReturnError() {
        // Given
        WebSocketCommandInfo commandInfo = new WebSocketCommandInfo("bridge", "test", "Test command", true, true);
        when(mockCommand.info()).thenReturn(commandInfo);

        testSession.setUserId(null); // User not logged in
        testSession.setWorldId("world-1"); // World selected

        // When
        ExecuteResponse response = worldBridgeService.executeCommand("session-1", testSession, testWebSocketCommand);

        // Then
        assertFalse(response.isSuccess());
        assertEquals("NOT_AUTHENTICATED", response.getErrorCode());
        assertEquals("User not authenticated", response.getMessage());
        verify(mockCommand, never()).execute(any());
    }

    @Test
    void testExecuteCommand_withAuthenticationRequired_userLoggedIn_shouldExecute() {
        // Given
        WebSocketCommandInfo commandInfo = new WebSocketCommandInfo("bridge", "test", "Test command", true, true);
        ExecuteResponse mockExecuteResponse = ExecuteResponse.success(null);

        when(mockCommand.info()).thenReturn(commandInfo);
        when(mockCommand.execute(any())).thenReturn(mockExecuteResponse);

        testSession.setUserId("user-1"); // User logged in
        testSession.setWorldId("world-1"); // World selected

        // When
        ExecuteResponse response = worldBridgeService.executeCommand("session-1", testSession, testWebSocketCommand);

        // Then
        assertTrue(response.isSuccess());
        verify(mockCommand).execute(any(ExecuteRequest.class));
    }

    @Test
    void testExecuteCommand_withoutAuthenticationRequired_userNotLoggedIn_shouldExecute() {
        // Given
        WebSocketCommandInfo commandInfo = new WebSocketCommandInfo("bridge", "test", "Test command", false, false);
        ExecuteResponse mockExecuteResponse = ExecuteResponse.success(null);

        when(mockCommand.info()).thenReturn(commandInfo);
        when(mockCommand.execute(any())).thenReturn(mockExecuteResponse);

        testSession.setUserId(null); // User not logged in
        testSession.setWorldId(null); // No world selected

        // When
        ExecuteResponse response = worldBridgeService.executeCommand("session-1", testSession, testWebSocketCommand);

        // Then
        assertTrue(response.isSuccess());
        verify(mockCommand).execute(any(ExecuteRequest.class));
    }

    @Test
    void testExecuteCommand_withWorldRequired_noWorldSelected_shouldReturnError() {
        // Given
        WebSocketCommandInfo commandInfo = new WebSocketCommandInfo("bridge", "test", "Test command", true, false);

        when(mockCommand.info()).thenReturn(commandInfo);

        testSession.setUserId(null); // User not logged in (but auth not required)
        testSession.setWorldId(null); // No world selected

        // When
        ExecuteResponse response = worldBridgeService.executeCommand("session-1", testSession, testWebSocketCommand);

        // Then
        assertFalse(response.isSuccess());
        assertEquals("NO_WORLD_SELECTED", response.getErrorCode());
        assertEquals("No world selected", response.getMessage());
        verify(mockCommand, never()).execute(any());
    }

    @Test
    void testExecuteCommand_withWorldRequired_worldSelected_shouldExecute() {
        // Given
        WebSocketCommandInfo commandInfo = new WebSocketCommandInfo("bridge", "test", "Test command", true, false);
        ExecuteResponse mockExecuteResponse = ExecuteResponse.success(null);

        when(mockCommand.info()).thenReturn(commandInfo);
        when(mockCommand.execute(any())).thenReturn(mockExecuteResponse);

        testSession.setUserId(null); // User not logged in (but auth not required)
        testSession.setWorldId("world-1"); // World selected

        // When
        ExecuteResponse response = worldBridgeService.executeCommand("session-1", testSession, testWebSocketCommand);

        // Then
        assertTrue(response.isSuccess());
        verify(mockCommand).execute(any(ExecuteRequest.class));
    }

    @Test
    void testExecuteCommand_unknownCommand_shouldReturnError() {
        // Given
        WorldWebSocketCommand unknownCommand = new WorldWebSocketCommand("bridge", "unknown", null, "req-1");

        // When
        ExecuteResponse response = worldBridgeService.executeCommand("session-1", testSession, unknownCommand);

        // Then
        assertFalse(response.isSuccess());
        assertEquals("UNKNOWN_COMMAND", response.getErrorCode());
        assertEquals("Unknown command: unknown", response.getMessage());
    }
}
