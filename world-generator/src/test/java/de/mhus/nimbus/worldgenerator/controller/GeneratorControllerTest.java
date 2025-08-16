package de.mhus.nimbus.worldgenerator.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.mhus.nimbus.world.dto.AddPhaseRequest;
import de.mhus.nimbus.world.dto.CreateWorldGeneratorRequest;
import de.mhus.nimbus.worldgenerator.entity.WorldGenerator;
import de.mhus.nimbus.worldgenerator.entity.WorldGeneratorPhase;
import de.mhus.nimbus.worldgenerator.service.GeneratorService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(GeneratorController.class)
class GeneratorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GeneratorService generatorService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @WithMockUser
    void testCreateWorldGenerator() throws Exception {
        // Given
        WorldGenerator worldGenerator = WorldGenerator.builder()
                .id(1L)
                .name("Test World")
                .description("Test Description")
                .status("INITIALIZED")
                .build();

        when(generatorService.createWorldGenerator(anyString(), anyString(), any()))
                .thenReturn(worldGenerator);

        CreateWorldGeneratorRequest request = CreateWorldGeneratorRequest.builder()
                .name("Test World")
                .description("Test Description")
                .parameters(Map.of("size", "large"))
                .build();

        // When & Then
        mockMvc.perform(post("/api/generator/create")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Test World"))
                .andExpect(jsonPath("$.status").value("INITIALIZED"));
    }

    @Test
    @WithMockUser
    void testGetAllWorldGenerators() throws Exception {
        // Given
        WorldGenerator worldGenerator = WorldGenerator.builder()
                .id(1L)
                .name("Test World")
                .status("COMPLETED")
                .build();

        when(generatorService.getAllWorldGenerators()).thenReturn(List.of(worldGenerator));

        // When & Then
        mockMvc.perform(get("/api/generator"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("Test World"));
    }

    @Test
    @WithMockUser
    void testGetWorldGenerator() throws Exception {
        // Given
        WorldGenerator worldGenerator = WorldGenerator.builder()
                .id(1L)
                .name("Test World")
                .build();

        when(generatorService.getWorldGenerator(1L)).thenReturn(Optional.of(worldGenerator));

        // When & Then
        mockMvc.perform(get("/api/generator/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Test World"));
    }

    @Test
    @WithMockUser
    void testGetWorldGeneratorNotFound() throws Exception {
        // Given
        when(generatorService.getWorldGenerator(1L)).thenReturn(Optional.empty());

        // When & Then
        mockMvc.perform(get("/api/generator/1"))
                .andExpect(status().isNotFound());
    }
}
