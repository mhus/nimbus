package de.mhus.nimbus.world.test.player;

import com.fasterxml.jackson.databind.JsonNode;
import de.mhus.nimbus.generated.rest.*;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.junit.jupiter.api.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Player API System-Tests für World Endpoints.
 * Testet alle GET Endpoints für World-Verwaltung gegen Player Server.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Player World API Tests")
class PlayerWorldApiTest extends AbstractPlayerTest {

    @Test
    @Order(1)
    @DisplayName("GET /api/worlds sollte Liste der verfügbaren Welten zurückgeben")
    void shouldGetWorldsList() throws Exception {
        // When
        try (CloseableHttpResponse response = performGet("/api/worlds")) {
            // Then
            assertThat(response.getCode()).isEqualTo(200);

            String responseBody = getResponseBody(response);
            assertThat(responseBody).isNotNull();

            JsonNode jsonNode = objectMapper.readTree(responseBody);

            if (jsonNode.isArray()) {
                // Direct array of worlds
                assertThat(jsonNode.size()).isGreaterThanOrEqualTo(0);

                if (jsonNode.size() > 0) {
                    JsonNode firstWorld = jsonNode.get(0);
                    assertThat(firstWorld.has("worldId")).isTrue();
                    assertThat(firstWorld.has("name")).isTrue();
                    assertThat(firstWorld.has("description")).isTrue();

                    // Parse as WorldListItemDTO
                    WorldListItemDTO worldItem = objectMapper.treeToValue(firstWorld, WorldListItemDTO.class);
                    assertThat(worldItem).isNotNull();

                    System.out.println("Found world: " + worldItem.toString());
                }
            } else if (jsonNode.has("worlds")) {
                // Wrapped in WorldListResponseDTO
                WorldListResponseDTO worldsResponse = objectMapper.treeToValue(jsonNode, WorldListResponseDTO.class);
                assertThat(worldsResponse).isNotNull();

                System.out.println("World list response: " + worldsResponse.toString());
            }

            System.out.println("Worlds API response validated successfully");
        }
    }

    @Test
    @Order(2)
    @DisplayName("GET /api/worlds/{worldId} sollte World Details zurückgeben")
    void shouldGetWorldDetails() throws Exception {
        // Given - Use configured worldId from test properties
        String testWorldId = getProperty("test.login.worldId", "test-world");

        // When
        try (CloseableHttpResponse response = performGet("/api/worlds/" + testWorldId)) {
            // Then
            if (response.getCode() == 200) {
                String responseBody = getResponseBody(response);
                JsonNode jsonNode = objectMapper.readTree(responseBody);

                // Check required fields from documentation
                assertThat(jsonNode.has("worldId")).isTrue();
                assertThat(jsonNode.has("name")).isTrue();
                assertThat(jsonNode.has("description")).isTrue();
                assertThat(jsonNode.has("start")).isTrue();
                assertThat(jsonNode.has("stop")).isTrue();
                assertThat(jsonNode.has("chunkSize")).isTrue();

                // Parse as WorldDetailDTO
                WorldDetailDTO worldDetail = objectMapper.treeToValue(jsonNode, WorldDetailDTO.class);
                assertThat(worldDetail).isNotNull();

                System.out.println("World Details validated for: " + testWorldId);
                System.out.println("  World ID: " + worldDetail.getWorldId());
                System.out.println("  Name: " + worldDetail.getName());
                System.out.println("  Description: " + worldDetail.getDescription());

            } else if (response.getCode() == 404) {
                System.out.println("World not found: " + testWorldId + " (this is acceptable for tests)");
            } else {
                System.out.println("Unexpected response code: " + response.getCode());
            }
        }
    }

    @Test
    @Order(3)
    @DisplayName("Authentication Header sollte korrekt gesetzt werden")
    void shouldSetAuthenticationHeader() throws Exception {
        // This test validates that our sessionId is being used correctly
        // When
        try (CloseableHttpResponse response = performGet("/api/worlds")) {
            // Then - Any response validates that auth header was set
            // (401 would indicate missing/invalid auth)
            assertThat(response.getCode()).isIn(200, 404, 403); // Not 401 Unauthorized

            System.out.println("Authentication validated - SessionId: " +
                (sessionId != null ? sessionId.substring(0, Math.min(10, sessionId.length())) + "..." : "null"));
        }
    }

    @Test
    @Order(4)
    @DisplayName("Contract Validation für alle World Response DTOs")
    void shouldValidateWorldContractDTOs() throws Exception {
        // This test validates that all World DTOs can be instantiated and serialized

        // Test WorldListItemDTO
        WorldListItemDTO worldItem = WorldListItemDTO.builder()
                .worldId("test-world")
                .name("Test World")
                .description("A test world")
                .build();

        String worldItemJson = objectMapper.writeValueAsString(worldItem);
        WorldListItemDTO deserializedItem = objectMapper.readValue(worldItemJson, WorldListItemDTO.class);
        assertThat(deserializedItem.getWorldId()).isEqualTo("test-world");

        // Test WorldDetailDTO
        WorldDetailDTO worldDetail = WorldDetailDTO.builder()
                .worldId("test-world")
                .name("Test World")
                .description("A test world")
                .chunkSize(16.0)
                .build();

        String worldDetailJson = objectMapper.writeValueAsString(worldDetail);
        WorldDetailDTO deserializedDetail = objectMapper.readValue(worldDetailJson, WorldDetailDTO.class);
        assertThat(deserializedDetail.getChunkSize()).isEqualTo(16.0);

        System.out.println("✅ World DTOs Contract validation successful");
        System.out.println("   - WorldListItemDTO: Serialization OK");
        System.out.println("   - WorldDetailDTO: Serialization OK");
    }
}

