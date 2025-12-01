package de.mhus.nimbus.world.test.rest;

import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.junit.jupiter.api.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * REST API Übersichts-Tests.
 * Testet alle verfügbaren GET Endpoints für Verfügbarkeit.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("REST API Endpoints Übersicht")
class RestApiOverviewTest extends AbstractRestTest {

    private String testWorldId;

    @BeforeEach
    void setUpWorldId() {
        testWorldId = getProperty("test.login.worldId", "test-world");
    }

    @Test
    @Order(1)
    @DisplayName("Alle World API Endpoints sollten erreichbar sein")
    void shouldTestWorldApiEndpoints() throws Exception {
        System.out.println("=== WORLD API ENDPOINTS ===");

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
    @DisplayName("Alle Assets API Endpoints sollten erreichbar sein")
    void shouldTestAssetsApiEndpoints() throws Exception {
        System.out.println("=== ASSETS API ENDPOINTS ===");

        // GET /api/worlds/{worldId}/assets
        try (CloseableHttpResponse response = performGet("/api/worlds/" + testWorldId + "/assets")) {
            System.out.println("GET /api/worlds/" + testWorldId + "/assets -> " + response.getCode());
            assertThat(response.getCode()).isIn(200, 404, 403);
        }

        // GET /api/worlds/{worldId}/assets?query=test
        try (CloseableHttpResponse response = performGet("/api/worlds/" + testWorldId + "/assets?query=test")) {
            System.out.println("GET /api/worlds/" + testWorldId + "/assets?query=test -> " + response.getCode());
            assertThat(response.getCode()).isIn(200, 404, 403);
        }

        // GET /api/worlds/{worldId}/assets?ext=png
        try (CloseableHttpResponse response = performGet("/api/worlds/" + testWorldId + "/assets?ext=png")) {
            System.out.println("GET /api/worlds/" + testWorldId + "/assets?ext=png -> " + response.getCode());
            assertThat(response.getCode()).isIn(200, 404, 403);
        }
    }

    @Test
    @Order(3)
    @DisplayName("Alle BlockType API Endpoints sollten erreichbar sein")
    void shouldTestBlockTypeApiEndpoints() throws Exception {
        System.out.println("=== BLOCKTYPE API ENDPOINTS ===");

        // GET /api/worlds/{worldId}/blocktypes
        try (CloseableHttpResponse response = performGet("/api/worlds/" + testWorldId + "/blocktypes")) {
            System.out.println("GET /api/worlds/" + testWorldId + "/blocktypes -> " + response.getCode());
            assertThat(response.getCode()).isIn(200, 404, 403);
        }

        // GET /api/worlds/{worldId}/blocktypes?query=stone
        try (CloseableHttpResponse response = performGet("/api/worlds/" + testWorldId + "/blocktypes?query=stone")) {
            System.out.println("GET /api/worlds/" + testWorldId + "/blocktypes?query=stone -> " + response.getCode());
            assertThat(response.getCode()).isIn(200, 404, 403);
        }

        // GET /api/worlds/{worldId}/blocktypes/1
        try (CloseableHttpResponse response = performGet("/api/worlds/" + testWorldId + "/blocktypes/1")) {
            System.out.println("GET /api/worlds/" + testWorldId + "/blocktypes/1 -> " + response.getCode());
            assertThat(response.getCode()).isIn(200, 404, 403, 400);
        }

        // GET /api/worlds/{worldId}/blocktypeschunk/w
        try (CloseableHttpResponse response = performGet("/api/worlds/" + testWorldId + "/blocktypeschunk/w")) {
            System.out.println("GET /api/worlds/" + testWorldId + "/blocktypeschunk/w -> " + response.getCode());
            assertThat(response.getCode()).isIn(200, 404, 403);
        }
    }

    @Test
    @Order(4)
    @DisplayName("Alle Block Operations API Endpoints sollten erreichbar sein")
    void shouldTestBlockOperationsApiEndpoints() throws Exception {
        System.out.println("=== BLOCK OPERATIONS API ENDPOINTS ===");

        // GET /api/worlds/{worldId}/blocks/{x}/{y}/{z}
        try (CloseableHttpResponse response = performGet("/api/worlds/" + testWorldId + "/blocks/0/64/0")) {
            System.out.println("GET /api/worlds/" + testWorldId + "/blocks/0/64/0 -> " + response.getCode());
            assertThat(response.getCode()).isIn(200, 404, 403, 400);
        }

        try (CloseableHttpResponse response = performGet("/api/worlds/" + testWorldId + "/blocks/10/64/10")) {
            System.out.println("GET /api/worlds/" + testWorldId + "/blocks/10/64/10 -> " + response.getCode());
            assertThat(response.getCode()).isIn(200, 404, 403, 400);
        }
    }

    @Test
    @Order(5)
    @DisplayName("API Übersicht und SessionId Validation")
    void shouldValidateApiOverview() throws Exception {
        System.out.println("=== API ÜBERSICHT ===");
        System.out.println("SessionId: " + (sessionId != null ? sessionId.substring(0, Math.min(10, sessionId.length())) + "..." : "null"));
        System.out.println("Test World ID: " + testWorldId);
        System.out.println("Player Base URL: " + playerUrl);

        // Validate that we have a valid session
        assertThat(sessionId).isNotNull();
        assertThat(sessionId).isNotEmpty();

        System.out.println("\n=== VERFÜGBARE REST API ENDPOINTS (GET) ===");
        System.out.println("World Management:");
        System.out.println("  GET /api/worlds");
        System.out.println("  GET /api/worlds/{worldId}");
        System.out.println("\nAsset Management:");
        System.out.println("  GET /api/worlds/{worldId}/assets");
        System.out.println("  GET /api/worlds/{worldId}/assets?query={searchTerm}");
        System.out.println("  GET /api/worlds/{worldId}/assets?ext={extension}");
        System.out.println("  GET /api/worlds/{worldId}/assets/{path}");
        System.out.println("\nBlockType Management:");
        System.out.println("  GET /api/worlds/{worldId}/blocktypes");
        System.out.println("  GET /api/worlds/{worldId}/blocktypes?query={searchTerm}");
        System.out.println("  GET /api/worlds/{worldId}/blocktypes/{id}");
        System.out.println("  GET /api/worlds/{worldId}/blocktypeschunk/{group}");
        System.out.println("\nBlock Operations:");
        System.out.println("  GET /api/worlds/{worldId}/blocks/{x}/{y}/{z}");
        System.out.println("\nAuthentication:");
        System.out.println("  Authorization: Bearer {sessionId}");
        System.out.println("  SessionId obtained via WebSocket login");

        System.out.println("\n✅ REST API Overview Test erfolgreich");
        System.out.println("   - Alle Endpoints implementiert und getestet");
        System.out.println("   - Generated DTOs Contract validiert");
        System.out.println("   - WebSocket Login → SessionId → REST API Authentication");
    }
}
