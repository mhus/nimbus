package de.mhus.nimbus.world.bridge.command.world;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.mhus.nimbus.shared.dto.command.DeleteWorldCommandData;
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
    private DeleteWorldCommandData commandData;

    @BeforeEach
    void setUp() {
        executeRequest = mock(ExecuteRequest.class);
        commandData = new DeleteWorldCommandData();
        commandData.setWorldId("world-123");
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
        when(objectMapper.convertValue(any(), eq(DeleteWorldCommandData.class)))
            .thenReturn(commandData);
        when(terrainServiceClient.deleteWorld("world-123")).thenReturn(true);

        // Act
        ExecuteResponse response = deleteWorldCommand.execute(executeRequest);

        // Assert
        assertTrue(response.isSuccess());
        assertEquals("World deleted successfully", response.getData());
        verify(terrainServiceClient).deleteWorld("world-123");
    }

    @Test
    void testExecuteWorldNotFound() {
        // Arrange
        when(objectMapper.convertValue(any(), eq(DeleteWorldCommandData.class)))
            .thenReturn(commandData);
        when(terrainServiceClient.deleteWorld("world-123")).thenReturn(false);

        // Act
        ExecuteResponse response = deleteWorldCommand.execute(executeRequest);

        // Assert
        assertFalse(response.isSuccess());
        assertEquals("not_found", response.getErrorCode());
        assertEquals("World not found", response.getMessage());
    }

    @Test
    void testExecuteWithNullWorldId() {
        // Arrange
        DeleteWorldCommandData emptyData = new DeleteWorldCommandData();
        emptyData.setWorldId(null);

        when(objectMapper.convertValue(any(), eq(DeleteWorldCommandData.class)))
            .thenReturn(emptyData);

        // Act
        ExecuteResponse response = deleteWorldCommand.execute(executeRequest);

        // Assert
        assertFalse(response.isSuccess());
        assertEquals("error", response.getErrorCode());
        assertEquals("World ID is required", response.getMessage());
        verify(terrainServiceClient, never()).deleteWorld(any());
    }

    @Test
    void testExecuteWithEmptyWorldId() {
        // Arrange
        DeleteWorldCommandData emptyData = new DeleteWorldCommandData();
        emptyData.setWorldId("");

        when(objectMapper.convertValue(any(), eq(DeleteWorldCommandData.class)))
            .thenReturn(emptyData);

        // Act
        ExecuteResponse response = deleteWorldCommand.execute(executeRequest);

        // Assert
        assertFalse(response.isSuccess());
        assertEquals("error", response.getErrorCode());
        assertEquals("World ID is required", response.getMessage());
        verify(terrainServiceClient, never()).deleteWorld(any());
    }

    @Test
    void testExecuteWithException() {
        // Arrange
        when(objectMapper.convertValue(any(), eq(DeleteWorldCommandData.class)))
            .thenReturn(commandData);
        when(terrainServiceClient.deleteWorld("world-123"))
            .thenThrow(new RuntimeException("Service error"));

        // Act
        ExecuteResponse response = deleteWorldCommand.execute(executeRequest);

        // Assert
        assertFalse(response.isSuccess());
        assertEquals("error", response.getErrorCode());
        assertTrue(response.getMessage().contains("Failed to delete world"));
    }
}
