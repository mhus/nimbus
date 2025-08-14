package de.mhus.nimbus.world.bridge.command.world;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.mhus.nimbus.shared.dto.command.UpdateWorldCommandData;
import de.mhus.nimbus.shared.dto.world.WorldDto;
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
    private UpdateWorldCommandData commandData;
    private WorldDto worldDto;

    @BeforeEach
    void setUp() {
        executeRequest = mock(ExecuteRequest.class);

        worldDto = new WorldDto();
        worldDto.setName("Updated World");
        worldDto.setDescription("Updated Description");

        commandData = new UpdateWorldCommandData();
        commandData.setWorldId("world-123");
        commandData.setWorld(worldDto);
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
        assertEquals(updatedWorld, response.getData());
        verify(terrainServiceClient).updateWorld("world-123", worldDto);
    }

    @Test
    void testExecuteWorldNotFound() {
        // Arrange
        when(objectMapper.convertValue(any(), eq(UpdateWorldCommandData.class)))
            .thenReturn(commandData);
        when(terrainServiceClient.updateWorld("world-123", worldDto))
            .thenReturn(Optional.empty());

        // Act
        ExecuteResponse response = updateWorldCommand.execute(executeRequest);

        // Assert
        assertFalse(response.isSuccess());
        assertEquals("not_found", response.getErrorCode());
        assertEquals("World not found", response.getMessage());
    }

    @Test
    void testExecuteWithNullWorldId() {
        // Arrange
        UpdateWorldCommandData emptyData = new UpdateWorldCommandData();
        emptyData.setWorldId(null);
        emptyData.setWorld(worldDto);

        when(objectMapper.convertValue(any(), eq(UpdateWorldCommandData.class)))
            .thenReturn(emptyData);

        // Act
        ExecuteResponse response = updateWorldCommand.execute(executeRequest);

        // Assert
        assertFalse(response.isSuccess());
        assertEquals("error", response.getErrorCode());
        assertEquals("World ID is required", response.getMessage());
        verify(terrainServiceClient, never()).updateWorld(any(), any());
    }

    @Test
    void testExecuteWithNullWorldData() {
        // Arrange
        UpdateWorldCommandData emptyData = new UpdateWorldCommandData();
        emptyData.setWorldId("world-123");
        emptyData.setWorld(null);

        when(objectMapper.convertValue(any(), eq(UpdateWorldCommandData.class)))
            .thenReturn(emptyData);

        // Act
        ExecuteResponse response = updateWorldCommand.execute(executeRequest);

        // Assert
        assertFalse(response.isSuccess());
        assertEquals("error", response.getErrorCode());
        assertEquals("World data is required", response.getMessage());
        verify(terrainServiceClient, never()).updateWorld(any(), any());
    }
}
