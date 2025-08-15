package de.mhus.nimbus.world.bridge.command.world;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.mhus.nimbus.shared.dto.command.DeleteWorldCommandData;
import de.mhus.nimbus.shared.dto.worldwebsocket.WorldWebSocketCommand;
import de.mhus.nimbus.world.bridge.command.ExecuteRequest;
import de.mhus.nimbus.world.bridge.command.ExecuteResponse;
import de.mhus.nimbus.world.bridge.command.WebSocketCommandInfo;
import de.mhus.nimbus.world.shared.client.TerrainServiceClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeleteWorldCommandTest {

    @Mock
    private TerrainServiceClient terrainServiceClient;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private DeleteWorldCommand deleteWorldCommand;

    private ExecuteRequest executeRequest;
    private WorldWebSocketCommand mockCommand;
    private DeleteWorldCommandData commandData;

    @BeforeEach
    void setUp() {
        executeRequest = mock(ExecuteRequest.class);
        mockCommand = mock(WorldWebSocketCommand.class);
        commandData = new DeleteWorldCommandData();
        commandData.setWorldId("world-123");

        // Configure the mock objects only when needed - not for testInfo()
    }

    private void setupMocksForExecution() {
        when(executeRequest.getCommand()).thenReturn(mockCommand);
        when(mockCommand.getService()).thenReturn("terrain");
        when(mockCommand.getCommand()).thenReturn("deleteWorld");
        when(mockCommand.getRequestId()).thenReturn("test-request-id");
    }

    @Test
    void testInfo() {
        WebSocketCommandInfo info = deleteWorldCommand.info();

        assertEquals("terrain", info.getService());
        assertEquals("deleteWorld", info.getCommand());
        assertEquals("Delete a world", info.getDescription());
        assertTrue(info.isWorldRequired());
    }

    @Test
    void testExecuteSuccess() {
        // Arrange
        setupMocksForExecution();
        when(objectMapper.convertValue(any(), eq(DeleteWorldCommandData.class)))
            .thenReturn(commandData);
        when(terrainServiceClient.deleteWorld("world-123")).thenReturn(true);

        // Act
        ExecuteResponse response = deleteWorldCommand.execute(executeRequest);

        // Assert
        assertTrue(response.isSuccess());
        assertEquals("World deleted successfully", response.getResponse().getData());
        verify(terrainServiceClient).deleteWorld("world-123");
    }

    @Test
    void testExecuteWorldNotFound() {
        // Arrange
        setupMocksForExecution();
        when(objectMapper.convertValue(any(), eq(DeleteWorldCommandData.class)))
            .thenReturn(commandData);
        when(terrainServiceClient.deleteWorld("world-123")).thenReturn(false);

        // Act
        ExecuteResponse response = deleteWorldCommand.execute(executeRequest);

        // Assert
        assertTrue(response.isSuccess());
        assertEquals("error", response.getResponse().getStatus());
        assertEquals("not_found", response.getResponse().getErrorCode());
        assertEquals("World not found", response.getResponse().getMessage());
    }

    @Test
    void testExecuteWithNullWorldId() {
        // Arrange
        DeleteWorldCommandData emptyData = new DeleteWorldCommandData();
        emptyData.setWorldId(null);

        setupMocksForExecution();
        when(objectMapper.convertValue(any(), eq(DeleteWorldCommandData.class)))
            .thenReturn(emptyData);

        // Act
        ExecuteResponse response = deleteWorldCommand.execute(executeRequest);

        // Assert
        assertTrue(response.isSuccess());
        assertEquals("error", response.getResponse().getStatus());
        assertEquals("error", response.getResponse().getErrorCode());
        assertEquals("World ID is required", response.getResponse().getMessage());
        verify(terrainServiceClient, never()).deleteWorld(any());
    }

    @Test
    void testExecuteWithEmptyWorldId() {
        // Arrange
        DeleteWorldCommandData emptyData = new DeleteWorldCommandData();
        emptyData.setWorldId("");

        setupMocksForExecution();
        when(objectMapper.convertValue(any(), eq(DeleteWorldCommandData.class)))
            .thenReturn(emptyData);

        // Act
        ExecuteResponse response = deleteWorldCommand.execute(executeRequest);

        // Assert
        assertTrue(response.isSuccess());
        assertEquals("error", response.getResponse().getStatus());
        assertEquals("error", response.getResponse().getErrorCode());
        assertEquals("World ID is required", response.getResponse().getMessage());
        verify(terrainServiceClient, never()).deleteWorld(any());
    }

    @Test
    void testExecuteWithException() {
        // Arrange
        setupMocksForExecution();
        when(objectMapper.convertValue(any(), eq(DeleteWorldCommandData.class)))
            .thenReturn(commandData);
        when(terrainServiceClient.deleteWorld("world-123"))
            .thenThrow(new RuntimeException("Service error"));

        // Act
        ExecuteResponse response = deleteWorldCommand.execute(executeRequest);

        // Assert
        assertTrue(response.isSuccess());
        assertEquals("error", response.getResponse().getStatus());
        assertEquals("error", response.getResponse().getErrorCode());
        assertTrue(response.getResponse().getMessage().contains("Failed to delete world"));
    }
}
