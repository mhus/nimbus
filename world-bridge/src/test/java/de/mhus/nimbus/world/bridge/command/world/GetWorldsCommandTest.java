package de.mhus.nimbus.world.bridge.command.world;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.mhus.nimbus.shared.dto.command.GetWorldsCommandData;
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

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GetWorldsCommandTest {

    @Mock
    private TerrainServiceClient terrainServiceClient;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private GetWorldsCommand getWorldsCommand;

    private ExecuteRequest executeRequest;
    private WorldWebSocketCommand mockCommand;

    @BeforeEach
    void setUp() {
        executeRequest = mock(ExecuteRequest.class);
        mockCommand = mock(WorldWebSocketCommand.class);

        // Configure the mock objects only when needed - not for testInfo()
    }

    private void setupMocksForExecution() {
        when(executeRequest.getCommand()).thenReturn(mockCommand);
        when(mockCommand.getService()).thenReturn("terrain");
        when(mockCommand.getCommand()).thenReturn("getWorlds");
        when(mockCommand.getRequestId()).thenReturn("test-request-id");
    }

    @Test
    void testInfo() {
        WebSocketCommandInfo info = getWorldsCommand.info();

        assertEquals("terrain", info.getService());
        assertEquals("getWorlds", info.getCommand());
        assertEquals("Get all worlds", info.getDescription());
        assertFalse(info.isWorldRequired());
    }

    @Test
    void testExecuteSuccess() {
        // Arrange
        WorldDto world1 = new WorldDto();
        world1.setId("world-1");
        world1.setName("World 1");

        WorldDto world2 = new WorldDto();
        world2.setId("world-2");
        world2.setName("World 2");

        List<WorldDto> worlds = Arrays.asList(world1, world2);
        when(terrainServiceClient.getAllWorlds()).thenReturn(worlds);
        setupMocksForExecution();

        // Act
        ExecuteResponse response = getWorldsCommand.execute(executeRequest);

        // Assert
        assertTrue(response.isSuccess());
        assertEquals(worlds, response.getResponse().getData());
        verify(terrainServiceClient).getAllWorlds();
    }

    @Test
    void testExecuteWithException() {
        // Arrange
        when(terrainServiceClient.getAllWorlds())
            .thenThrow(new RuntimeException("Service error"));
        setupMocksForExecution();

        // Act
        ExecuteResponse response = getWorldsCommand.execute(executeRequest);

        // Assert
        assertTrue(response.isSuccess());
        assertEquals("error", response.getResponse().getStatus());
        assertEquals("error", response.getResponse().getErrorCode());
        assertTrue(response.getResponse().getMessage().contains("Failed to get worlds"));
    }
}
