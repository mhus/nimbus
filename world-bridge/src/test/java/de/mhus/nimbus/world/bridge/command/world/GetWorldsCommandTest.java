package de.mhus.nimbus.world.bridge.command.world;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.mhus.nimbus.shared.dto.command.GetWorldsCommandData;
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

    @BeforeEach
    void setUp() {
        executeRequest = mock(ExecuteRequest.class);
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

        // Act
        ExecuteResponse response = getWorldsCommand.execute(executeRequest);

        // Assert
        assertTrue(response.isSuccess());
        assertEquals(worlds, response.getData());
        verify(terrainServiceClient).getAllWorlds();
    }

    @Test
    void testExecuteWithException() {
        // Arrange
        when(terrainServiceClient.getAllWorlds())
            .thenThrow(new RuntimeException("Service error"));

        // Act
        ExecuteResponse response = getWorldsCommand.execute(executeRequest);

        // Assert
        assertFalse(response.isSuccess());
        assertEquals("error", response.getErrorCode());
        assertTrue(response.getMessage().contains("Failed to get worlds"));
    }
}
