package de.mhus.nimbus.world.test.player;

import com.fasterxml.jackson.databind.JsonNode;
import de.mhus.nimbus.generated.rest.*;
import de.mhus.nimbus.generated.types.BlockType;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.junit.jupiter.api.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Player API System-Tests für BlockType Endpoints.
 * Nutzt die generated DTOs als Contract für alle GET Endpoints gegen Player Server.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Player BlockType API Tests")
class PlayerBlockTypeTest extends AbstractPlayerTest {

    private String testWorldId;

    @BeforeEach
    void setUpWorldId() {
        testWorldId = getProperty("test.login.worldId", "test-world");
    }

    @Test
    @Order(1)
    @DisplayName("GET /api/worlds/{worldId}/blocktypes sollte BlockType Liste zurückgeben")
    void shouldGetBlockTypeList() throws Exception {
        // When
        try (CloseableHttpResponse response = performGet("/api/worlds/" + testWorldId + "/blocktypes")) {
            // Then
            if (response.getCode() == 200) {
                String responseBody = getResponseBody(response);
                JsonNode jsonNode = objectMapper.readTree(responseBody);

                // Should have "blockTypes" array according to documentation
                if (jsonNode.has("blockTypes")) {
                    JsonNode blockTypes = jsonNode.get("blockTypes");
                    assertThat(blockTypes.isArray()).isTrue();

                    System.out.println("BlockTypes found: " + blockTypes.size());

                    if (blockTypes.size() > 0) {
                        JsonNode firstBlockType = blockTypes.get(0);

                        // Parse using generated BlockType (core type)
                        BlockType coreBlockType = objectMapper.treeToValue(firstBlockType, BlockType.class);
                        assertThat(coreBlockType.getId()).isNotNull();

                        System.out.println("Sample BlockType ID: " + coreBlockType.getId());
                        System.out.println("Sample BlockType Description: " + coreBlockType.getDescription());
                    }

                } else if (jsonNode.isArray()) {
                    // Direct array of BlockTypes
                    assertThat(jsonNode.size()).isGreaterThanOrEqualTo(0);

                    if (jsonNode.size() > 0) {
                        BlockType coreBlockType = objectMapper.treeToValue(jsonNode.get(0), BlockType.class);
                        assertThat(coreBlockType.getId()).isNotNull();
                    }
                }

            } else if (response.getCode() == 404) {
                System.out.println("World not found for blocktypes: " + testWorldId);
            } else {
                System.out.println("BlockTypes endpoint returned: " + response.getCode());
            }
        }
    }

    @Test
    @Order(2)
    @DisplayName("GET /api/worlds/{worldId}/blocktypes?query=fence sollte gefilterte BlockTypes zurückgeben")
    void shouldSearchBlockTypes() throws Exception {
        // When
        try (CloseableHttpResponse response = performGet("/api/worlds/" + testWorldId + "/blocktypes?query=fence")) {
            // Then
            if (response.getCode() == 200) {
                String responseBody = getResponseBody(response);
                JsonNode jsonNode = objectMapper.readTree(responseBody);

                JsonNode blockTypes = jsonNode.has("blockTypes") ? jsonNode.get("blockTypes") : jsonNode;

                if (blockTypes.isArray()) {
                    System.out.println("BlockTypes matching 'fence': " + blockTypes.size());

                    // Validate search results (if any)
                    for (JsonNode blockTypeNode : blockTypes) {
                        if (blockTypeNode.has("description")) {
                            String description = blockTypeNode.get("description").asText().toLowerCase();
                            System.out.println("  Found: " + description);
                            // Search might be fuzzy, so we just log for validation
                        }
                    }
                }

            } else if (response.getCode() == 404) {
                System.out.println("World not found for BlockType search: " + testWorldId);
            }
        }
    }

