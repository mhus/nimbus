package de.mhus.nimbus.registry.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.mhus.nimbus.registry.service.RegistryService;
import de.mhus.nimbus.server.shared.dto.CreateWorldDto;
import de.mhus.nimbus.server.shared.dto.UpdateWorldDto;
import de.mhus.nimbus.server.shared.dto.WorldDto;
import de.mhus.nimbus.server.shared.util.AuthorizationUtils;
import de.mhus.nimbus.shared.util.IdentityServiceUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for RegistryController.
 */
@WebMvcTest(RegistryController.class)
class RegistryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RegistryService registryService;

    @MockBean
    private IdentityServiceUtils identityServiceUtils;

    @Autowired
    private ObjectMapper objectMapper;

    private WorldDto testWorldDto;
    private CreateWorldDto createWorldDto;
    private UpdateWorldDto updateWorldDto;

    @BeforeEach
    void setUp() {
        testWorldDto = WorldDto.builder()
                .id("world-123")
                .name("Test World")
                .description("A test world")
                .ownerId("user-123")
                .enabled(true)
                .accessUrl("ws://localhost:8080/world/world-123")
                .properties(Map.of("key1", "value1"))
                .createdAt(System.currentTimeMillis())
                .updatedAt(System.currentTimeMillis())
                .build();

        createWorldDto = CreateWorldDto.builder()
                .name("Test World")
                .description("A test world")
                .accessUrl("ws://localhost:8080/world/world-123")
                .properties(Map.of("key1", "value1"))
                .build();

        updateWorldDto = UpdateWorldDto.builder()
                .name("Updated World")
                .description("An updated test world")
                .accessUrl("ws://localhost:8080/world/updated-123")
                .properties(Map.of("key2", "value2"))
                .build();
    }

    @Test
    @WithMockUser(roles = {"CREATOR"})
    void createWorld_ShouldReturnCreatedWorld() throws Exception {
        // Given
        when(registryService.createWorld(any(CreateWorldDto.class), anyString()))
                .thenReturn(testWorldDto);

        try (MockedStatic<AuthorizationUtils> mockedUtils = mockStatic(AuthorizationUtils.class)) {
            mockedUtils.when(() -> AuthorizationUtils.getUserId(any(HttpServletRequest.class)))
                    .thenReturn("test-user-123");

            // When & Then
            mockMvc.perform(post("/worlds")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(createWorldDto)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value("world-123"))
                    .andExpect(jsonPath("$.name").value("Test World"));

            verify(registryService).createWorld(any(CreateWorldDto.class), anyString());
        }
    }

    @Test
    @WithMockUser(roles = {"USER"})
    void getWorld_ShouldReturnWorld_WhenExists() throws Exception {
        // Given
        when(registryService.getWorldById("world-123")).thenReturn(Optional.of(testWorldDto));

        // When & Then
        mockMvc.perform(get("/worlds/world-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("world-123"))
                .andExpect(jsonPath("$.name").value("Test World"));

        verify(registryService).getWorldById("world-123");
    }

    @Test
    @WithMockUser(roles = {"USER"})
    void getWorld_ShouldReturnNotFound_WhenNotExists() throws Exception {
        // Given
        when(registryService.getWorldById("nonexistent")).thenReturn(Optional.empty());

        // When & Then
        mockMvc.perform(get("/worlds/nonexistent"))
                .andExpect(status().isNotFound());

        verify(registryService).getWorldById("nonexistent");
    }

    @Test
    @WithMockUser(roles = {"USER"})
    void listWorlds_ShouldReturnPageOfWorlds() throws Exception {
        // Given
        Page<WorldDto> worldPage = new PageImpl<>(List.of(testWorldDto));
        when(registryService.listWorlds(any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(worldPage);

        // When & Then
        mockMvc.perform(get("/worlds")
                .param("page", "0")
                .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value("world-123"))
                .andExpect(jsonPath("$.content[0].name").value("Test World"));

        verify(registryService).listWorlds(null, null, null, 0, 20);
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    void updateWorld_ShouldReturnUpdatedWorld() throws Exception {
        // Given
        when(registryService.updateWorld(anyString(), any(UpdateWorldDto.class), anyString(), anyList()))
                .thenReturn(Optional.of(testWorldDto));

        try (MockedStatic<AuthorizationUtils> mockedUtils = mockStatic(AuthorizationUtils.class)) {
            mockedUtils.when(() -> AuthorizationUtils.getUserId(any(HttpServletRequest.class)))
                    .thenReturn("test-user-123");
            mockedUtils.when(() -> AuthorizationUtils.getUserRoles(any(HttpServletRequest.class)))
                    .thenReturn(List.of("ADMIN"));

            // When & Then
            mockMvc.perform(put("/worlds/world-123")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(updateWorldDto)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value("world-123"));

            verify(registryService).updateWorld(eq("world-123"), any(UpdateWorldDto.class),
                                              anyString(), anyList());
        }
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    void deleteWorld_ShouldReturnNoContent_WhenDeleted() throws Exception {
        // Given
        when(registryService.deleteWorld(anyString(), anyString(), anyList()))
                .thenReturn(true);

        try (MockedStatic<AuthorizationUtils> mockedUtils = mockStatic(AuthorizationUtils.class)) {
            mockedUtils.when(() -> AuthorizationUtils.getUserId(any(HttpServletRequest.class)))
                    .thenReturn("test-user-123");
            mockedUtils.when(() -> AuthorizationUtils.getUserRoles(any(HttpServletRequest.class)))
                    .thenReturn(List.of("ADMIN"));

            // When & Then
            mockMvc.perform(delete("/worlds/world-123")
                    .with(csrf()))
                    .andExpect(status().isNoContent());

            verify(registryService).deleteWorld(eq("world-123"), anyString(), anyList());
        }
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    void enableWorld_ShouldReturnEnabledWorld() throws Exception {
        // Given
        when(registryService.enableWorld(anyString(), anyString(), anyList()))
                .thenReturn(Optional.of(testWorldDto));

        try (MockedStatic<AuthorizationUtils> mockedUtils = mockStatic(AuthorizationUtils.class)) {
            mockedUtils.when(() -> AuthorizationUtils.getUserId(any(HttpServletRequest.class)))
                    .thenReturn("test-user-123");
            mockedUtils.when(() -> AuthorizationUtils.getUserRoles(any(HttpServletRequest.class)))
                    .thenReturn(List.of("ADMIN"));

            // When & Then
            mockMvc.perform(post("/worlds/world-123/enable")
                    .with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value("world-123"));

            verify(registryService).enableWorld(eq("world-123"), anyString(), anyList());
        }
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    void disableWorld_ShouldReturnDisabledWorld() throws Exception {
        // Given
        when(registryService.disableWorld(anyString(), anyString(), anyList()))
                .thenReturn(Optional.of(testWorldDto));

        try (MockedStatic<AuthorizationUtils> mockedUtils = mockStatic(AuthorizationUtils.class)) {
            mockedUtils.when(() -> AuthorizationUtils.getUserId(any(HttpServletRequest.class)))
                    .thenReturn("test-user-123");
            mockedUtils.when(() -> AuthorizationUtils.getUserRoles(any(HttpServletRequest.class)))
                    .thenReturn(List.of("ADMIN"));

            // When & Then
            mockMvc.perform(post("/worlds/world-123/disable")
                    .with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value("world-123"));

            verify(registryService).disableWorld(eq("world-123"), anyString(), anyList());
        }
    }

    @Test
    void createWorld_ShouldReturnUnauthorized_WhenNoAuthentication() throws Exception {
        // When & Then
        mockMvc.perform(post("/worlds")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createWorldDto)))
                .andExpect(status().isUnauthorized());

        verify(registryService, never()).createWorld(any(), any());
    }
}
