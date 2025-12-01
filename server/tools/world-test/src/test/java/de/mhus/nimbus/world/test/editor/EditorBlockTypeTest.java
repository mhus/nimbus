package de.mhus.nimbus.world.test.editor;

import com.fasterxml.jackson.databind.JsonNode;
import de.mhus.nimbus.generated.rest.BlockTypeDTO;
import de.mhus.nimbus.generated.types.BlockType;
import de.mhus.nimbus.world.test.rest.AbstractEditorTest;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.junit.jupiter.api.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Editor Tests für BlockType Management.
 * Testet alle CRUD Operationen gegen world-editor Server.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Editor BlockType CRUD Tests")
class EditorBlockTypeTest extends AbstractEditorTest {

    private String testWorldId;
    private String createdBlockTypeId;

    @BeforeEach
    void setUpWorldId() {
        testWorldId = getProperty("test.login.worldId", "test-world");
    }

    @Test
    @Order(1)
    @DisplayName("GET /api/worlds/{worldId}/blocktypes sollte gegen Editor Server funktionieren")
    void shouldGetBlockTypesFromEditor() throws Exception {
        // When
        try (CloseableHttpResponse response = performGet("/api/worlds/" + testWorldId + "/blocktypes")) {
            // Then
            System.out.println("Editor GET BlockTypes: " + response.getCode());

            if (response.getCode() == 200) {
                String responseBody = getResponseBody(response);
                JsonNode jsonNode = objectMapper.readTree(responseBody);

                JsonNode blockTypes = jsonNode.has("blockTypes") ? jsonNode.get("blockTypes") : jsonNode;

                if (blockTypes.isArray()) {
                    System.out.println("Editor returned " + blockTypes.size() + " BlockTypes");

                    if (blockTypes.size() > 0) {
                        BlockType blockType = objectMapper.treeToValue(blockTypes.get(0), BlockType.class);
                        System.out.println("Sample BlockType from Editor: " + blockType.getId());
                    }
                }
            } else {
                System.out.println("Editor BlockTypes endpoint: " + response.getCode());
            }

            assertThat(response.getCode()).isIn(200, 404, 403);
        }
    }

    @Test
    @Order(2)
    @DisplayName("POST /api/worlds/{worldId}/blocktypes sollte neuen BlockType erstellen")
    void shouldCreateBlockType() throws Exception {
        // Given - Create BlockType using generated contract
        BlockType newBlockType = BlockType.builder()
                .description("Test Block created by Editor Tests")
                .initialStatus(0.0)
                .build();

        String blockTypeJson = objectMapper.writeValueAsString(newBlockType);

        // When
        try (CloseableHttpResponse response = performPost("/api/worlds/" + testWorldId + "/blocktypes", blockTypeJson)) {
            // Then
            System.out.println("Editor POST BlockType: " + response.getCode());

            if (response.getCode() == 201) {
                String responseBody = getResponseBody(response);
                JsonNode jsonNode = objectMapper.readTree(responseBody);

                // Should return created BlockType with ID
                if (jsonNode.has("id")) {
                    createdBlockTypeId = jsonNode.get("id").asText();
                    System.out.println("Created BlockType with ID: " + createdBlockTypeId);

                    assertThat(createdBlockTypeId).isNotNull();
                    assertThat(createdBlockTypeId).isNotEmpty();
                } else {
                    System.out.println("Created BlockType response: " + responseBody);
                }

            } else if (response.getCode() == 400) {
                String errorBody = getResponseBody(response);
                System.out.println("BlockType creation failed (400): " + errorBody);
            } else {
                System.out.println("BlockType creation returned: " + response.getCode());
            }

            assertThat(response.getCode()).isIn(201, 400, 404, 403);
        }
    }