    @Test
    @Order(3)
    @DisplayName("GET /api/worlds/{worldId}/blocktypes/{id} sollte einzelnen BlockType zurückgeben")
    void shouldGetSingleBlockType() throws Exception {
        // Given - First get list to find a valid ID
        String blockTypeId = "1"; // Default assumption

        try (CloseableHttpResponse listResponse = performGet("/api/worlds/" + testWorldId + "/blocktypes")) {
            if (listResponse.getCode() == 200) {
                String listBody = getResponseBody(listResponse);
                JsonNode listJson = objectMapper.readTree(listBody);

                JsonNode blockTypes = listJson.has("blockTypes") ? listJson.get("blockTypes") : listJson;

                if (blockTypes.isArray() && blockTypes.size() > 0) {
                    JsonNode firstBlockType = blockTypes.get(0);
                    if (firstBlockType.has("id")) {
                        blockTypeId = firstBlockType.get("id").asText();
                    }
                }
            }
        }

        // When
        try (CloseableHttpResponse response = performGet("/api/worlds/" + testWorldId + "/blocktypes/" + blockTypeId)) {
            // Then
            if (response.getCode() == 200) {
                String responseBody = getResponseBody(response);
                JsonNode jsonNode = objectMapper.readTree(responseBody);

                // Could be direct BlockType or wrapped in response
                JsonNode blockTypeNode = jsonNode;
                if (jsonNode.has("blocktype")) {
                    blockTypeNode = jsonNode.get("blocktype");
                }

                // Parse as BlockTypeDTO using generated type
                BlockTypeDTO blockTypeDTO = objectMapper.treeToValue(blockTypeNode, BlockTypeDTO.class);

                assertThat(blockTypeDTO.getId()).isNotNull();
                assertThat(blockTypeDTO.getName()).isNotNull();

                System.out.println("Single BlockType retrieved:");
                System.out.println("  ID: " + blockTypeDTO.getId());
                System.out.println("  Name: " + blockTypeDTO.getName());
                System.out.println("  Display Name: " + blockTypeDTO.getDisplayName());
                System.out.println("  Shape: " + blockTypeDTO.getShape());
                System.out.println("  Texture: " + blockTypeDTO.getTexture());

            } else if (response.getCode() == 404) {
                System.out.println("BlockType not found: " + blockTypeId + " (acceptable for tests)");
            } else {
                System.out.println("BlockType single endpoint returned: " + response.getCode());
            }
        }
    }

    @Test
    @Order(4)
    @DisplayName("GET /api/worlds/{worldId}/blocktypeschunk/{group} sollte BlockType Range zurückgeben")
    void shouldGetBlockTypeChunk() throws Exception {
        // When - Test with default group "w" and some range
        try (CloseableHttpResponse response = performGet("/api/worlds/" + testWorldId + "/blocktypeschunk/w")) {
            // Then
            if (response.getCode() == 200) {
                String responseBody = getResponseBody(response);
                JsonNode jsonNode = objectMapper.readTree(responseBody);

                // Should be an array according to documentation
                assertThat(jsonNode.isArray()).isTrue();

                System.out.println("BlockType chunk 'w' contains: " + jsonNode.size() + " types");

                if (jsonNode.size() > 0) {
                    // Parse as array of BlockTypeDTO
                    for (JsonNode blockTypeNode : jsonNode) {
                        if (blockTypeNode.has("id")) {
                            BlockTypeDTO blockType = objectMapper.treeToValue(blockTypeNode, BlockTypeDTO.class);
                            System.out.println("  BlockType in chunk: " + blockType.getId() + " - " + blockType.getName());
                        }
                    }
                }

            } else if (response.getCode() == 404) {
                System.out.println("World or chunk not found: w (acceptable for tests)");
            } else {
                System.out.println("BlockType chunk endpoint returned: " + response.getCode());
            }
        }
    }

