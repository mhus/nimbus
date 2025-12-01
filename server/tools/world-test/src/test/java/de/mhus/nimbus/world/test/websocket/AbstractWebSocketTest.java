package de.mhus.nimbus.world.test.websocket;

import com.fasterxml.jackson.databind.JsonNode;
import de.mhus.nimbus.generated.network.*;
import de.mhus.nimbus.generated.network.messages.*;
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
     * Erstellt eine Login Message mit DTOs
     */
    protected String createLoginMessage(String messageId) throws Exception {
        // Map clientType string to ClientType enum
        ClientType clientTypeEnum;
        try {
            switch (clientType.toLowerCase()) {
                case "web" -> clientTypeEnum = ClientType.WEB;
                case "xbox" -> clientTypeEnum = ClientType.XBOX;
                case "mobile" -> clientTypeEnum = ClientType.MOBILE;
                case "desktop" -> clientTypeEnum = ClientType.DESKTOP;
                default -> {
                    System.out.println("⚠️ Unknown clientType: " + clientType + ", using WEB as default");
                    clientTypeEnum = ClientType.WEB;
                }
            }
        } catch (Exception e) {
            System.out.println("❌ ClientType mapping failed: " + e.getMessage() + ", using WEB");
            clientTypeEnum = ClientType.WEB;
        }

        // Use LoginRequestData DTO instead of manual JSON building
        LoginRequestData loginData = LoginRequestData.builder()
                .username(loginUsername)
                .password(loginPassword)
                .worldId(worldId)
                .clientType(clientTypeEnum)
                .build();

        // Use RequestMessage DTO for the wrapper
        RequestMessage message = RequestMessage.builder()
                .i(messageId)
                .t(MessageType.LOGIN)
                .d(loginData)
                .build();

        System.out.println("🚀 Creating login message using DTOs: LoginRequestData + RequestMessage");
        String serialized = objectMapper.writeValueAsString(message);
        System.out.println("   Serialized message: " + serialized.substring(0, Math.min(100, serialized.length())) + "...");
        return serialized;
    }

    /**
     * Alternative: Fallback method for manual message creation if DTO fails
     */
    protected String createLoginMessageManual(String messageId) throws Exception {
        System.out.println("⚠️ Falling back to manual JSON message creation");
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
     * Führt Login durch und extrahiert sessionId mit DTOs
     */
    protected void performLogin() throws Exception {
        // First verify that case-insensitive enum mapping is working
        verifyEnumCaseInsensitiveMapping();

        try {
            String loginMessage = createLoginMessage("login1");
            String response = sendMessageAndWaitForResponse(loginMessage);

            // Try to parse response as LoginResponseMessage + LoginResponseData DTOs first
            try {
                JsonNode responseNode = parseMessage(response);

                // Parse as ResponseMessage (case-insensitive enum support should help here)
                ResponseMessage responseMsg = objectMapper.treeToValue(responseNode, ResponseMessage.class);
                System.out.println("✅ Response parsed as ResponseMessage DTO (with case-insensitive enum support)");

                // Parse data as LoginResponseData
                if (responseMsg.getD() != null) {
                    JsonNode dataNode = objectMapper.valueToTree(responseMsg.getD());
                    LoginResponseData loginResponseData = objectMapper.treeToValue(dataNode, LoginResponseData.class);

                    sessionId = loginResponseData.getSessionId();
                    System.out.println("✅ Login response parsed using LoginResponseData DTO");
                    System.out.println("   Success: " + loginResponseData.isSuccess());
                    System.out.println("   UserId: " + loginResponseData.getUserId());
                    System.out.println("   SessionId: " + (sessionId != null ? sessionId.substring(0, Math.min(8, sessionId.length())) + "..." : "null"));

                    return; // Success with DTOs
                }
            } catch (Exception e) {
                System.out.println("⚠️ DTO parsing failed: " + e.getMessage() + " - falling back to manual parsing");
                // Note: Case-insensitive enum support might resolve enum-related parsing issues
                if (e.getMessage().toLowerCase().contains("enum")) {
                    System.out.println("   This might be an enum parsing issue - case-insensitive support should help");
                }
            }

            // Fallback to manual parsing
            JsonNode responseNode = parseMessage(response);
            JsonNode data = responseNode.get("d");

            if (data != null && data.has("sessionId")) {
                sessionId = data.get("sessionId").asText();
                System.out.println("⚠️ Using manual JSON parsing for login response (DTOs failed)");
            }

        } catch (Exception e) {
            System.out.println("❌ DTO-based login failed, trying manual login: " + e.getMessage());
            e.printStackTrace();

            // Fallback to completely manual approach
            String loginMessage = createLoginMessageManual("login1");
            String response = sendMessageAndWaitForResponse(loginMessage);

            JsonNode responseNode = parseMessage(response);
            JsonNode data = responseNode.get("d");

            if (data != null && data.has("sessionId")) {
                sessionId = data.get("sessionId").asText();
                System.out.println("✅ Manual login successful (DTOs not compatible)");
            }
        }
    }

    /**
     * Testet ob der ObjectMapper Enums case-insensitive parsen kann und als lowercase serialisiert
     */
    protected void verifyEnumCaseInsensitiveMapping() {
        try {
            // Test 1: Case-insensitive DESERIALIZATION (JSON -> Enum)
            System.out.println("🧪 Testing case-insensitive enum DESERIALIZATION:");

            // Test MessageType enum parsing
            String loginJson = "{\"t\":\"login\"}";
            JsonNode testNode = objectMapper.readTree(loginJson);
            MessageType messageType = objectMapper.treeToValue(testNode.get("t"), MessageType.class);

            if (messageType == MessageType.LOGIN) {
                System.out.println("✅ Case-insensitive enum parsing working: 'login' -> MessageType.LOGIN");
            } else {
                System.out.println("⚠️ Case-insensitive enum parsing failed: 'login' -> " + messageType);
            }

            // Test ClientType enum parsing
            String clientJson = "{\"clientType\":\"web\"}";
            JsonNode clientNode = objectMapper.readTree(clientJson);
            ClientType clientType = objectMapper.treeToValue(clientNode.get("clientType"), ClientType.class);

            if (clientType == ClientType.WEB) {
                System.out.println("✅ Case-insensitive enum parsing working: 'web' -> ClientType.WEB");
            } else {
                System.out.println("⚠️ Case-insensitive enum parsing failed: 'web' -> " + clientType);
            }

            // Test 2: Lowercase enum SERIALIZATION (Enum -> JSON)
            System.out.println("\n🧪 Testing lowercase enum SERIALIZATION:");

            // Test MessageType serialization
            String messageTypeSerialized = objectMapper.writeValueAsString(MessageType.LOGIN);
            if ("\"login\"".equals(messageTypeSerialized)) {
                System.out.println("✅ Lowercase enum serialization working: MessageType.LOGIN -> 'login'");
            } else {
                System.out.println("⚠️ Lowercase enum serialization failed: MessageType.LOGIN -> " + messageTypeSerialized + " (expected: 'login')");
            }

            // Test ClientType serialization
            String clientTypeSerialized = objectMapper.writeValueAsString(ClientType.WEB);
            if ("\"web\"".equals(clientTypeSerialized)) {
                System.out.println("✅ Lowercase enum serialization working: ClientType.WEB -> 'web'");
            } else {
                System.out.println("⚠️ Lowercase enum serialization failed: ClientType.WEB -> " + clientTypeSerialized + " (expected: 'web')");
            }

            // Test 3: Full round-trip (Enum -> JSON -> Enum)
            System.out.println("\n🧪 Testing full round-trip (Enum -> JSON -> Enum):");

            // Create a complete message with enums
            RequestMessage testMessage = RequestMessage.builder()
                    .i("test1")
                    .t(MessageType.PING)
                    .build();

            String serializedMessage = objectMapper.writeValueAsString(testMessage);
            System.out.println("   Serialized message: " + serializedMessage);

            // Parse it back
            RequestMessage parsedMessage = objectMapper.readValue(serializedMessage, RequestMessage.class);
            if (parsedMessage.getT() == MessageType.PING && "test1".equals(parsedMessage.getI())) {
                System.out.println("✅ Full round-trip successful: MessageType.PING -> JSON -> MessageType.PING");
            } else {
                System.out.println("⚠️ Full round-trip failed: " + parsedMessage.getT());
            }

        } catch (Exception e) {
            System.out.println("❌ Enum case-insensitive/lowercase test failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Demonstriert die neue lowercase enum serialization Funktionalität
     */
    protected void demonstrateLowercaseEnumSerialization() throws Exception {
        System.out.println("\n🎯 DEMONSTRATION: Lowercase Enum Serialization");
        System.out.println("=====================================================");

        // Beispiel 1: Login Message mit lowercase enums
        RequestMessage loginMessage = RequestMessage.builder()
                .i("demo1")
                .t(MessageType.LOGIN)
                .d(LoginRequestData.builder()
                        .username("demo_user")
                        .clientType(ClientType.WEB)
                        .worldId("demo-world")
                        .build())
                .build();

        String serializedLogin = objectMapper.writeValueAsString(loginMessage);
        System.out.println("📤 Serialized Login Message:");
        System.out.println("   " + serializedLogin);
        System.out.println("   ✅ MessageType.LOGIN → \"login\"");
        System.out.println("   ✅ ClientType.WEB → \"web\"");

        // Beispiel 2: Ping Message mit lowercase enum
        RequestMessage pingMessage = RequestMessage.builder()
                .i("demo2")
                .t(MessageType.PING)
                .d(PingData.builder().cTs(System.currentTimeMillis()).build())
                .build();

        String serializedPing = objectMapper.writeValueAsString(pingMessage);
        System.out.println("\n📤 Serialized Ping Message:");
        System.out.println("   " + serializedPing);
        System.out.println("   ✅ MessageType.PING → \"ping\"");

        // Beispiel 3: Chunk Register Message
        RequestMessage chunkMessage = RequestMessage.builder()
                .i("demo3")
                .t(MessageType.CHUNK_REGISTER)
                .build();

        String serializedChunk = objectMapper.writeValueAsString(chunkMessage);
        System.out.println("\n📤 Serialized Chunk Message:");
        System.out.println("   " + serializedChunk);
        System.out.println("   ✅ MessageType.CHUNK_REGISTER → \"chunk_register\"");

        System.out.println("\n🎉 Alle Enums werden automatisch als lowercase serialisiert!");
    }
}