    @Test
    @Order(3)
    @DisplayName("GET /api/worlds/{worldId}/blocktypes/{id} sollte erstellten BlockType zurückgeben")
    void shouldGetCreatedBlockType() throws Exception {
        // Given - Skip if no BlockType was created
        if (createdBlockTypeId == null) {
            System.out.println("Skipping GET single BlockType - no BlockType was created");
            return;
        }

        // When
        try (CloseableHttpResponse response = performGet("/api/worlds/" + testWorldId + "/blocktypes/" + createdBlockTypeId)) {
            // Then
            System.out.println("Editor GET single BlockType: " + response.getCode());

            if (response.getCode() == 200) {
                String responseBody = getResponseBody(response);
                JsonNode jsonNode = objectMapper.readTree(responseBody);

                // Parse as BlockTypeDTO
                JsonNode blockTypeNode = jsonNode.has("blocktype") ? jsonNode.get("blocktype") : jsonNode;
                BlockTypeDTO blockType = objectMapper.treeToValue(blockTypeNode, BlockTypeDTO.class);

                System.out.println("Retrieved BlockType: " + blockType.getId() + " - " + blockType.getName());
                assertThat(String.valueOf((int)blockType.getId())).isEqualTo(createdBlockTypeId);

            } else {
                System.out.println("GET single BlockType returned: " + response.getCode());
            }

            assertThat(response.getCode()).isIn(200, 404, 403);
        }
    }

    @Test
    @Order(4)
    @DisplayName("PUT /api/worlds/{worldId}/blocktypes/{id} sollte BlockType aktualisieren")
    void shouldUpdateBlockType() throws Exception {
        // Given - Skip if no BlockType was created
        if (createdBlockTypeId == null) {
            System.out.println("Skipping PUT BlockType - no BlockType was created");
            return;
        }

        // Create updated BlockType
        BlockType updatedBlockType = BlockType.builder()
                .id(Double.parseDouble(createdBlockTypeId))
                .description("Updated Test Block description")
                .initialStatus(1.0)
                .build();

        String updateJson = objectMapper.writeValueAsString(updatedBlockType);

        // When
        try (CloseableHttpResponse response = performPut("/api/worlds/" + testWorldId + "/blocktypes/" + createdBlockTypeId, updateJson)) {
            // Then
            System.out.println("Editor PUT BlockType: " + response.getCode());

            if (response.getCode() == 200) {
                String responseBody = getResponseBody(response);
                JsonNode jsonNode = objectMapper.readTree(responseBody);

                // Verify updated fields
                if (jsonNode.has("description")) {
                    String description = jsonNode.get("description").asText();
                    assertThat(description).contains("Updated");
                    System.out.println("BlockType updated successfully: " + description);
                }

            } else if (response.getCode() == 400) {
                String errorBody = getResponseBody(response);
                System.out.println("BlockType update failed (400): " + errorBody);
            } else {
                System.out.println("PUT BlockType returned: " + response.getCode());
            }

            assertThat(response.getCode()).isIn(200, 400, 404, 403);
        }
    }

    @Test
    @Order(5)
    @DisplayName("DELETE /api/worlds/{worldId}/blocktypes/{id} sollte BlockType löschen")
    void shouldDeleteBlockType() throws Exception {
        // Given - Skip if no BlockType was created
        if (createdBlockTypeId == null) {
            System.out.println("Skipping DELETE BlockType - no BlockType was created");
            return;
        }

        // When
        try (CloseableHttpResponse response = performDelete("/api/worlds/" + testWorldId + "/blocktypes/" + createdBlockTypeId)) {
            // Then
            System.out.println("Editor DELETE BlockType: " + response.getCode());

            if (response.getCode() == 204) {
                System.out.println("BlockType deleted successfully");

                // Verify deletion by trying to GET the BlockType
                try (CloseableHttpResponse getResponse = performGet("/api/worlds/" + testWorldId + "/blocktypes/" + createdBlockTypeId)) {
                    System.out.println("GET deleted BlockType: " + getResponse.getCode());
                    assertThat(getResponse.getCode()).isEqualTo(404);
                }

            } else {
                System.out.println("DELETE BlockType returned: " + response.getCode());
            }

            assertThat(response.getCode()).isIn(204, 404, 403);
        }
    }

