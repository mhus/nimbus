package de.mhus.nimbus.world.bridge.command.world;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.mhus.nimbus.shared.dto.command.GetWorldCommandData;
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
class GetWorldCommandTest {

    @Mock
    private TerrainServiceClient terrainServiceClient;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private GetWorldCommand getWorldCommand;

    private ExecuteRequest executeRequest;
    private GetWorldCommandData commandData;

    @BeforeEach
    void setUp() {
        executeRequest = mock(ExecuteRequest.class);
        commandData = new GetWorldCommandData();
        commandData.setWorldId("world-123");
    }

    @Test
    void testInfo() {
        WebSocketCommandInfo info = getWorldCommand.info();

        assertEquals("terrain", info.getService());
        assertEquals("getWorld", info.getCommand());
        assertEquals("Get world by ID", info.getDescription());
        assertTrue(info.isWorldRequired());
    }

    @Test
    void testExecuteSuccess() {
        // Arrange
        WorldDto worldDto = new WorldDto();
        worldDto.setId("world-123");
        worldDto.setName("Test World");

        when(objectMapper.convertValue(any(), eq(GetWorldCommandData.class)))
            .thenReturn(commandData);
        when(terrainServiceClient.getWorld("world-123")).thenReturn(Optional.of(worldDto));

        // Act
        ExecuteResponse response = getWorldCommand.execute(executeRequest);

        // Assert
        assertTrue(response.isSuccess());
        assertEquals(worldDto, response.getData());
        verify(terrainServiceClient).getWorld("world-123");
    }

    @Test
    void testExecuteWorldNotFound() {
        // Arrange
        when(objectMapper.convertValue(any(), eq(GetWorldCommandData.class)))
            .thenReturn(commandData);
        when(terrainServiceClient.getWorld("world-123")).thenReturn(Optional.empty());

        // Act
        ExecuteResponse response = getWorldCommand.execute(executeRequest);

        // Assert
        assertFalse(response.isSuccess());
        assertEquals("not_found", response.getErrorCode());
        assertEquals("World not found", response.getMessage());
    }

    @Test
    void testExecuteWithNullWorldId() {
        // Arrange
        GetWorldCommandData emptyData = new GetWorldCommandData();
        emptyData.setWorldId(null);

        when(objectMapper.convertValue(any(), eq(GetWorldCommandData.class)))
            .thenReturn(emptyData);

        // Act
        ExecuteResponse response = getWorldCommand.execute(executeRequest);

        // Assert
        assertFalse(response.isSuccess());
        assertEquals("error", response.getErrorCode());
        assertEquals("World ID is required", response.getMessage());
        verify(terrainServiceClient, never()).getWorld(any());
    }

    @Test
    void testExecuteWithEmptyWorldId() {
        // Arrange
        GetWorldCommandData emptyData = new GetWorldCommandData();
        emptyData.setWorldId("");

        when(objectMapper.convertValue(any(), eq(GetWorldCommandData.class)))
            .thenReturn(emptyData);

        // Act
        ExecuteResponse response = getWorldCommand.execute(executeRequest);

        // Assert
        assertFalse(response.isSuccess());
        assertEquals("error", response.getErrorCode());
        assertEquals("World ID is required", response.getMessage());
        verify(terrainServiceClient, never()).getWorld(any());
    }
}