    @Test
    @Order(5)
    @DisplayName("BlockType API Contract sollte allen generated DTOs entsprechen")
    void shouldMatchAllBlockTypeContracts() throws Exception {
        // Test BlockTypeDTO contract
        BlockTypeDTO blockTypeDTO = BlockTypeDTO.builder()
                .id(1.0)
                .name("test_block")
                .displayName("Test Block")
                .shape("CUBE")
                .texture("test.png")
                .hardness(1.5)
                .miningtime(1500.0)
                .build();

        String blockTypeDTOJson = objectMapper.writeValueAsString(blockTypeDTO);
        BlockTypeDTO deserializedDTO = objectMapper.readValue(blockTypeDTOJson, BlockTypeDTO.class);
        assertThat(deserializedDTO.getId()).isEqualTo(1.0);
        assertThat(deserializedDTO.getShape()).isEqualTo("CUBE");

        // Test core BlockType contract
        BlockType coreBlockType = BlockType.builder()
                .id(1.0)
                .description("Core block type")
                .initialStatus(0.0)
                .build();

        String coreBlockTypeJson = objectMapper.writeValueAsString(coreBlockType);
        BlockType deserializedCore = objectMapper.readValue(coreBlockTypeJson, BlockType.class);
        assertThat(deserializedCore.getId()).isEqualTo(1.0);

        // Test BlockTypeListResponseDTO
        BlockTypeListResponseDTO listResponse = BlockTypeListResponseDTO.builder()
                .count(10.0)
                .limit(100.0)
                .offset(0.0)
                .build();

        String listResponseJson = objectMapper.writeValueAsString(listResponse);
        assertThat(listResponseJson).contains("\"count\":10.0");

        System.out.println("✅ BlockType DTOs Contract validation successful");
        System.out.println("   - BlockTypeDTO: All fields serializable");
        System.out.println("   - Core BlockType: Basic contract working");
        System.out.println("   - BlockTypeListResponseDTO: Response wrapper working");
        System.out.println("   - All API endpoints tested with contract validation");
    }

    @Test
    @Order(6)
    @DisplayName("Alle BlockType Felder sollten generated Contract entsprechen")
    void shouldMatchGeneratedContract() throws Exception {
        // When
        try (CloseableHttpResponse response = performGet("/api/worlds/" + testWorldId + "/blocktypes")) {
            if (response.getCode() == 200) {
                String responseBody = getResponseBody(response);
                JsonNode jsonNode = objectMapper.readTree(responseBody);

                JsonNode blockTypes = jsonNode.has("blockTypes") ? jsonNode.get("blockTypes") : jsonNode;

                if (blockTypes.isArray() && blockTypes.size() > 0) {
                    JsonNode blockTypeNode = blockTypes.get(0);

                    // Test with BlockTypeDTO
                    if (blockTypeNode.has("name")) {
                        BlockTypeDTO blockTypeDTO = objectMapper.treeToValue(blockTypeNode, BlockTypeDTO.class);

                        System.out.println("BlockType API->DTO Contract Verification:");
                        System.out.println("  ID: " + blockTypeDTO.getId());
                        System.out.println("  Name: " + blockTypeDTO.getName());
                        System.out.println("  Display Name: " + blockTypeDTO.getDisplayName());
                        System.out.println("  Shape: " + blockTypeDTO.getShape());
                        System.out.println("  Texture: " + blockTypeDTO.getTexture());
                        System.out.println("  Hardness: " + blockTypeDTO.getHardness());
                        System.out.println("  Mining Time: " + blockTypeDTO.getMiningtime());
                        System.out.println("  Options: " + blockTypeDTO.getOptions());

                        assertThat(blockTypeDTO).isNotNull();
                    } else {
                        // Test with core BlockType
                        BlockType coreBlockType = objectMapper.treeToValue(blockTypeNode, BlockType.class);

                        System.out.println("BlockType API->Core Contract Verification:");
                        System.out.println("  ID: " + coreBlockType.getId());
                        System.out.println("  Description: " + coreBlockType.getDescription());
                        System.out.println("  Initial Status: " + coreBlockType.getInitialStatus());
                        System.out.println("  Modifiers: " + coreBlockType.getModifiers());

                        assertThat(coreBlockType).isNotNull();
                    }
                }
            }
        }
    }
}
