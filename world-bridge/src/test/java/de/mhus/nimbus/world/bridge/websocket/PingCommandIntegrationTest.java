package de.mhus.nimbus.world.bridge.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.mhus.nimbus.shared.dto.worldwebsocket.PingCommandData;
import de.mhus.nimbus.shared.dto.worldwebsocket.WorldWebSocketCommand;
import de.mhus.nimbus.shared.dto.worldwebsocket.WorldWebSocketResponse;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.web.socket.*;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;

import java.net.URI;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PingCommandIntegrationTest {

    @LocalServerPort
    private int port;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void testPingCommandWithoutAuthentication() throws Exception {
        // Given
        URI uri = URI.create("ws://localhost:" + port + "/ws");
        CompletableFuture<WorldWebSocketResponse> responseFuture = new CompletableFuture<>();

        TestWebSocketHandler handler = new TestWebSocketHandler(responseFuture);

        // Create WebSocket client
        StandardWebSocketClient client = new StandardWebSocketClient();

        WebSocketSession webSocketSession = client.doHandshake(handler, null, uri).get(5, TimeUnit.SECONDS);

        // When - Send ping command without authentication
        Long timestamp = System.currentTimeMillis();
        WorldWebSocketCommand pingCommand = new WorldWebSocketCommand(
            "bridge", "ping", new PingCommandData(timestamp), "test-request-1");

        String commandJson = objectMapper.writeValueAsString(pingCommand);
        webSocketSession.sendMessage(new TextMessage(commandJson));

        // Then
        WorldWebSocketResponse response = responseFuture.get(5, TimeUnit.SECONDS);

        assertNotNull(response);
        assertEquals("success", response.getStatus());
        assertEquals("pong", response.getCommand());
        assertEquals("test-request-1", response.getRequestId());

        PingCommandData responseData = objectMapper.convertValue(response.getData(), PingCommandData.class);
        assertEquals(timestamp, responseData.getTimestamp());

        webSocketSession.close();
    }

    private static class TestWebSocketHandler implements WebSocketHandler {
        private final CompletableFuture<WorldWebSocketResponse> responseFuture;

        public TestWebSocketHandler(CompletableFuture<WorldWebSocketResponse> responseFuture) {
            this.responseFuture = responseFuture;
        }

        @Override
        public void afterConnectionEstablished(WebSocketSession session) {
            // Connection established
        }

        @Override
        public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
            String responseJson = (String) message.getPayload();
            ObjectMapper mapper = new ObjectMapper();
            WorldWebSocketResponse response = mapper.readValue(responseJson, WorldWebSocketResponse.class);
            responseFuture.complete(response);
        }

        @Override
        public void handleTransportError(WebSocketSession session, Throwable exception) {
            responseFuture.completeExceptionally(exception);
        }

        @Override
        public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) {
            // Connection closed
        }

        @Override
        public boolean supportsPartialMessages() {
            return false;
        }
    }
}
