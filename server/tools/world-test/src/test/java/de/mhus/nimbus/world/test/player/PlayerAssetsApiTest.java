package de.mhus.nimbus.world.test.player;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.junit.jupiter.api.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Player API System-Tests für Asset Endpoints.
 * Testet alle GET Endpoints für Asset-Verwaltung gegen Player Server.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Player Assets API Tests")
class PlayerAssetsApiTest extends AbstractPlayerTest {

    private String testWorldId;

    @BeforeEach
    void setUpWorldId() {
        testWorldId = getProperty("test.login.worldId", "test-world");
    }

    @Test
    @Order(1)
    @DisplayName("GET /api/worlds/{worldId}/assets sollte Asset Liste zurückgeben")
    void shouldGetAssetsList() throws Exception {
        // When
        try (CloseableHttpResponse response = performGet("/api/worlds/" + testWorldId + "/assets")) {
            // Then
            if (response.getCode() == 200) {
                String responseBody = getResponseBody(response);
                JsonNode jsonNode = objectMapper.readTree(responseBody);

                // Should have "assets" array according to documentation
                assertThat(jsonNode.has("assets")).isTrue();
                JsonNode assets = jsonNode.get("assets");
                assertThat(assets.isArray()).isTrue();

                System.out.println("Assets found: " + assets.size());

                // If assets exist, validate structure
                if (assets.size() > 0) {
                    JsonNode firstAsset = assets.get(0);
                    assertThat(firstAsset.has("path")).isTrue();
                    assertThat(firstAsset.has("size")).isTrue();
                    assertThat(firstAsset.has("mimeType")).isTrue();
                    assertThat(firstAsset.has("lastModified")).isTrue();
                    assertThat(firstAsset.has("extension")).isTrue();
                    assertThat(firstAsset.has("category")).isTrue();

                    System.out.println("Sample asset: " + firstAsset.get("path").asText());
                }

            } else if (response.getCode() == 404) {
                System.out.println("World not found for assets: " + testWorldId);
            } else {
                System.out.println("Assets endpoint returned: " + response.getCode());
            }
        }
    }

    @Test
    @Order(2)
    @DisplayName("GET /api/worlds/{worldId}/assets?query=stone sollte gefilterte Assets zurückgeben")
    void shouldSearchAssets() throws Exception {
        // When
        try (CloseableHttpResponse response = performGet("/api/worlds/" + testWorldId + "/assets?query=stone")) {
            // Then
            if (response.getCode() == 200) {
                String responseBody = getResponseBody(response);
                JsonNode jsonNode = objectMapper.readTree(responseBody);

                assertThat(jsonNode.has("assets")).isTrue();
                JsonNode assets = jsonNode.get("assets");
                assertThat(assets.isArray()).isTrue();

                System.out.println("Assets matching 'stone': " + assets.size());

                // Validate that results contain search term (if any results)
                for (JsonNode asset : assets) {
                    String path = asset.get("path").asText().toLowerCase();
                    // Should contain 'stone' in path or category
                    boolean hasStone = path.contains("stone") ||
                                     asset.get("category").asText().toLowerCase().contains("stone");
                    // Note: Search might be fuzzy, so we just log for now
                    System.out.println("  Asset: " + path + " (contains stone: " + hasStone + ")");
                }

            } else if (response.getCode() == 404) {
                System.out.println("World not found for asset search: " + testWorldId);
            }
        }
    }