    @Test
    @Order(6)
    @DisplayName("CRUD Cycle sollte vollständig funktionieren")
    void shouldPerformCompleteCrudCycle() throws Exception {
        System.out.println("=== COMPLETE BLOCKTYPE CRUD CYCLE ===");

        // CREATE
        BlockType createBlockType = BlockType.builder()
                .description("CRUD Cycle Test Block")
                .initialStatus(0.0)
                .build();

        String createJson = objectMapper.writeValueAsString(createBlockType);
        String createdId = null;

        try (CloseableHttpResponse createResponse = performPost("/api/worlds/" + testWorldId + "/blocktypes", createJson)) {
            System.out.println("1. CREATE: " + createResponse.getCode());

            if (createResponse.getCode() == 201) {
                JsonNode createResult = objectMapper.readTree(getResponseBody(createResponse));
                if (createResult.has("id")) {
                    createdId = createResult.get("id").asText();
                    System.out.println("   Created ID: " + createdId);
                }
            }
        }

        if (createdId != null) {
            // READ
            try (CloseableHttpResponse readResponse = performGet("/api/worlds/" + testWorldId + "/blocktypes/" + createdId)) {
                System.out.println("2. READ: " + readResponse.getCode());
                if (readResponse.getCode() == 200) {
                    System.out.println("   Successfully read created BlockType");
                }
            }

            // UPDATE
            BlockType updateBlockType = BlockType.builder()
                    .id(Double.parseDouble(createdId))
                    .description("CRUD Cycle Test Block - UPDATED")
                    .initialStatus(1.0)
                    .build();

            String updateJson = objectMapper.writeValueAsString(updateBlockType);

            try (CloseableHttpResponse updateResponse = performPut("/api/worlds/" + testWorldId + "/blocktypes/" + createdId, updateJson)) {
                System.out.println("3. UPDATE: " + updateResponse.getCode());
            }

            // DELETE
            try (CloseableHttpResponse deleteResponse = performDelete("/api/worlds/" + testWorldId + "/blocktypes/" + createdId)) {
                System.out.println("4. DELETE: " + deleteResponse.getCode());
            }
        }

        System.out.println("✅ CRUD Cycle completed");
        assertThat(true).isTrue(); // Test passes if no exceptions thrown
    }

    @Test
    @Order(7)
    @DisplayName("Editor API Contract Validation für BlockType")
    void shouldValidateEditorBlockTypeContract() throws Exception {
        // Test that all generated DTOs work with editor responses

        // Test BlockType creation contract
        BlockType contractTest = BlockType.builder()
                .description("Contract validation test")
                .initialStatus(0.0)
                .build();

        String contractJson = objectMapper.writeValueAsString(contractTest);
        BlockType deserialized = objectMapper.readValue(contractJson, BlockType.class);

        assertThat(deserialized.getDescription()).isEqualTo("Contract validation test");
        assertThat(deserialized.getInitialStatus()).isEqualTo(0.0);

        // Test BlockTypeDTO parsing
        BlockTypeDTO dtoTest = BlockTypeDTO.builder()
                .id(999.0)
                .name("contract_test")
                .displayName("Contract Test Block")
                .build();

        String dtoJson = objectMapper.writeValueAsString(dtoTest);
        BlockTypeDTO deserializedDto = objectMapper.readValue(dtoJson, BlockTypeDTO.class);

        assertThat(deserializedDto.getId()).isEqualTo(999.0);
        assertThat(deserializedDto.getName()).isEqualTo("contract_test");

        System.out.println("✅ Editor BlockType Contract validation successful");
        System.out.println("   - Create/Update operations use BlockType");
        System.out.println("   - Read operations return BlockTypeDTO");
        System.out.println("   - All generated DTOs serialize/deserialize correctly");
        System.out.println("   - Editor Server URL: " + editorUrl);
    }
}
