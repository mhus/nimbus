package de.mhus.nimbus.world.bridge.command.world;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.mhus.nimbus.shared.dto.command.UpdateWorldCommandData;
import de.mhus.nimbus.shared.dto.world.WorldDto;
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

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UpdateWorldCommandTest {

    @Mock
    private TerrainServiceClient terrainServiceClient;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private UpdateWorldCommand updateWorldCommand;

    private ExecuteRequest executeRequest;
    private WorldWebSocketCommand mockCommand;
    private UpdateWorldCommandData commandData;
    private WorldDto worldDto;

    @BeforeEach
    void setUp() {
        executeRequest = mock(ExecuteRequest.class);
        mockCommand = mock(WorldWebSocketCommand.class);

        worldDto = new WorldDto();
        worldDto.setName("Updated World");
        worldDto.setDescription("Updated Description");

        commandData = new UpdateWorldCommandData();
        commandData.setWorldId("world-123");
        commandData.setWorld(worldDto);

        // Configure the mock objects only when needed - not for testInfo()
    }

    private void setupMocksForExecution() {
        when(executeRequest.getCommand()).thenReturn(mockCommand);
        when(mockCommand.getService()).thenReturn("terrain");
        when(mockCommand.getCommand()).thenReturn("updateWorld");
        when(mockCommand.getRequestId()).thenReturn("test-request-id");
    }

    @Test
    void testInfo() {
        WebSocketCommandInfo info = updateWorldCommand.info();

        assertEquals("terrain", info.getService());
        assertEquals("updateWorld", info.getCommand());
        assertEquals("Update an existing world", info.getDescription());
        assertTrue(info.isWorldRequired());
    }

    @Test
    void testExecuteSuccess() {
        // Arrange
        setupMocksForExecution();
        WorldDto updatedWorld = new WorldDto();
        updatedWorld.setId("world-123");
        updatedWorld.setName("Updated World");

        when(objectMapper.convertValue(any(), eq(UpdateWorldCommandData.class)))
            .thenReturn(commandData);
        when(terrainServiceClient.updateWorld("world-123", worldDto))
            .thenReturn(Optional.of(updatedWorld));

        // Act
        ExecuteResponse response = updateWorldCommand.execute(executeRequest);

        // Assert
        assertTrue(response.isSuccess());
        assertEquals(updatedWorld, response.getResponse().getData());
        verify(terrainServiceClient).updateWorld("world-123", worldDto);
    }

    @Test
    void testExecuteWorldNotFound() {
        // Arrange
        setupMocksForExecution();
        when(objectMapper.convertValue(any(), eq(UpdateWorldCommandData.class)))
            .thenReturn(commandData);
        when(terrainServiceClient.updateWorld("world-123", worldDto))
            .thenReturn(Optional.empty());

        // Act
        ExecuteResponse response = updateWorldCommand.execute(executeRequest);

        // Assert
        assertTrue(response.isSuccess());
        assertEquals("error", response.getResponse().getStatus());
        assertEquals("not_found", response.getResponse().getErrorCode());
        assertEquals("World not found", response.getResponse().getMessage());
    }

    @Test
    void testExecuteWithNullWorldId() {
        // Arrange
        UpdateWorldCommandData emptyData = new UpdateWorldCommandData();
        emptyData.setWorldId(null);
        emptyData.setWorld(worldDto);

        setupMocksForExecution();
        when(objectMapper.convertValue(any(), eq(UpdateWorldCommandData.class)))
            .thenReturn(emptyData);

        // Act
        ExecuteResponse response = updateWorldCommand.execute(executeRequest);

        // Assert
        assertTrue(response.isSuccess());
        assertEquals("error", response.getResponse().getStatus());
        assertEquals("error", response.getResponse().getErrorCode());
        assertEquals("World ID is required", response.getResponse().getMessage());
        verify(terrainServiceClient, never()).updateWorld(any(), any());
    }

    @Test
    void testExecuteWithNullWorldData() {
        // Arrange
        UpdateWorldCommandData emptyData = new UpdateWorldCommandData();
        emptyData.setWorldId("world-123");
        emptyData.setWorld(null);

        setupMocksForExecution();
        when(objectMapper.convertValue(any(), eq(UpdateWorldCommandData.class)))
            .thenReturn(emptyData);

        // Act
        ExecuteResponse response = updateWorldCommand.execute(executeRequest);

        // Assert
        assertTrue(response.isSuccess());
        assertEquals("error", response.getResponse().getStatus());
        assertEquals("error", response.getResponse().getErrorCode());
        assertEquals("World data is required", response.getResponse().getMessage());
        verify(terrainServiceClient, never()).updateWorld(any(), any());
    }
}
