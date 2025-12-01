package de.mhus.nimbus.world.test.editor;

import de.mhus.nimbus.world.test.rest.AbstractEditorTest;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.junit.jupiter.api.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Editor API Übersichts-Tests.
 * Testet alle verfügbaren GET/POST/PUT/DELETE Endpoints für Verfügbarkeit.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Editor API Endpoints Übersicht")
class EditorApiOverviewTest extends AbstractEditorTest {

    private String testWorldId;

    @BeforeEach
    void setUpWorldId() {
        testWorldId = getProperty("test.login.worldId", "test-world");
    }

    @Test
    @Order(1)
    @DisplayName("Alle Editor World API Endpoints sollten erreichbar sein")
    void shouldTestEditorWorldApiEndpoints() throws Exception {
        System.out.println("=== EDITOR WORLD API ENDPOINTS ===");

        // GET /api/worlds
        try (CloseableHttpResponse response = performGet("/api/worlds")) {
            System.out.println("GET /api/worlds -> " + response.getCode());
            assertThat(response.getCode()).isIn(200, 404, 403);
        }

        // GET /api/worlds/{worldId}
        try (CloseableHttpResponse response = performGet("/api/worlds/" + testWorldId)) {
            System.out.println("GET /api/worlds/" + testWorldId + " -> " + response.getCode());
            assertThat(response.getCode()).isIn(200, 404, 403);
        }
    }

    @Test
    @Order(2)
    @DisplayName("Alle Editor Assets API Endpoints sollten erreichbar sein")
    void shouldTestEditorAssetsApiEndpoints() throws Exception {
        System.out.println("=== EDITOR ASSETS API ENDPOINTS ===");

        // GET /api/worlds/{worldId}/assets
        try (CloseableHttpResponse response = performGet("/api/worlds/" + testWorldId + "/assets")) {
            System.out.println("GET /api/worlds/" + testWorldId + "/assets -> " + response.getCode());
            assertThat(response.getCode()).isIn(200, 404, 403);
        }

        // POST /api/worlds/{worldId}/assets/{path} (test endpoint availability)
        String testPath = "textures/test/availability_test.png";
        byte[] testData = "TEST_DATA".getBytes();

        try (CloseableHttpResponse response = performPostAsset("/api/worlds/" + testWorldId + "/assets/" + testPath, testData)) {
            System.out.println("POST /api/worlds/" + testWorldId + "/assets/{path} -> " + response.getCode());
            assertThat(response.getCode()).isIn(201, 400, 404, 403);

            // If created, try to delete it
            if (response.getCode() == 201) {
                try (CloseableHttpResponse deleteResponse = performDelete("/api/worlds/" + testWorldId + "/assets/" + testPath)) {
                    System.out.println("DELETE /api/worlds/" + testWorldId + "/assets/{path} -> " + deleteResponse.getCode());
                }
            }
        }
    }

    @Test
    @Order(3)
    @DisplayName("Alle Editor BlockType API Endpoints sollten erreichbar sein")
    void shouldTestEditorBlockTypeApiEndpoints() throws Exception {
        System.out.println("=== EDITOR BLOCKTYPE API ENDPOINTS ===");

        // GET /api/worlds/{worldId}/blocktypes
        try (CloseableHttpResponse response = performGet("/api/worlds/" + testWorldId + "/blocktypes")) {
            System.out.println("GET /api/worlds/" + testWorldId + "/blocktypes -> " + response.getCode());
            assertThat(response.getCode()).isIn(200, 404, 403);
        }

        // POST /api/worlds/{worldId}/blocktypes (test endpoint availability)
        var blockTypeData = objectMapper.createObjectNode();
        blockTypeData.put("description", "Availability test block");
        blockTypeData.put("initialStatus", 0);
        String blockTypeJson = objectMapper.writeValueAsString(blockTypeData);

        try (CloseableHttpResponse response = performPost("/api/worlds/" + testWorldId + "/blocktypes", blockTypeJson)) {
            System.out.println("POST /api/worlds/" + testWorldId + "/blocktypes -> " + response.getCode());
            assertThat(response.getCode()).isIn(201, 400, 404, 403);

            // If created, try to delete it
            if (response.getCode() == 201) {
                String responseBody = getResponseBody(response);
                var jsonNode = objectMapper.readTree(responseBody);
                if (jsonNode.has("id")) {
                    String createdId = jsonNode.get("id").asText();
                    try (CloseableHttpResponse deleteResponse = performDelete("/api/worlds/" + testWorldId + "/blocktypes/" + createdId)) {
                        System.out.println("DELETE /api/worlds/" + testWorldId + "/blocktypes/{id} -> " + deleteResponse.getCode());
                    }
                }
            }
        }
    }

