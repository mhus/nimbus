package de.mhus.nimbus.world.bridge.command.world;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.mhus.nimbus.shared.dto.command.CreateWorldCommandData;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreateWorldCommandTest {

    @Mock
    private TerrainServiceClient terrainServiceClient;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private CreateWorldCommand createWorldCommand;

    private ExecuteRequest executeRequest;
    private WorldDto worldDto;
    private CreateWorldCommandData commandData;

    @BeforeEach
    void setUp() {
        executeRequest = mock(ExecuteRequest.class);
        worldDto = new WorldDto();
        worldDto.setName("Test World");
        worldDto.setDescription("Test Description");

        commandData = new CreateWorldCommandData();
        commandData.setWorld(worldDto);
    }

    @Test
    void testInfo() {
        WebSocketCommandInfo info = createWorldCommand.info();

        assertEquals("terrain", info.getService());
        assertEquals("createWorld", info.getCommand());
        assertEquals("Create a new world", info.getDescription());
        assertFalse(info.isWorldRequired());
    }

    @Test
    void testExecuteSuccess() {
        // Arrange
        WorldDto createdWorld = new WorldDto();
        createdWorld.setId("world-123");
        createdWorld.setName("Test World");

        when(objectMapper.convertValue(any(), eq(CreateWorldCommandData.class)))
            .thenReturn(commandData);
        when(terrainServiceClient.createWorld(worldDto)).thenReturn(createdWorld);

        // Act
        ExecuteResponse response = createWorldCommand.execute(executeRequest);

        // Assert
        assertTrue(response.isSuccess());
        assertEquals(createdWorld, response.getData());
        verify(terrainServiceClient).createWorld(worldDto);
    }

    @Test
    void testExecuteWithNullWorldData() {
        // Arrange
        CreateWorldCommandData emptyData = new CreateWorldCommandData();
        emptyData.setWorld(null);

        when(objectMapper.convertValue(any(), eq(CreateWorldCommandData.class)))
            .thenReturn(emptyData);

        // Act
        ExecuteResponse response = createWorldCommand.execute(executeRequest);

        // Assert
        assertFalse(response.isSuccess());
        assertEquals("error", response.getErrorCode());
        assertEquals("World data is required", response.getMessage());
        verify(terrainServiceClient, never()).createWorld(any());
    }

    @Test
    void testExecuteWithException() {
        // Arrange
        when(objectMapper.convertValue(any(), eq(CreateWorldCommandData.class)))
            .thenReturn(commandData);
        when(terrainServiceClient.createWorld(worldDto))
            .thenThrow(new RuntimeException("Service error"));

        // Act
        ExecuteResponse response = createWorldCommand.execute(executeRequest);

        // Assert
        assertFalse(response.isSuccess());
        assertEquals("error", response.getErrorCode());
        assertTrue(response.getMessage().contains("Failed to create world"));
    }
}
