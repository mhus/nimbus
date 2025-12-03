package de.mhus.nimbus.world.test.main;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
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

            // Wait for login response (may get "connected" event first from new server)
            Thread.sleep(500);

            String loginResponse = null;
            while (!messages.isEmpty()) {
                String msg = messages.poll();
                JsonNode msgNode = mapper.readTree(msg);
                String msgType = msgNode.get("t").asText();

                log.info("--- Received message type: {}", msgType);

                if ("connected".equals(msgType)) {
                    System.out.println("   (Skipping 'connected' event from new server)");
                    continue;
                }

                if ("loginResponse".equals(msgType)) {
                    loginResponse = msg;
                    break;
                }
            }

            assertThat(loginResponse).as("Should receive login response").isNotNull();

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

                // Find block at position (8, 72, 1) - should be BlockType 310
                System.out.println("\n=== Searching for Block at (8, 72, 1) ===");
                JsonNode targetBlock = null;
                for (JsonNode block : blocks) {
                    // Check both formats: direct x,y,z or position object
                    int bx = -1, by = -1, bz = -1;

                    if (block.has("position")) {
                        JsonNode pos = block.get("position");
                        bx = pos.get("x").asInt();
                        by = pos.get("y").asInt();
                        bz = pos.get("z").asInt();
                    } else if (block.has("x") && block.has("y") && block.has("z")) {
                        bx = block.get("x").asInt();
                        by = block.get("y").asInt();
                        bz = block.get("z").asInt();
                    }

                    if (bx == 8 && by == 72 && bz == 1) {
                        targetBlock = block;
                        break;
                    }
                }

                if (targetBlock == null) {
                    System.out.println("❌ Block at (8, 72, 1) NOT FOUND!");
                } else {
                    System.out.println("✅ Found block at (8, 72, 1)");
                    System.out.println("\nComplete block structure:");
                    System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(targetBlock));

                    // Validate ALL block fields
                    System.out.println("\n=== Validating ALL block fields ===");

                    boolean blockHasErrors = false;

                    // 1. Position - REQUIRED
                    if (targetBlock.has("position")) {
                        JsonNode pos = targetBlock.get("position");
                        if (pos.has("x") && pos.has("y") && pos.has("z")) {
                            int x = pos.get("x").asInt();
                            int y = pos.get("y").asInt();
                            int z = pos.get("z").asInt();
                            System.out.println("✅ position: {x:" + x + ", y:" + y + ", z:" + z + "}");
                            if (x != 8 || y != 72 || z != 1) {
                                System.out.println("❌ WRONG VALUES: Expected (8,72,1), got (" + x + "," + y + "," + z + ")");
                                blockHasErrors = true;
                            }
                        } else {
                            System.out.println("❌ position object missing x,y,z fields");
                            blockHasErrors = true;
                        }
                    } else if (targetBlock.has("x") && targetBlock.has("y") && targetBlock.has("z")) {
                        int x = targetBlock.get("x").asInt();
                        int y = targetBlock.get("y").asInt();
                        int z = targetBlock.get("z").asInt();
                        System.out.println("✅ x,y,z: (" + x + ", " + y + ", " + z + ")");
                        if (x != 8 || y != 72 || z != 1) {
                            System.out.println("❌ WRONG VALUES: Expected (8,72,1)");
                            blockHasErrors = true;
                        }
                    } else {
                        System.out.println("❌ MISSING: position or x,y,z fields");
                        blockHasErrors = true;
                    }

                    // 2. BlockTypeId - REQUIRED, must be Number 310
                    if (targetBlock.has("blockTypeId")) {
                        JsonNode typeNode = targetBlock.get("blockTypeId");
                        if (typeNode.isNumber()) {
                            int typeId = typeNode.asInt();
                            System.out.println("✅ blockTypeId: " + typeId + " (Number)");
                            if (typeId != 310) {
                                System.out.println("❌ WRONG VALUE: Expected 310, got " + typeId);
                                blockHasErrors = true;
                            }
                        } else {
                            System.out.println("❌ WRONG TYPE: blockTypeId is not a Number");
                            blockHasErrors = true;
                        }
                    } else if (targetBlock.has("t")) {
                        JsonNode typeNode = targetBlock.get("t");
                        if (typeNode.isTextual()) {
                            String type = typeNode.asText();
                            System.out.println("✅ t (type): \"" + type + "\" (String)");
                            if (!"w:310".equals(type)) {
                                System.out.println("❌ WRONG VALUE: Expected 'w:310', got '" + type + "'");
                                blockHasErrors = true;
                            }
                        } else {
                            System.out.println("❌ WRONG TYPE: t is not a String");
                            blockHasErrors = true;
                        }
                    } else {
                        System.out.println("❌ MISSING: blockTypeId or t field");
                        blockHasErrors = true;
                    }

                    // 3. Offsets - should be Array[24] with numbers
                    if (targetBlock.has("offsets")) {
                        JsonNode offsets = targetBlock.get("offsets");
                        if (offsets.isArray()) {
                            System.out.println("✅ offsets: Array[" + offsets.size() + "]");
                            if (offsets.size() != 24) {
                                System.out.println("⚠️  Expected 24 offsets, got " + offsets.size());
                            }
                            // Check all are numbers
                            boolean allNumbers = true;
                            for (JsonNode offset : offsets) {
                                if (!offset.isNumber()) {
                                    allNumbers = false;
                                    break;
                                }
                            }
                            if (!allNumbers) {
                                System.out.println("❌ Offsets contain non-number values");
                                blockHasErrors = true;
                            }
                        } else {
                            System.out.println("❌ offsets is not an array");
                            blockHasErrors = true;
                        }
                    } else if (targetBlock.has("s")) {
                        JsonNode statusNode = targetBlock.get("s");
                        if (statusNode.isNumber()) {
                            System.out.println("✅ s (status): " + statusNode.asInt() + " (Number)");
                        } else {
                            System.out.println("❌ s is not a Number");
                            blockHasErrors = true;
                        }
                    } else {
                        System.out.println("⚠️  No offsets or s field");
                    }

                    // List all fields
                    System.out.println("\nAll fields in block (8,72,1):");
                    targetBlock.fieldNames().forEachRemaining(field ->
                        System.out.println("  - " + field)
                    );

                    if (blockHasErrors) {
                        System.out.println("\n❌❌❌ BLOCK HAS ERRORS ❌❌❌");
                        assertThat(blockHasErrors).as("Block at (8,72,1) must be valid").isFalse();
                    } else {
                        System.out.println("\n✅ Block (8,72,1) is valid");
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

            // Find and validate BlockType 310 (used in first block)
            System.out.println("\n=================================================");
            System.out.println("BLOCKTYPE 310 VALIDATION (Grass Block)");
            System.out.println("=================================================");

            JsonNode blockType310 = null;
            for (JsonNode bt : blockTypes) {
                String id = bt.get("id").asText();
                if ("w:310".equals(id) || "310".equals(id)) {
                    blockType310 = bt;
                    break;
                }
            }

            if (blockType310 == null) {
                System.out.println("❌ CRITICAL ERROR: BlockType 310 NOT FOUND!");
                System.out.println("   Blocks are invisible because BlockType is missing!");
                assertThat(blockType310).as("BlockType 310 must exist").isNotNull();
                return;
            }

            System.out.println("✅ Found BlockType 310");
            System.out.println("\nComplete structure:");
            System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(blockType310));

            // Validate ALL BlockType 310 fields
            System.out.println("\n=== Validating ALL BlockType 310 fields ===");

            boolean hasErrors = false;

            // 1. id - REQUIRED
            if (blockType310.has("id")) {
                String id = blockType310.get("id").asText();
                System.out.println("✅ id: \"" + id + "\"");
                if (!"w:310".equals(id)) {
                    System.out.println("❌ WRONG VALUE: Expected 'w:310', got '" + id + "'");
                    hasErrors = true;
                }
            } else {
                System.out.println("❌ MISSING: id");
                hasErrors = true;
            }

            // 2. name - REQUIRED
            if (blockType310.has("name")) {
                System.out.println("✅ name: \"" + blockType310.get("name").asText() + "\"");
            } else {
                System.out.println("❌ MISSING: name");
                hasErrors = true;
            }

            // 3. description - REQUIRED
            if (blockType310.has("description")) {
                String desc = blockType310.get("description").asText();
                System.out.println("✅ description: \"" + desc.substring(0, Math.min(50, desc.length())) + "...\"");
            } else {
                System.out.println("❌ MISSING: description");
                hasErrors = true;
            }

            // 4. modifiers - REQUIRED
            if (!blockType310.has("modifiers")) {
                System.out.println("❌ MISSING: modifiers object");
                hasErrors = true;
            } else {
                JsonNode modifiers = blockType310.get("modifiers");
                System.out.println("✅ modifiers: Object with " + modifiers.size() + " entries");

                // 4.1. modifiers.0 (default modifier) - REQUIRED
                if (!modifiers.has("0")) {
                    System.out.println("❌ MISSING: modifiers.0 (default modifier)");
                    hasErrors = true;
                } else {
                    JsonNode mod0 = modifiers.get("0");
                    System.out.println("✅ modifiers.0: exists");

                    // 4.1.1. visibility - REQUIRED
                    if (!mod0.has("visibility")) {
                        System.out.println("❌ MISSING: modifiers.0.visibility");
                        hasErrors = true;
                    } else {
                        JsonNode visibility = mod0.get("visibility");
                        System.out.println("✅ modifiers.0.visibility: exists");

                        // 4.1.1.1. shape - REQUIRED, must be Number 1
                        if (!visibility.has("shape")) {
                            System.out.println("❌ MISSING: modifiers.0.visibility.shape");
                            hasErrors = true;
                        } else {
                            JsonNode shapeNode = visibility.get("shape");

                            // Check type - MUST be number, not string!
                            if (shapeNode.isNumber()) {
                                int shape = shapeNode.asInt();
                                System.out.println("✅ modifiers.0.visibility.shape = " + shape + " (Number)");

                                // Validate value - must be 1 for CUBE
                                if (shape != 1) {
                                    System.out.println("❌ WRONG VALUE: Expected 1 (CUBE), got " + shape);
                                    hasErrors = true;
                                }
                            } else if (shapeNode.isTextual()) {
                                String shapeStr = shapeNode.asText();
                                System.out.println("❌ WRONG TYPE: modifiers.0.visibility.shape = \"" + shapeStr + "\" (String)");
                                System.out.println("   *** THIS IS THE BUG! Client expects Number (1), not String (\"CUBE\")! ***");
                                hasErrors = true;
                            } else {
                                System.out.println("❌ WRONG TYPE: modifiers.0.visibility.shape is " + shapeNode.getNodeType());
                                hasErrors = true;
                            }
                        }

                        // 4.1.1.2. textures - REQUIRED
                        if (!visibility.has("textures")) {
                            System.out.println("❌ MISSING: modifiers.0.visibility.textures");
                            hasErrors = true;
                        } else {
                            JsonNode textures = visibility.get("textures");
                            System.out.println("✅ modifiers.0.visibility.textures: Object");

                            // texture 0 - REQUIRED
                            if (!textures.has("0")) {
                                System.out.println("❌ MISSING: modifiers.0.visibility.textures.0");
                                hasErrors = true;
                            } else {
                                JsonNode tex0 = textures.get("0");
                                if (tex0.isTextual()) {
                                    String texture0 = tex0.asText();
                                    System.out.println("✅ modifiers.0.visibility.textures.0 = \"" + texture0 + "\" (String)");
                                    if (!"textures/block/basic/grass_top.png".equals(texture0)) {
                                        System.out.println("⚠️  Expected 'textures/block/basic/grass_top.png', got '" + texture0 + "'");
                                    }
                                } else {
                                    System.out.println("❌ WRONG TYPE: texture.0 is not a String");
                                    hasErrors = true;
                                }
                            }
                        }
                    }

                    // 4.1.2. physics - OPTIONAL but expected
                    if (mod0.has("physics")) {
                        JsonNode physics = mod0.get("physics");
                        System.out.println("✅ modifiers.0.physics: exists");

                        if (physics.has("solid")) {
                            boolean solid = physics.get("solid").asBoolean();
                            System.out.println("✅ modifiers.0.physics.solid = " + solid);
                        }

                        if (physics.has("autoClimbable")) {
                            boolean autoClimb = physics.get("autoClimbable").asBoolean();
                            System.out.println("✅ modifiers.0.physics.autoClimbable = " + autoClimb);
                        }
                    }

                    // 4.1.3. audio - OPTIONAL
                    if (mod0.has("audio")) {
                        JsonNode audio = mod0.get("audio");
                        if (audio.isArray()) {
                            System.out.println("✅ modifiers.0.audio: Array[" + audio.size() + "]");
                        }
                    }
                }
            }

            if (hasErrors) {
                System.out.println("\n❌❌❌ BLOCKTYPE 310 HAS ERRORS ❌❌❌");
                System.out.println("This is why blocks are invisible in the client!");
                assertThat(hasErrors).as("BlockType 310 must be valid").isFalse();
            } else {
                System.out.println("\n✅ BlockType 310 is completely valid");
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
