package de.mhus.nimbus.world.test.websocket;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

/**
 * WebSocket System-Tests für Login und grundlegende Nachrichten.
 * Diese Tests müssen in der angegebenen Reihenfolge ausgeführt werden.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("WebSocket Login und Grundfunktionen")
class WebSocketLoginTest extends AbstractWebSocketTest {

    @Test
    @Order(1)
    @DisplayName("Login mit username/password sollte erfolgreich sein")
    void shouldLoginSuccessfully() throws Exception {
        // Given
        String loginMessage = createLoginMessage("1");

        // When
        String response = sendMessageAndWaitForResponse(loginMessage);

        // Then
        assertThat(response).isNotNull();

        JsonNode responseNode = parseMessage(response);
        assertThat(responseNode.get("r").asText()).isEqualTo("1");
        assertThat(responseNode.get("t").asText()).isEqualTo("loginResponse");

        JsonNode data = responseNode.get("d");
        assertThat(data.get("success").asBoolean()).isTrue();
        assertThat(data.has("sessionId")).isTrue();
        assertThat(data.has("userId")).isTrue();
        assertThat(data.has("worldInfo")).isTrue();

        // Store sessionId for subsequent tests
        sessionId = data.get("sessionId").asText();
        assertThat(sessionId).isNotEmpty();

        // Verify worldInfo structure
        JsonNode worldInfo = data.get("worldInfo");
        assertThat(worldInfo.get("worldId").asText()).isEqualTo(worldId);
        assertThat(worldInfo.has("name")).isTrue();
        assertThat(worldInfo.has("chunkSize")).isTrue();
        assertThat(worldInfo.has("start")).isTrue();
        assertThat(worldInfo.has("stop")).isTrue();
    }

    @Test
    @Order(2)
    @DisplayName("Login mit falschen Credentials sollte fehlschlagen")
    void shouldFailLoginWithInvalidCredentials() throws Exception {
        // Given
        var loginData = objectMapper.createObjectNode();
        loginData.put("username", "invaliduser");
        loginData.put("password", "wrongpass");
        loginData.put("worldId", worldId);
        loginData.put("clientType", clientType);

        var message = objectMapper.createObjectNode();
        message.put("i", "2");
        message.put("t", "login");
        message.set("d", loginData);

        String loginMessage = objectMapper.writeValueAsString(message);

        // When
        String response = sendMessageAndWaitForResponse(loginMessage);

        // Then
        assertThat(response).isNotNull();

        JsonNode responseNode = parseMessage(response);
        assertThat(responseNode.get("r").asText()).isEqualTo("2");
        assertThat(responseNode.get("t").asText()).isEqualTo("loginResponse");

        JsonNode data = responseNode.get("d");
        assertThat(data.get("success").asBoolean()).isFalse();
        assertThat(data.has("errorCode")).isTrue();
        assertThat(data.has("errorMessage")).isTrue();
    }

    @Test
    @Order(3)
    @DisplayName("Ping sollte Pong zurückgeben")
    void shouldRespondToPing() throws Exception {
        // Given - Login first
        performLogin();

        long clientTimestamp = System.currentTimeMillis();

        var pingData = objectMapper.createObjectNode();
        pingData.put("cTs", clientTimestamp);

        var message = objectMapper.createObjectNode();
        message.put("i", "ping1");
        message.put("t", "p");
        message.set("d", pingData);

        String pingMessage = objectMapper.writeValueAsString(message);

        // When
        String response = sendMessageAndWaitForResponse(pingMessage);

        // Then
        assertThat(response).isNotNull();

        JsonNode responseNode = parseMessage(response);
        assertThat(responseNode.get("r").asText()).isEqualTo("ping1");
        assertThat(responseNode.get("t").asText()).isEqualTo("p");

        JsonNode data = responseNode.get("d");
        assertThat(data.get("cTs").asLong()).isEqualTo(clientTimestamp);
        assertThat(data.has("sTs")).isTrue();

        long serverTimestamp = data.get("sTs").asLong();
        assertThat(serverTimestamp).isGreaterThan(clientTimestamp);
    }

    @Test
    @Order(4)
    @DisplayName("Login mit existierender sessionId sollte funktionieren")
    void shouldLoginWithExistingSessionId() throws Exception {
        // Given - First login to get sessionId
        performLogin();
        String existingSessionId = sessionId;

        // Close and reconnect
        tearDownWebSocket();
        setUpWebSocket();

        // Create login with existing sessionId
        var loginData = objectMapper.createObjectNode();
        loginData.put("username", loginUsername);
        loginData.put("password", loginPassword);
        loginData.put("worldId", worldId);
        loginData.put("clientType", clientType);
        loginData.put("sessionId", existingSessionId);

        var message = objectMapper.createObjectNode();
        message.put("i", "4");
        message.put("t", "login");
        message.set("d", loginData);

        String loginMessage = objectMapper.writeValueAsString(message);

        // When
        String response = sendMessageAndWaitForResponse(loginMessage);

        // Then
        assertThat(response).isNotNull();

        JsonNode responseNode = parseMessage(response);
        JsonNode data = responseNode.get("d");

        assertThat(data.get("success").asBoolean()).isTrue();
        assertThat(data.has("sessionId")).isTrue();

        // SessionId might be same or new one
        String newSessionId = data.get("sessionId").asText();
        assertThat(newSessionId).isNotEmpty();
    }
}
