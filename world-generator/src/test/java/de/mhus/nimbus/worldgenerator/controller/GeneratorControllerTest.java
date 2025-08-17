package de.mhus.nimbus.worldgenerator.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.mhus.nimbus.worldgenerator.entity.WorldGenerator;
import de.mhus.nimbus.worldgenerator.entity.WorldGeneratorPhase;
import de.mhus.nimbus.worldgenerator.service.GeneratorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.*;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = GeneratorController.class)
@TestPropertySource(properties = {"nimbus.shared-secret=test-secret"})
@WithMockUser
class GeneratorControllerTest {

    @TestConfiguration
    static class TestConfig {
        @Bean
        @Primary
        public GeneratorService generatorService() {
            return mock(GeneratorService.class);
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private GeneratorService generatorService;

    @Autowired
    private ObjectMapper objectMapper;

    private WorldGenerator testWorldGenerator;
    private List<WorldGeneratorPhase> testPhases;

    @BeforeEach
    void setUp() {
        testWorldGenerator = WorldGenerator.builder()
                .id(1L)
                .worldId("test-world-001")
                .name("Test Fantasy World")
                .description("Eine Testwelt für Controller Tests")
                .status(WorldGenerator.GenerationStatus.PENDING)
                .parameters(Map.of(
                        "worldSize", "medium",
                        "biomeVariety", "high",
                        "magicLevel", "medium"
                ))
                .totalPhases(8)
                .completedPhases(0)
                .createdAt(LocalDateTime.now())
                .build();

        testPhases = Arrays.asList(
                createTestPhase(WorldGeneratorPhase.PHASE_INITIALIZATION, 1),
                createTestPhase(WorldGeneratorPhase.PHASE_ASSET_MATERIAL_GENERATION, 2),
                createTestPhase(WorldGeneratorPhase.PHASE_CONTINENT_GENERATION, 3),
                createTestPhase(WorldGeneratorPhase.PHASE_TERRAIN_GENERATION, 4),
                createTestPhase(WorldGeneratorPhase.PHASE_HISTORICAL_GENERATION, 5),
                createTestPhase(WorldGeneratorPhase.PHASE_STRUCTURE_GENERATION, 6),
                createTestPhase(WorldGeneratorPhase.PHASE_ITEM_GENERATION, 7),
                createTestPhase(WorldGeneratorPhase.PHASE_QUEST_GENERATION, 8)
        );
    }

    private WorldGeneratorPhase createTestPhase(String phaseType, int order) {
        return WorldGeneratorPhase.builder()
                .id((long) order)
                .worldGenerator(testWorldGenerator)
                .phaseType(phaseType)
                .phaseOrder(order)
                .status(WorldGeneratorPhase.PhaseStatus.PENDING)
                .parameters(Map.of("testParam", "testValue"))
                .progressPercentage(0)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void createWorldGenerator_ShouldReturnCreatedGenerator() throws Exception {
        // Given
        Map<String, Object> requestBody = Map.of(
                "worldId", "test-world-001",
                "name", "Test Fantasy World",
                "description", "Eine Testwelt für Controller Tests",
                "parameters", Map.of("worldSize", "medium")
        );

        when(generatorService.createWorldGenerator(anyString(), anyString(), anyString(), anyMap()))
                .thenReturn(testWorldGenerator);

        // When & Then
        mockMvc.perform(post("/api/generator/worlds")
                        .header("X-Shared-Secret", "test-secret")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody))
                        .with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.worldId").value("test-world-001"))
                .andExpect(jsonPath("$.name").value("Test Fantasy World"))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void getAllWorldGenerators_ShouldReturnListOfGenerators() throws Exception {
        // Given
        List<WorldGenerator> generators = List.of(testWorldGenerator);
        when(generatorService.getAllWorldGenerators()).thenReturn(generators);

        // When & Then
        mockMvc.perform(get("/api/generator/worlds")
                        .header("X-Shared-Secret", "test-secret"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].worldId").value("test-world-001"))
                .andExpect(jsonPath("$[0].name").value("Test Fantasy World"));

        verify(generatorService).getAllWorldGenerators();
    }

    @Test
    void getWorldGeneratorById_ShouldReturnGenerator() throws Exception {
        // Given
        when(generatorService.getWorldGeneratorById(1L)).thenReturn(Optional.of(testWorldGenerator));

        // When & Then
        mockMvc.perform(get("/api/generator/worlds/1")
                        .header("X-Shared-Secret", "test-secret"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.worldId").value("test-world-001"))
                .andExpect(jsonPath("$.name").value("Test Fantasy World"));

        verify(generatorService).getWorldGeneratorById(1L);
    }

    @Test
    void getWorldGeneratorById_ShouldReturn404WhenNotFound() throws Exception {
        // Given
        when(generatorService.getWorldGeneratorById(999L)).thenReturn(Optional.empty());

        // When & Then
        mockMvc.perform(get("/api/generator/worlds/999")
                        .header("X-Shared-Secret", "test-secret"))
                .andExpect(status().isNotFound());

        verify(generatorService).getWorldGeneratorById(999L);
    }

    @Test
    void startGeneration_ShouldStartGenerationProcess() throws Exception {
        // Given
        when(generatorService.startGeneration(1L)).thenReturn(true);

        // When & Then
        mockMvc.perform(post("/api/generator/worlds/1/start")
                        .header("X-Shared-Secret", "test-secret")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Generation started successfully"));

        verify(generatorService).startGeneration(1L);
    }

    @Test
    void startGeneration_ShouldReturn404WhenGeneratorNotFound() throws Exception {
        // Given
        when(generatorService.startGeneration(999L)).thenReturn(false);

        // When & Then
        mockMvc.perform(post("/api/generator/worlds/999/start")
                        .header("X-Shared-Secret", "test-secret")
                        .with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("World generator not found"));

        verify(generatorService).startGeneration(999L);
    }

    @Test
    void getPhasesByWorldGenerator_ShouldReturnPhases() throws Exception {
        // Given
        when(generatorService.getPhasesByWorldGenerator(1L)).thenReturn(testPhases);

        // When & Then
        mockMvc.perform(get("/api/generator/worlds/1/phases")
                        .header("X-Shared-Secret", "test-secret"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(8))
                .andExpect(jsonPath("$[0].phaseType").value("INITIALIZATION"))
                .andExpect(jsonPath("$[1].phaseType").value("ASSET_MATERIAL_GENERATION"))
                .andExpect(jsonPath("$[2].phaseType").value("CONTINENT_GENERATION"))
                .andExpect(jsonPath("$[3].phaseType").value("TERRAIN_GENERATION"))
                .andExpect(jsonPath("$[4].phaseType").value("HISTORICAL_GENERATION"))
                .andExpect(jsonPath("$[5].phaseType").value("STRUCTURE_GENERATION"))
                .andExpect(jsonPath("$[6].phaseType").value("ITEM_GENERATION"))
                .andExpect(jsonPath("$[7].phaseType").value("QUEST_GENERATION"));

        verify(generatorService).getPhasesByWorldGenerator(1L);
    }

    @Test
    void addPhaseToWorldGenerator_ShouldAddNewPhase() throws Exception {
        // Given
        WorldGeneratorPhase newPhase = createTestPhase(WorldGeneratorPhase.PHASE_INITIALIZATION, 1);
        Map<String, Object> requestBody = Map.of(
                "phaseType", "INITIALIZATION",
                "phaseOrder", 1,
                "parameters", Map.of("seed", "12345")
        );

        when(generatorService.addPhaseToWorldGenerator(eq(1L), eq("INITIALIZATION"),
                eq(1), anyMap())).thenReturn(newPhase);

        // When & Then
        mockMvc.perform(post("/api/generator/worlds/1/phases")
                        .header("X-Shared-Secret", "test-secret")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody))
                        .with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.phaseType").value("INITIALIZATION"))
                .andExpect(jsonPath("$.phaseOrder").value(1));

        verify(generatorService).addPhaseToWorldGenerator(1L,
                "INITIALIZATION", 1, Map.of("seed", "12345"));
    }

    @Test
    void updatePhaseProgress_ShouldUpdateProgress() throws Exception {
        // Given
        Map<String, Object> requestBody = Map.of("progressPercentage", 75);
        when(generatorService.updatePhaseProgress(1L, 75)).thenReturn(true);

        // When & Then
        mockMvc.perform(put("/api/generator/phases/1/progress")
                        .header("X-Shared-Secret", "test-secret")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Phase progress updated successfully"));

        verify(generatorService).updatePhaseProgress(1L, 75);
    }

    @Test
    void completePhase_ShouldCompletePhase() throws Exception {
        // Given
        Map<String, Object> requestBody = Map.of("resultSummary", "Phase completed successfully");
        when(generatorService.completePhase(1L, "Phase completed successfully")).thenReturn(true);

        // When & Then
        mockMvc.perform(post("/api/generator/phases/1/complete")
                        .header("X-Shared-Secret", "test-secret")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Phase completed successfully"));

        verify(generatorService).completePhase(1L, "Phase completed successfully");
    }

    @Test
    void failPhase_ShouldFailPhase() throws Exception {
        // Given
        Map<String, Object> requestBody = Map.of("errorMessage", "Phase failed due to invalid parameters");
        when(generatorService.failPhase(1L, "Phase failed due to invalid parameters")).thenReturn(true);

        // When & Then
        mockMvc.perform(post("/api/generator/phases/1/fail")
                        .header("X-Shared-Secret", "test-secret")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Phase failed"));

        verify(generatorService).failPhase(1L, "Phase failed due to invalid parameters");
    }

    @Test
    void getWorldGeneratorsByStatus_ShouldReturnFilteredGenerators() throws Exception {
        // Given
        List<WorldGenerator> completedGenerators = List.of(testWorldGenerator);
        when(generatorService.getWorldGeneratorsByStatus(WorldGenerator.GenerationStatus.COMPLETED))
                .thenReturn(completedGenerators);

        // When & Then
        mockMvc.perform(get("/api/generator/worlds")
                        .param("status", "COMPLETED")
                        .header("X-Shared-Secret", "test-secret"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].worldId").value("test-world-001"));

        verify(generatorService).getWorldGeneratorsByStatus(WorldGenerator.GenerationStatus.COMPLETED);
    }

    @Test
    void deleteWorldGenerator_ShouldDeleteGenerator() throws Exception {
        // Given
        when(generatorService.deleteWorldGenerator(1L)).thenReturn(true);

        // When & Then
        mockMvc.perform(delete("/api/generator/worlds/1")
                        .header("X-Shared-Secret", "test-secret")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("World generator deleted successfully"));

        verify(generatorService).deleteWorldGenerator(1L);
    }

    @Test
    void deleteWorldGenerator_ShouldReturn404WhenNotFound() throws Exception {
        // Given
        when(generatorService.deleteWorldGenerator(999L)).thenReturn(false);

        // When & Then
        mockMvc.perform(delete("/api/generator/worlds/999")
                        .header("X-Shared-Secret", "test-secret")
                        .with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("World generator not found"));

        verify(generatorService).deleteWorldGenerator(999L);
    }

    @Test
    void unauthorized_ShouldReturn401WithoutSharedSecret() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/generator/worlds"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void unauthorized_ShouldReturn401WithInvalidSharedSecret() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/generator/worlds")
                        .header("X-Shared-Secret", "invalid-secret"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getWorldGeneratorStatus_ShouldReturnStatus() throws Exception {
        // Given
        testWorldGenerator.setStatus(WorldGenerator.GenerationStatus.RUNNING);
        testWorldGenerator.setCompletedPhases(3);
        when(generatorService.getWorldGeneratorById(1L)).thenReturn(Optional.of(testWorldGenerator));

        // When & Then
        mockMvc.perform(get("/api/generator/worlds/1/status")
                        .header("X-Shared-Secret", "test-secret"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RUNNING"))
                .andExpect(jsonPath("$.progressPercentage").value(37.5))
                .andExpect(jsonPath("$.completedPhases").value(3))
                .andExpect(jsonPath("$.totalPhases").value(8));

        verify(generatorService).getWorldGeneratorById(1L);
    }
}