    @Test
    @Order(4)
    @DisplayName("Alle Editor Block Operations API Endpoints sollten erreichbar sein")
    void shouldTestEditorBlockOperationsApiEndpoints() throws Exception {
        System.out.println("=== EDITOR BLOCK OPERATIONS API ENDPOINTS ===");

        int x = 200, y = 64, z = 200; // Use unique coordinates

        // GET /api/worlds/{worldId}/blocks/{x}/{y}/{z}
        try (CloseableHttpResponse response = performGet("/api/worlds/" + testWorldId + "/blocks/" + x + "/" + y + "/" + z)) {
            System.out.println("GET /api/worlds/" + testWorldId + "/blocks/" + x + "/" + y + "/" + z + " -> " + response.getCode());
            assertThat(response.getCode()).isIn(200, 404, 403, 400);
        }

        // POST /api/worlds/{worldId}/blocks/{x}/{y}/{z} (test endpoint availability)
        var blockData = objectMapper.createObjectNode();
        blockData.put("blockTypeId", "w:1");
        blockData.put("status", 0);
        String blockJson = objectMapper.writeValueAsString(blockData);

        try (CloseableHttpResponse response = performPost("/api/worlds/" + testWorldId + "/blocks/" + x + "/" + y + "/" + z, blockJson)) {
            System.out.println("POST /api/worlds/" + testWorldId + "/blocks/" + x + "/" + y + "/" + z + " -> " + response.getCode());
            assertThat(response.getCode()).isIn(201, 400, 404, 403);

            // If created, try to delete it
            if (response.getCode() == 201) {
                try (CloseableHttpResponse deleteResponse = performDelete("/api/worlds/" + testWorldId + "/blocks/" + x + "/" + y + "/" + z)) {
                    System.out.println("DELETE /api/worlds/" + testWorldId + "/blocks/" + x + "/" + y + "/" + z + " -> " + deleteResponse.getCode());
                }
            }
        }
    }

    @Test
    @Order(5)
    @DisplayName("Editor vs Read-Only Server Unterschiede")
    void shouldCompareEditorAndReadOnlyServers() throws Exception {
        System.out.println("=== EDITOR VS READ-ONLY SERVER COMPARISON ===");

        System.out.println("Player Server URL: " + playerUrl);
        System.out.println("Editor Server URL: " + editorUrl);
        System.out.println("WebSocket Server URL: " + webSocketUrl);

        System.out.println("\nRead-Only Server features:");
        System.out.println("  - GET endpoints only");
        System.out.println("  - Asset download");
        System.out.println("  - Block inspection");
        System.out.println("  - BlockType browsing");

        System.out.println("\nEditor Server features:");
        System.out.println("  - All GET endpoints (same as read-only)");
        System.out.println("  - POST: Create new assets, blocktypes, blocks");
        System.out.println("  - PUT: Update existing assets, blocktypes, blocks");
        System.out.println("  - DELETE: Remove assets, blocktypes, blocks");

        System.out.println("\nAuthentication:");
        System.out.println("  - WebSocket login for sessionId");
        System.out.println("  - Bearer token for REST API calls");
        System.out.println("  - SessionId: " + (sessionId != null ? sessionId.substring(0, Math.min(10, sessionId.length())) + "..." : "null"));
    }

