package de.mhus.nimbus.registry.service;

import de.mhus.nimbus.registry.entity.World;
import de.mhus.nimbus.registry.repository.WorldRepository;
import de.mhus.nimbus.server.shared.dto.CreateWorldDto;
import de.mhus.nimbus.server.shared.dto.UpdateWorldDto;
import de.mhus.nimbus.server.shared.dto.WorldDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for RegistryService.
 */
@ExtendWith(MockitoExtension.class)
class RegistryServiceTest {

    @Mock
    private WorldRepository worldRepository;

    @InjectMocks
    private RegistryService registryService;

    private World testWorld;
    private CreateWorldDto createWorldDto;
    private UpdateWorldDto updateWorldDto;

    @BeforeEach
    void setUp() {
        testWorld = World.builder()
                .id("world-123")
                .name("Test World")
                .description("A test world")
                .ownerId("user-123")
                .enabled(true)
                .accessUrl("ws://localhost:8080/world/world-123")
                .properties(Map.of("key1", "value1"))
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
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
    void createWorld_ShouldCreateAndReturnWorld() {
        // Given
        when(worldRepository.save(any(World.class))).thenReturn(testWorld);

        // When
        WorldDto result = registryService.createWorld(createWorldDto, "user-123");

        // Then
        assertNotNull(result);
        assertEquals("world-123", result.getId());
        assertEquals("Test World", result.getName());
        assertEquals("A test world", result.getDescription());
        assertEquals("user-123", result.getOwnerId());
        assertTrue(result.getEnabled());
        assertEquals("ws://localhost:8080/world/world-123", result.getAccessUrl());
        assertEquals(Map.of("key1", "value1"), result.getProperties());

        verify(worldRepository).save(any(World.class));
    }

    @Test
    void getWorldById_ShouldReturnWorld_WhenWorldExists() {
        // Given
        when(worldRepository.findById("world-123")).thenReturn(Optional.of(testWorld));

        // When
        Optional<WorldDto> result = registryService.getWorldById("world-123");

        // Then
        assertTrue(result.isPresent());
        assertEquals("world-123", result.get().getId());
        assertEquals("Test World", result.get().getName());

        verify(worldRepository).findById("world-123");
    }

    @Test
    void getWorldById_ShouldReturnEmpty_WhenWorldNotExists() {
        // Given
        when(worldRepository.findById("nonexistent")).thenReturn(Optional.empty());

        // When
        Optional<WorldDto> result = registryService.getWorldById("nonexistent");

        // Then
        assertFalse(result.isPresent());

        verify(worldRepository).findById("nonexistent");
    }

    @Test
    void listWorlds_ShouldReturnPageOfWorlds() {
        // Given
        List<World> worlds = Arrays.asList(testWorld);
        Page<World> worldPage = new PageImpl<>(worlds);
        when(worldRepository.findWorldsWithFilters(any(), any(), any(), any(Pageable.class)))
                .thenReturn(worldPage);

        // When
        Page<WorldDto> result = registryService.listWorlds(null, null, null, 0, 20);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("world-123", result.getContent().get(0).getId());

        verify(worldRepository).findWorldsWithFilters(eq(null), eq(null), eq(null), any(Pageable.class));
    }

    @Test
    void listWorlds_ShouldLimitPageSize() {
        // Given
        List<World> worlds = Arrays.asList(testWorld);
        Page<World> worldPage = new PageImpl<>(worlds);
        when(worldRepository.findWorldsWithFilters(any(), any(), any(), any(Pageable.class)))
                .thenReturn(worldPage);

        // When
        Page<WorldDto> result = registryService.listWorlds(null, null, null, 0, 200);

        // Then
        assertNotNull(result);
        verify(worldRepository).findWorldsWithFilters(any(), any(), any(), argThat(pageable ->
            pageable.getPageSize() == 100));
    }

    @Test
    void updateWorld_ShouldUpdateWorld_WhenUserIsOwner() {
        // Given
        when(worldRepository.findById("world-123")).thenReturn(Optional.of(testWorld));
        when(worldRepository.save(any(World.class))).thenReturn(testWorld);

        // When
        Optional<WorldDto> result = registryService.updateWorld(
                "world-123", updateWorldDto, "user-123", Arrays.asList("USER"));

        // Then
        assertTrue(result.isPresent());
        verify(worldRepository).findById("world-123");
        verify(worldRepository).save(any(World.class));
    }

    @Test
    void updateWorld_ShouldUpdateWorld_WhenUserIsAdmin() {
        // Given
        when(worldRepository.findById("world-123")).thenReturn(Optional.of(testWorld));
        when(worldRepository.save(any(World.class))).thenReturn(testWorld);

        // When
        Optional<WorldDto> result = registryService.updateWorld(
                "world-123", updateWorldDto, "admin-123", Arrays.asList("ADMIN"));

        // Then
        assertTrue(result.isPresent());
        verify(worldRepository).findById("world-123");
        verify(worldRepository).save(any(World.class));
    }

    @Test
    void updateWorld_ShouldReturnEmpty_WhenUserNotAuthorized() {
        // Given
        when(worldRepository.findById("world-123")).thenReturn(Optional.of(testWorld));

        // When
        Optional<WorldDto> result = registryService.updateWorld(
                "world-123", updateWorldDto, "other-user", Arrays.asList("USER"));

        // Then
        assertFalse(result.isPresent());
        verify(worldRepository).findById("world-123");
        verify(worldRepository, never()).save(any(World.class));
    }

    @Test
    void deleteWorld_ShouldDeleteWorld_WhenUserIsOwner() {
        // Given
        when(worldRepository.findById("world-123")).thenReturn(Optional.of(testWorld));

        // When
        boolean result = registryService.deleteWorld("world-123", "user-123", Arrays.asList("USER"));

        // Then
        assertTrue(result);
        verify(worldRepository).findById("world-123");
        verify(worldRepository).delete(testWorld);
    }

    @Test
    void deleteWorld_ShouldReturnFalse_WhenUserNotAuthorized() {
        // Given
        when(worldRepository.findById("world-123")).thenReturn(Optional.of(testWorld));

        // When
        boolean result = registryService.deleteWorld("world-123", "other-user", Arrays.asList("USER"));

        // Then
        assertFalse(result);
        verify(worldRepository).findById("world-123");
        verify(worldRepository, never()).delete(any(World.class));
    }

    @Test
    void enableWorld_ShouldEnableWorld_WhenUserIsOwner() {
        // Given
        testWorld.setEnabled(false);
        when(worldRepository.findById("world-123")).thenReturn(Optional.of(testWorld));
        when(worldRepository.save(any(World.class))).thenReturn(testWorld);

        // When
        Optional<WorldDto> result = registryService.enableWorld("world-123", "user-123", Arrays.asList("USER"));

        // Then
        assertTrue(result.isPresent());
        verify(worldRepository).findById("world-123");
        verify(worldRepository).save(argThat(world -> world.getEnabled()));
    }

    @Test
    void disableWorld_ShouldDisableWorld_WhenUserIsOwner() {
        // Given
        when(worldRepository.findById("world-123")).thenReturn(Optional.of(testWorld));
        when(worldRepository.save(any(World.class))).thenReturn(testWorld);

        // When
        Optional<WorldDto> result = registryService.disableWorld("world-123", "user-123", Arrays.asList("USER"));

        // Then
        assertTrue(result.isPresent());
        verify(worldRepository).findById("world-123");
        verify(worldRepository).save(argThat(world -> !world.getEnabled()));
    }
}
