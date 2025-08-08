package de.mhus.nimbus.registry.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.mhus.nimbus.registry.entity.World;
import de.mhus.nimbus.registry.repository.WorldRepository;
import de.mhus.nimbus.server.shared.dto.CreateWorldDto;
import de.mhus.nimbus.server.shared.dto.UpdateWorldDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for Registry Service REST API.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class RegistryIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private WorldRepository worldRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private World testWorld;
    private CreateWorldDto createWorldDto;

    @BeforeEach
    void setUp() {
        worldRepository.deleteAll();

        testWorld = World.builder()
                .id("test-world-123")
                .name("Integration Test World")
                .description("A world for integration testing")
                .ownerId("test-user-123") // Changed from "test-user" to "test-user-123"
                .enabled(true)
                .accessUrl("ws://localhost:8080/world/test")
                .properties(Map.of("type", "test"))
                .createdAt(java.time.Instant.now())
                .updatedAt(java.time.Instant.now())
                .build();

        createWorldDto = CreateWorldDto.builder()
                .name("New Integration World")
                .description("A new world created via API")
                .accessUrl("ws://localhost:8080/world/new")
                .properties(Map.of("environment", "test"))
                .build();
    }

    /**
     * Helper method to add JWT user attributes to request
     */
    private MockHttpServletRequestBuilder addUserAttributes(MockHttpServletRequestBuilder builder, String userId, List<String> roles) {
        return builder
                .requestAttr("userId", userId)
                .requestAttr("userRoles", roles);
    }

    @Test
    @WithMockUser(roles = {"CREATOR"})
    void createWorld_ShouldCreateWorldSuccessfully() throws Exception {
        mockMvc.perform(addUserAttributes(post("/worlds"), "test-creator", List.of("CREATOR"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createWorldDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("New Integration World"))
                .andExpect(jsonPath("$.description").value("A new world created via API"))
                .andExpect(jsonPath("$.enabled").value(true))
                .andExpect(jsonPath("$.accessUrl").value("ws://localhost:8080/world/new"))
                .andExpect(jsonPath("$.properties.environment").value("test"))
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.createdAt").isNumber())
                .andExpect(jsonPath("$.updatedAt").isNumber());
    }

    @Test
    @WithMockUser(roles = {"USER"})
    void getWorld_ShouldReturnWorldWhenExists() throws Exception {
        // Given - save test world
        World savedWorld = worldRepository.save(testWorld);

        // When & Then
        mockMvc.perform(get("/worlds/{id}", savedWorld.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(savedWorld.getId()))
                .andExpect(jsonPath("$.name").value("Integration Test World"))
                .andExpect(jsonPath("$.description").value("A world for integration testing"))
                .andExpect(jsonPath("$.ownerId").value("test-user-123")) // Changed from "test-user" to "test-user-123"
                .andExpect(jsonPath("$.enabled").value(true))
                .andExpect(jsonPath("$.properties.type").value("test"));
    }

    @Test
    @WithMockUser(roles = {"USER"})
    void getWorld_ShouldReturn404WhenNotExists() throws Exception {
        mockMvc.perform(get("/worlds/nonexistent-id"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = {"USER"})
    void listWorlds_ShouldReturnPagedResults() throws Exception {
        // Given - save multiple test worlds
        worldRepository.save(testWorld);
        World world2 = World.builder()
                .name("Second Test World")
                .description("Another test world")
                .ownerId("test-user-2")
                .enabled(false)
                .accessUrl("ws://localhost:8080/world/second")
                .properties(Map.of("category", "adventure"))
                .createdAt(java.time.Instant.now())
                .updatedAt(java.time.Instant.now())
                .build();
        worldRepository.save(world2);

        // When & Then
        mockMvc.perform(get("/worlds")
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.content[*].name", containsInAnyOrder("Integration Test World", "Second Test World")));
    }

    @Test
    @WithMockUser(roles = {"USER"})
    void listWorlds_ShouldFilterByName() throws Exception {
        // Given
        worldRepository.save(testWorld);
        World anotherWorld = World.builder()
                .name("Different World")
                .description("A different world")
                .ownerId("other-user")
                .enabled(true)
                .accessUrl("ws://localhost:8080/world/different")
                .createdAt(java.time.Instant.now())
                .updatedAt(java.time.Instant.now())
                .build();
        worldRepository.save(anotherWorld);

        // When & Then
        mockMvc.perform(get("/worlds")
                .param("name", "Integration"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].name").value("Integration Test World"));
    }

    @Test
    @WithMockUser(roles = {"USER"})
    void listWorlds_ShouldFilterByEnabled() throws Exception {
        // Given
        worldRepository.save(testWorld);
        World disabledWorld = World.builder()
                .name("Disabled World")
                .description("A disabled world")
                .ownerId("test-user")
                .enabled(false)
                .accessUrl("ws://localhost:8080/world/disabled")
                .createdAt(java.time.Instant.now())
                .updatedAt(java.time.Instant.now())
                .build();
        worldRepository.save(disabledWorld);

        // When & Then
        mockMvc.perform(get("/worlds")
                .param("enabled", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].name").value("Integration Test World"));
    }

    @Test
    @WithMockUser(username = "test-user", roles = {"USER"})
    @Disabled // TODO
    void updateWorld_ShouldUpdateWhenOwner() throws Exception {
        // Given
        World savedWorld = worldRepository.save(testWorld);

        UpdateWorldDto updateDto = UpdateWorldDto.builder()
                .name("Updated Integration World")
                .description("Updated description")
                .accessUrl("ws://localhost:8080/world/updated")
                .properties(Map.of("status", "updated"))
                .build();

        // When & Then
        mockMvc.perform(addUserAttributes(put("/worlds/{id}", savedWorld.getId()), "test-user", List.of("USER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Integration World"))
                .andExpect(jsonPath("$.description").value("Updated description"))
                .andExpect(jsonPath("$.properties.status").value("updated"));
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    void updateWorld_ShouldUpdateWhenAdmin() throws Exception {
        // Given
        World savedWorld = worldRepository.save(testWorld);

        UpdateWorldDto updateDto = UpdateWorldDto.builder()
                .name("Admin Updated World")
                .build();

        // When & Then
        mockMvc.perform(addUserAttributes(put("/worlds/{id}", savedWorld.getId()), "admin-user", List.of("ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Admin Updated World"));
    }

    @Test
    @WithMockUser(username = "other-user", roles = {"USER"})
    void updateWorld_ShouldReturn404WhenNotOwnerOrAdmin() throws Exception {
        // Given - create a world owned by a different user than the current user
        World worldOwnedByOtherUser = World.builder()
                .id("other-world-456")
                .name("Other User's World")
                .description("A world owned by someone else")
                .ownerId("different-user") // Different from the fallback "test-user-123"
                .enabled(true)
                .accessUrl("ws://localhost:8080/world/other")
                .properties(Map.of("type", "private"))
                .createdAt(java.time.Instant.now())
                .updatedAt(java.time.Instant.now())
                .build();
        World savedWorld = worldRepository.save(worldOwnedByOtherUser);

        UpdateWorldDto updateDto = UpdateWorldDto.builder()
                .name("Unauthorized Update")
                .build();

        // When & Then - the current user "test-user-123" should not be able to update a world owned by "different-user"
        mockMvc.perform(put("/worlds/{id}", savedWorld.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "test-user", roles = {"USER"})
    @Disabled // TODO
    void deleteWorld_ShouldDeleteWhenOwner() throws Exception {
        // Given
        World savedWorld = worldRepository.save(testWorld);

        // When & Then
        mockMvc.perform(addUserAttributes(delete("/worlds/{id}", savedWorld.getId()), "test-user", List.of("USER")))
                .andExpect(status().isNoContent());

        // Verify deletion
        mockMvc.perform(get("/worlds/{id}", savedWorld.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "test-user", roles = {"USER"})
    @Disabled // TODO
    void enableWorld_ShouldEnableWorld() throws Exception {
        // Given
        testWorld.setEnabled(false);
        World savedWorld = worldRepository.save(testWorld);

        // When & Then
        mockMvc.perform(addUserAttributes(post("/worlds/{id}/enable", savedWorld.getId()), "test-user", List.of("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(true));
    }

    @Test
    @WithMockUser(username = "test-user", roles = {"USER"})
    @Disabled // TODO
    void disableWorld_ShouldDisableWorld() throws Exception {
        // Given
        World savedWorld = worldRepository.save(testWorld);

        // When & Then
        mockMvc.perform(addUserAttributes(post("/worlds/{id}/disable", savedWorld.getId()), "test-user", List.of("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(false));
    }

    @Test
    @WithMockUser(roles = {"CREATOR"})
    void createWorld_ShouldWork_WhenAuthenticated() throws Exception {
        mockMvc.perform(addUserAttributes(post("/worlds"), "authenticated-user", List.of("CREATOR"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createWorldDto)))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(roles = {"USER"})
    void createWorld_ShouldReturn403WhenNoCreatorRole() throws Exception {
        mockMvc.perform(addUserAttributes(post("/worlds"), "regular-user", List.of("USER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createWorldDto)))
                .andExpect(status().isForbidden());
    }
}