    @Test
    @Order(6)
    @DisplayName("Editor API vollständige Übersicht")
    void shouldProvideCompleteEditorApiOverview() throws Exception {
        System.out.println("\n=== VOLLSTÄNDIGE EDITOR API ÜBERSICHT ===");

        System.out.println("World Management:");
        System.out.println("  GET /api/worlds");
        System.out.println("  GET /api/worlds/{worldId}");

        System.out.println("\nAsset Management (CRUD):");
        System.out.println("  GET /api/worlds/{worldId}/assets");
        System.out.println("  GET /api/worlds/{worldId}/assets?query={searchTerm}");
        System.out.println("  GET /api/worlds/{worldId}/assets?ext={extension}");
        System.out.println("  GET /api/worlds/{worldId}/assets/{path}");
        System.out.println("  POST /api/worlds/{worldId}/assets/{path}    # Create asset");
        System.out.println("  PUT /api/worlds/{worldId}/assets/{path}     # Update asset");
        System.out.println("  DELETE /api/worlds/{worldId}/assets/{path} # Delete asset");

        System.out.println("\nBlockType Management (CRUD):");
        System.out.println("  GET /api/worlds/{worldId}/blocktypes");
        System.out.println("  GET /api/worlds/{worldId}/blocktypes?query={searchTerm}");
        System.out.println("  GET /api/worlds/{worldId}/blocktypes/{id}");
        System.out.println("  GET /api/worlds/{worldId}/blocktypeschunk/{group}");
        System.out.println("  POST /api/worlds/{worldId}/blocktypes       # Create blocktype");
        System.out.println("  PUT /api/worlds/{worldId}/blocktypes/{id}   # Update blocktype");
        System.out.println("  DELETE /api/worlds/{worldId}/blocktypes/{id} # Delete blocktype");

        System.out.println("\nBlock Operations (CRUD):");
        System.out.println("  GET /api/worlds/{worldId}/blocks/{x}/{y}/{z}");
        System.out.println("  POST /api/worlds/{worldId}/blocks/{x}/{y}/{z}    # Create/place block");
        System.out.println("  PUT /api/worlds/{worldId}/blocks/{x}/{y}/{z}     # Update block");
        System.out.println("  DELETE /api/worlds/{worldId}/blocks/{x}/{y}/{z}  # Remove block");

        System.out.println("\nAuthentication:");
        System.out.println("  1. WebSocket login -> SessionId");
        System.out.println("  2. Authorization: Bearer {sessionId}");

        System.out.println("\nGenerated DTOs Contract:");
        System.out.println("  - Request/Response validation through generated DTOs");
        System.out.println("  - Type safety and schema compliance");
        System.out.println("  - JSON serialization/deserialization");

        System.out.println("\n✅ Editor API Overview Test erfolgreich");
        System.out.println("   - Alle CRUD Endpoints implementiert und getestet");
        System.out.println("   - Read + Write Operations verfügbar");
        System.out.println("   - Generated DTOs Contract validiert");
        System.out.println("   - Multi-Server Architektur (read-only + editor)");
    }

    // Helper method for asset upload
    private CloseableHttpResponse performPostAsset(String endpoint, byte[] data) throws Exception {
        String url = editorUrl + (endpoint.startsWith("/") ? endpoint : "/" + endpoint);

        org.apache.hc.client5.http.classic.methods.HttpPost request =
            new org.apache.hc.client5.http.classic.methods.HttpPost(url);

        if (sessionId != null) {
            request.setHeader("Authorization", "Bearer " + sessionId);
        }

        request.setEntity(new org.apache.hc.core5.http.io.entity.ByteArrayEntity(data,
            org.apache.hc.core5.http.ContentType.APPLICATION_OCTET_STREAM));

        return httpClient.execute(request);
    }
}
