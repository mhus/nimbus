package de.mhus.nimbus.world.test.websocket;

import com.fasterxml.jackson.databind.JsonNode;
import de.mhus.nimbus.world.test.AbstractSystemTest;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import java.net.URI;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

/**
 * Basis-Klasse für WebSocket System-Tests gegen externen test_server.
 */
public abstract class AbstractWebSocketTest extends AbstractSystemTest {

    protected WebSocketClient webSocketClient;
    protected final ConcurrentLinkedQueue<String> receivedMessages = new ConcurrentLinkedQueue<>();
    protected CompletableFuture<Void> connectionFuture;
    protected String sessionId;

    @BeforeEach
    void setUpWebSocket() throws Exception {
        connectionFuture = new CompletableFuture<>();

        webSocketClient = new WebSocketClient(URI.create(webSocketUrl)) {
            @Override
            public void onOpen(ServerHandshake handshake) {
                System.out.println("WebSocket connection opened");
                connectionFuture.complete(null);
            }

            @Override
            public void onMessage(String message) {
                System.out.println("Received message: " + message);
                receivedMessages.offer(message);
            }

            @Override
            public void onClose(int code, String reason, boolean remote) {
                System.out.println("WebSocket connection closed: " + reason);
            }

            @Override
            public void onError(Exception ex) {
                System.err.println("WebSocket error: " + ex.getMessage());
                connectionFuture.completeExceptionally(ex);
            }
        };

        // Connect to WebSocket
        webSocketClient.connect();

        // Wait for connection with timeout
        connectionFuture.get(getIntProperty("test.websocket.connect.timeout", 5000), TimeUnit.MILLISECONDS);

        // Clear any initial messages
        receivedMessages.clear();
    }

    @AfterEach
    void tearDownWebSocket() {
        if (webSocketClient != null && webSocketClient.isOpen()) {
            webSocketClient.close();
        }
    }

    /**
     * Sendet eine Message und wartet auf Response
     */
    protected String sendMessageAndWaitForResponse(String message) throws Exception {
        int messagesBefore = receivedMessages.size();
        webSocketClient.send(message);

        // Wait for response
        long timeout = getIntProperty("test.websocket.message.timeout", 3000);
        long start = System.currentTimeMillis();

        while (receivedMessages.size() <= messagesBefore &&
               System.currentTimeMillis() - start < timeout) {
            Thread.sleep(10);
        }

        return receivedMessages.poll();
    }

    /**
     * Parsed eine JSON Message
     */
    protected JsonNode parseMessage(String message) throws Exception {
        return objectMapper.readTree(message);
    }

    /**
     * Erstellt eine Login Message
     */
    protected String createLoginMessage(String messageId) throws Exception {
        var loginData = objectMapper.createObjectNode();
        loginData.put("username", loginUsername);
        loginData.put("password", loginPassword);
        loginData.put("worldId", worldId);
        loginData.put("clientType", clientType);

        var message = objectMapper.createObjectNode();
        message.put("i", messageId);
        message.put("t", "login");
        message.set("d", loginData);

        return objectMapper.writeValueAsString(message);
    }

    /**
     * Führt Login durch und extrahiert sessionId
     */
    protected void performLogin() throws Exception {
        String loginMessage = createLoginMessage("login1");
        String response = sendMessageAndWaitForResponse(loginMessage);

        JsonNode responseNode = parseMessage(response);
        JsonNode data = responseNode.get("d");

        if (data != null && data.has("sessionId")) {
            sessionId = data.get("sessionId").asText();
        }
    }
}