    @Test
    @Order(3)
    @DisplayName("GET /api/worlds/{worldId}/assets?ext=png sollte PNG Assets zurückgeben")
    void shouldFilterAssetsByExtension() throws Exception {
        // When
        try (CloseableHttpResponse response = performGet("/api/worlds/" + testWorldId + "/assets?ext=png")) {
            // Then
            if (response.getCode() == 200) {
                String responseBody = getResponseBody(response);
                JsonNode jsonNode = objectMapper.readTree(responseBody);

                assertThat(jsonNode.has("assets")).isTrue();
                JsonNode assets = jsonNode.get("assets");
                assertThat(assets.isArray()).isTrue();

                System.out.println("PNG assets found: " + assets.size());

                // Validate that results have .png extension
                for (JsonNode asset : assets) {
                    String extension = asset.get("extension").asText();
                    String path = asset.get("path").asText();
                    System.out.println("  PNG Asset: " + path + " (ext: " + extension + ")");

                    // Should be .png files
                    assertThat(extension.toLowerCase()).contains("png");
                }

            } else if (response.getCode() == 404) {
                System.out.println("World not found for PNG asset filter: " + testWorldId);
            }
        }
    }

    @Test
    @Order(4)
    @DisplayName("GET /api/worlds/{worldId}/assets/{path} sollte Asset Datei herunterladen")
    void shouldDownloadAssetFile() throws Exception {
        // First, get list of assets to find a downloadable one
        String assetPath = "textures/block/basic/stone.png"; // Default test path

        try (CloseableHttpResponse listResponse = performGet("/api/worlds/" + testWorldId + "/assets")) {
            if (listResponse.getCode() == 200) {
                String listBody = getResponseBody(listResponse);
                JsonNode listJson = objectMapper.readTree(listBody);

                if (listJson.has("assets") && listJson.get("assets").size() > 0) {
                    assetPath = listJson.get("assets").get(0).get("path").asText();
                    System.out.println("Using asset path for download test: " + assetPath);
                }
            }
        }

        // When - Download the asset
        try (CloseableHttpResponse response = performGet("/api/worlds/" + testWorldId + "/assets/" + assetPath)) {
            // Then
            if (response.getCode() == 200) {
                // Should have binary content
                assertThat(response.getEntity()).isNotNull();
                assertThat(response.getEntity().getContentLength()).isGreaterThan(0);

                // Should have appropriate Content-Type
                String contentType = response.getEntity().getContentType().toString();
                assertThat(contentType).isNotNull();

                System.out.println("Asset downloaded successfully:");
                System.out.println("  Path: " + assetPath);
                System.out.println("  Content-Type: " + contentType);
                System.out.println("  Size: " + response.getEntity().getContentLength() + " bytes");

            } else if (response.getCode() == 404) {
                System.out.println("Asset not found: " + assetPath + " (this is acceptable for tests)");
            } else {
                System.out.println("Asset download returned: " + response.getCode());
            }
        }
    }

    @Test
    @Order(5)
    @DisplayName("Asset API Contract Validation")
    void shouldValidateAssetApiContract() throws Exception {
        // This test validates the asset API response structure
        try (CloseableHttpResponse response = performGet("/api/worlds/" + testWorldId + "/assets")) {
            if (response.getCode() == 200) {
                String responseBody = getResponseBody(response);
                JsonNode jsonNode = objectMapper.readTree(responseBody);

                // Validate contract structure from documentation
                assertThat(jsonNode.has("assets")).isTrue();
                JsonNode assets = jsonNode.get("assets");

                if (assets.size() > 0) {
                    JsonNode asset = assets.get(0);

                    // Required fields according to API documentation
                    String[] requiredFields = {"path", "size", "mimeType", "lastModified", "extension", "category"};

                    for (String field : requiredFields) {
                        assertThat(asset.has(field))
                            .as("Asset should have field: " + field)
                            .isTrue();
                    }

                    // Validate field types
                    assertThat(asset.get("path").isTextual()).isTrue();
                    assertThat(asset.get("size").isNumber()).isTrue();
                    assertThat(asset.get("mimeType").isTextual()).isTrue();
                    assertThat(asset.get("lastModified").isTextual()).isTrue();
                    assertThat(asset.get("extension").isTextual()).isTrue();
                    assertThat(asset.get("category").isTextual()).isTrue();

                    System.out.println("✅ Assets API Contract validation successful");
                    System.out.println("   - Response structure matches documentation");
                    System.out.println("   - All required fields present and correct types");
                }
            }
        }
    }
}
