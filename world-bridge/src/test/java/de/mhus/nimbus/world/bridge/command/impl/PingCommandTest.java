package de.mhus.nimbus.world.bridge.command.impl;

import de.mhus.nimbus.shared.dto.worldwebsocket.PingCommandData;
import de.mhus.nimbus.shared.dto.worldwebsocket.WorldWebSocketCommand;
import de.mhus.nimbus.world.bridge.command.ExecuteRequest;
import de.mhus.nimbus.world.bridge.command.ExecuteResponse;
import de.mhus.nimbus.world.bridge.command.WebSocketCommandInfo;
import de.mhus.nimbus.world.bridge.model.WebSocketSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PingCommandTest {

    private PingCommand pingCommand;
    private WebSocketSession testSession;
    private WorldWebSocketCommand testCommand;

    @BeforeEach
    void setUp() {
        pingCommand = new PingCommand();
        testSession = new WebSocketSession();
        testCommand = new WorldWebSocketCommand("bridge", "ping", null, "req-1");
    }

    @Test
    void testInfo() {
        // When
        WebSocketCommandInfo info = pingCommand.info();

        // Then
        assertEquals("bridge", info.getService());
        assertEquals("ping", info.getCommand());
        assertEquals("Test connection with pong response", info.getDescription());
    }

    @Test
    void testExecute() {
        // Given
        Long timestamp = System.currentTimeMillis();
        PingCommandData pingData = new PingCommandData(timestamp);
        testCommand.setData(pingData);
        ExecuteRequest request = new ExecuteRequest("session-1", testSession, testCommand);

        // When
        ExecuteResponse response = pingCommand.execute(request);

        // Then
        assertTrue(response.isSuccess());
        assertEquals("success", response.getResponse().getStatus());
        assertEquals("pong", response.getResponse().getCommand());

        PingCommandData responseData = (PingCommandData) response.getResponse().getData();
        assertEquals(timestamp, responseData.getTimestamp());
    }
}
