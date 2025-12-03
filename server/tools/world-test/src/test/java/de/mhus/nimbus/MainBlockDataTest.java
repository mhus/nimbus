package de.mhus.nimbus;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration Test für World 'main' - JSON Struktur Validierung.
 *
 * Prüft Block-Daten (Chunks) und BlockTypes vom test_server.
 * Nur plain JSON mit Jackson, keine DTOs.
 * Kann später gegen neuen Server laufen um Unterschiede zu identifizieren.
 */
@DisplayName("Main World Block Data Structure Test")
public class MainBlockDataTest extends AbstractSystemTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    @DisplayName("Load and validate chunk and blocktype structures from test_server")
    void validateMainWorldDataStructures() throws Exception {
        System.out.println("\n=================================================");
        System.out.println("MAIN WORLD BLOCK DATA TEST");
        System.out.println("Server: " + webSocketUrl + " / " + playerUrl);
        System.out.println("World: main");
        System.out.println("=================================================\n");

        // Setup WebSocket
        CompletableFuture<Void> connected = new CompletableFuture<>();
        ConcurrentLinkedQueue<String> messages = new ConcurrentLinkedQueue<>();

        WebSocketClient ws = new WebSocketClient(URI.create(webSocketUrl)) {
            @Override
            public void onOpen(ServerHandshake handshake) {
                System.out.println("✅ WebSocket connected");
                connected.complete(null);
            }

            @Override
            public void onMessage(String message) {
                System.out.println("   [WS] Received: " + message.substring(0, Math.min(100, message.length())) + "...");
                messages.offer(message);
            }

            @Override
            public void onClose(int code, String reason, boolean remote) {
                System.out.println("   [WS] Closed: code=" + code + ", reason=" + reason + ", remote=" + remote);
            }

            @Override
            public void onError(Exception ex) {
                System.err.println("   [WS] Error: " + ex.getMessage());
                ex.printStackTrace();
                connected.completeExceptionally(ex);
            }
        };

        try {
            // ===== STEP 1: Connect and Login =====
            System.out.println("=== Step 1: Login ===");
            ws.connect();
            connected.get(5, TimeUnit.SECONDS);

            worldId = "main"; //XXX
            clientType = "web"; //XXX

            // Send login
            ObjectNode loginData = mapper.createObjectNode();
            loginData.put("username", loginUsername);
            loginData.put("password", loginPassword);
            loginData.put("worldId", worldId);
            loginData.put("clientType", clientType);

            ObjectNode loginMsg = mapper.createObjectNode();
            loginMsg.put("i", "login1");
            loginMsg.put("t", "login");
            loginMsg.set("d", loginData);

            ws.send(mapper.writeValueAsString(loginMsg));

            // Wait for login response
            Thread.sleep(500);
            String loginResponse = messages.poll();
            assertThat(loginResponse).isNotNull();

            JsonNode loginNode = mapper.readTree(loginResponse);
            String sessionId = loginNode.get("d").get("sessionId").asText();
            System.out.println("✅ Login successful, sessionId: " + sessionId);

            // ===== STEP 2: Register Chunk 0,0 and collect ALL responses =====
            System.out.println("\n=== Step 2: Register Chunk 0,0 ===");
            // Don't clear - keep collecting all messages

            ObjectNode regData = mapper.createObjectNode();
            var regChunks = mapper.createArrayNode();
            ObjectNode chunk = mapper.createObjectNode();
            {
                chunk.put("cx", 0);  // chunk coordinates, not block coordinates!
                chunk.put("cz", 0);
                regChunks.add(chunk);
            }
            regData.set("c", regChunks);

            // Send message WITHOUT wrapper, WITHOUT message id (chunk registration has no "i" field)
            ObjectNode regMsg = mapper.createObjectNode();
            regMsg.put("t", "c.r");
            regMsg.set("d", regData);

            String regMsgStr = mapper.writeValueAsString(regMsg);
            System.out.println("   Sending: " + regMsgStr);
            ws.send(regMsgStr);
            System.out.println("   Waiting for responses...");

            // Wait for all messages (entities + chunks)
            Thread.sleep(1000);

            System.out.println("   Received " + messages.size() + " messages");
            System.out.println("   WebSocket still open: " + ws.isOpen());

            // Find chunk data (type "c.u")
            JsonNode chunkData = null;
            for (String msg : messages) {
                JsonNode msgNode = mapper.readTree(msg);
                String msgType = msgNode.get("t").asText();
                System.out.println("   Message type: " + msgType);

                if ("c.u".equals(msgType)) {
                    // Chunk update message
                    JsonNode dataArray = msgNode.get("d");
                    if (dataArray != null && dataArray.isArray() && dataArray.size() > 0) {
                        JsonNode firstChunk = dataArray.get(0);
                        if (firstChunk.has("cx") && firstChunk.has("cz")) {
                            chunkData = firstChunk;
                            System.out.println("   ✅ Found chunk data: cx=" + firstChunk.get("cx") +
                                             ", cz=" + firstChunk.get("cz"));
                            break;
                        }
                    }
                }
            }

            assertThat(chunkData).as("Should receive chunk data from server").isNotNull();

            // ===== STEP 3: Load BlockTypes via REST =====
            System.out.println("\n=== Step 3: Load BlockTypes group 'w' ===");
            JsonNode blockTypes;
            try (CloseableHttpResponse response = performGet("/api/worlds/main/blocktypeschunk/w")) {
                assertThat(response.getCode()).isEqualTo(200);
                String body = EntityUtils.toString(response.getEntity());
                blockTypes = mapper.readTree(body);
                assertThat(blockTypes.isArray()).isTrue();
                System.out.println("✅ Loaded " + blockTypes.size() + " BlockTypes");
            }

            // ===== STEP 4: Validate CHUNK Structure =====
            System.out.println("\n=================================================");
            System.out.println("CHUNK STRUCTURE (Chunk 0,0)");
            System.out.println("=================================================");

            final JsonNode finalChunk = chunkData;
            System.out.println("Fields:");
            chunkData.fieldNames().forEachRemaining(field -> {
                JsonNode value = finalChunk.get(field);
                if (value.isArray()) {
                    System.out.println("  - " + field + ": Array[" + value.size() + "]");
                } else if (value.isObject()) {
                    System.out.println("  - " + field + ": Object{...}");
                } else {
                    System.out.println("  - " + field + ": " + value);
                }
            });

            // Check essential chunk fields
            assertThat(chunkData.has("cx")).as("Chunk needs cx field").isTrue();
            assertThat(chunkData.has("cz")).as("Chunk needs cz field").isTrue();

            // Check blocks field (could be 'b' or 'blocks')
            String blocksField = chunkData.has("b") ? "b" : "blocks";
            assertThat(chunkData.has(blocksField)).as("Chunk needs blocks field").isTrue();

            JsonNode blocks = chunkData.get(blocksField);
            assertThat(blocks.isArray()).isTrue();
            System.out.println("\n✅ Blocks field: '" + blocksField + "' with " + blocks.size() + " blocks");

            // Print first block structure
            if (blocks.size() > 0) {
                JsonNode firstBlock = blocks.get(0);
                System.out.println("\nFirst block structure:");
                firstBlock.fieldNames().forEachRemaining(field ->
                    System.out.println("  - " + field + ": " + firstBlock.get(field))
                );

                // Find block at position 0,0 (any y)
                System.out.println("\nSearching for block at x:0, z:0...");
                for (JsonNode block : blocks) {
                    if (block.has("x") && block.has("z")) {
                        int x = block.get("x").asInt();
                        int z = block.get("z").asInt();
                        if (x == 0 && z == 0) {
                            System.out.println("Found block at x:0, z:0, y:" + block.get("y").asInt());
                            System.out.println("  Type: " + (block.has("t") ? block.get("t").asText() : "N/A"));
                            System.out.println("  Status: " + (block.has("s") ? block.get("s").asInt() : "N/A"));
                            break;
                        }
                    }
                }
            }

            // ===== STEP 5: Validate BLOCKTYPE Structure =====
            System.out.println("\n=================================================");
            System.out.println("BLOCKTYPE STRUCTURE (Group 'w')");
            System.out.println("=================================================");
            System.out.println("Count: " + blockTypes.size());

            JsonNode firstType = blockTypes.get(0);
            System.out.println("\nFirst BlockType fields:");
            firstType.fieldNames().forEachRemaining(field -> {
                JsonNode value = firstType.get(field);
                if (value.isArray()) {
                    System.out.println("  - " + field + ": Array[" + value.size() + "]");
                } else if (value.isObject()) {
                    System.out.println("  - " + field + ": Object{...}");
                } else {
                    String str = value.asText();
                    if (str.length() > 60) str = str.substring(0, 60) + "...";
                    System.out.println("  - " + field + ": " + str);
                }
            });

            assertThat(firstType.has("id")).as("BlockType needs id field").isTrue();
            System.out.println("\n✅ First BlockType ID: " + firstType.get("id").asText());

            // Sample some BlockType IDs
            System.out.println("\nSample BlockType IDs:");
            for (int i = 0; i < Math.min(5, blockTypes.size()); i++) {
                System.out.println("  - " + blockTypes.get(i).get("id").asText());
            }

            // ===== FINAL SUMMARY =====
            System.out.println("\n=================================================");
            System.out.println("TEST RESULT: SUCCESS");
            System.out.println("=================================================");
            System.out.println("✅ Chunk 0,0: " + blocks.size() + " blocks");
            System.out.println("✅ BlockTypes group 'w': " + blockTypes.size() + " types");
            System.out.println("✅ All structures validated");
            System.out.println("=================================================\n");

        } finally {
            if (ws.isOpen()) {
                ws.close();
            }
        }
    }

    private CloseableHttpResponse performGet(String endpoint) throws IOException {
        String url = playerUrl + endpoint;
        HttpGet request = new HttpGet(url);
        return httpClient.execute(request);
    }
}
