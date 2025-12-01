package de.mhus.nimbus.world.test.rest;

import com.fasterxml.jackson.databind.JsonNode;
import de.mhus.nimbus.world.test.AbstractSystemTest;
import org.apache.hc.client5.http.classic.methods.*;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.junit.jupiter.api.BeforeEach;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

/**
 * Basis-Klasse für REST Editor API System-Tests.
 * Testet gegen world-editor Server der POST, PUT, DELETE unterstützt.
 */
public abstract class AbstractEditorTest extends AbstractSystemTest {

    protected String sessionId;

    @BeforeEach
    void setUpSession() throws Exception {
        // Login über WebSocket um sessionId zu erhalten
        sessionId = performWebSocketLogin();
    }

    /**
     * Führt WebSocket Login durch und gibt sessionId zurück
     */
    private String performWebSocketLogin() throws Exception {
        WebSocketClient wsClient = null;
        try {
            CompletableFuture<Void> connectionFuture = new CompletableFuture<>();
            ConcurrentLinkedQueue<String> receivedMessages = new ConcurrentLinkedQueue<>();

            wsClient = new WebSocketClient(URI.create(webSocketUrl)) {
                @Override
                public void onOpen(ServerHandshake handshake) {
                    connectionFuture.complete(null);
                }

                @Override
                public void onMessage(String message) {
                    receivedMessages.offer(message);
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    // Connection closed
                }

                @Override
                public void onError(Exception ex) {
                    connectionFuture.completeExceptionally(ex);
                }
            };

            // Connect
            wsClient.connect();
            connectionFuture.get(5, TimeUnit.SECONDS);

            // Send login message
            var loginData = objectMapper.createObjectNode();
            loginData.put("username", loginUsername);
            loginData.put("password", loginPassword);
            loginData.put("worldId", worldId);
            loginData.put("clientType", clientType);

            var message = objectMapper.createObjectNode();
            message.put("i", "editor-login");
            message.put("t", "login");
            message.set("d", loginData);

            String loginMessage = objectMapper.writeValueAsString(message);
            wsClient.send(loginMessage);

            // Wait for response
            long timeout = 5000;
            long start = System.currentTimeMillis();

            while (receivedMessages.isEmpty() &&
                   System.currentTimeMillis() - start < timeout) {
                Thread.sleep(10);
            }

            String response = receivedMessages.poll();
            if (response != null) {
                JsonNode responseNode = objectMapper.readTree(response);
                JsonNode data = responseNode.get("d");
                if (data != null && data.has("sessionId")) {
                    return data.get("sessionId").asText();
                }
            }

            throw new RuntimeException("Could not obtain sessionId from WebSocket login");

        } finally {
            if (wsClient != null && wsClient.isOpen()) {
                wsClient.close();
            }
        }
    }

    /**
     * Führt GET Request gegen Editor Server aus
     */
    protected CloseableHttpResponse performGet(String endpoint) throws IOException {
        String url = editorUrl + (endpoint.startsWith("/") ? endpoint : "/" + endpoint);

        HttpGet request = new HttpGet(url);

        // Add sessionId as Authorization Bearer Token
        if (sessionId != null) {
            request.setHeader("Authorization", "Bearer " + sessionId);
        }

        return httpClient.execute(request);
    }

    /**
     * Führt POST Request gegen Editor Server aus
     */
    protected CloseableHttpResponse performPost(String endpoint, String jsonBody) throws IOException {
        String url = editorUrl + (endpoint.startsWith("/") ? endpoint : "/" + endpoint);

        HttpPost request = new HttpPost(url);

        // Add sessionId as Authorization Bearer Token
        if (sessionId != null) {
            request.setHeader("Authorization", "Bearer " + sessionId);
        }

        if (jsonBody != null) {
            request.setEntity(new StringEntity(jsonBody));
            request.setHeader("Content-Type", "application/json");
        }

        return httpClient.execute(request);
    }

    /**
     * Führt PUT Request gegen Editor Server aus
     */
    protected CloseableHttpResponse performPut(String endpoint, String jsonBody) throws IOException {
        String url = editorUrl + (endpoint.startsWith("/") ? endpoint : "/" + endpoint);

        HttpPut request = new HttpPut(url);

        // Add sessionId as Authorization Bearer Token
        if (sessionId != null) {
            request.setHeader("Authorization", "Bearer " + sessionId);
        }

        if (jsonBody != null) {
            request.setEntity(new StringEntity(jsonBody));
            request.setHeader("Content-Type", "application/json");
        }

        return httpClient.execute(request);
    }

    /**
     * Führt DELETE Request gegen Editor Server aus
     */
    protected CloseableHttpResponse performDelete(String endpoint) throws IOException {
        String url = editorUrl + (endpoint.startsWith("/") ? endpoint : "/" + endpoint);

        HttpDelete request = new HttpDelete(url);

        // Add sessionId as Authorization Bearer Token
        if (sessionId != null) {
            request.setHeader("Authorization", "Bearer " + sessionId);
        }

        return httpClient.execute(request);
    }

    /**
     * Liest Response Body als String
     */
    protected String getResponseBody(CloseableHttpResponse response) throws IOException {
        try {
            return org.apache.hc.core5.http.io.entity.EntityUtils.toString(response.getEntity());
        } catch (org.apache.hc.core5.http.ParseException e) {
            throw new IOException("Failed to parse response body", e);
        }
    }
}
