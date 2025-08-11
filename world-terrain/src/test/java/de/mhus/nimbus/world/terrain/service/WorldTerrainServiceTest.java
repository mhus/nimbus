package de.mhus.nimbus.world.terrain.service;

import de.mhus.nimbus.shared.dto.world.*;
import de.mhus.nimbus.world.terrain.entity.*;
import de.mhus.nimbus.world.terrain.repository.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WorldTerrainServiceTest {

    @Mock
    private WorldRepository worldRepository;

    @Mock
    private MaterialRepository materialRepository;

    @Mock
    private MapRepository mapRepository;

    @Mock
    private SpriteRepository spriteRepository;

    @Mock
    private TerrainGroupRepository terrainGroupRepository;

    @Mock
    private AssetRepository assetRepository;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private TypeFactory typeFactory;

    @InjectMocks
    private WorldTerrainService worldTerrainService;

    private WorldEntity worldEntity;
    private WorldDto worldDto;
    private MaterialEntity materialEntity;
    private MaterialDto materialDto;

    @BeforeEach
    void setUp() {
        // Setup test data
        worldEntity = WorldEntity.builder()
                .id("world-1")
                .name("Test World")
                .description("A test world")
                .properties("{\"key\": \"value\"}")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        worldDto = WorldDto.builder()
                .id("world-1")
                .name("Test World")
                .description("A test world")
                .properties(Map.of("key", "value"))
                .build();

        materialEntity = MaterialEntity.builder()
                .id(1)
                .name("grass")
                .blocking(false)
                .friction(0.5f)
                .color("#00FF00")
                .texture("grass.png")
                .soundWalk("grass.wav")
                .properties("{\"type\": \"natural\"}")
                .build();

        materialDto = MaterialDto.builder()
                .id(1)
                .name("grass")
                .blocking(false)
                .friction(0.5f)
                .color("#00FF00")
                .texture("grass.png")
                .soundWalk("grass.wav")
                .properties(Map.of("type", "natural"))
                .build();
    }

    @Test
    void createWorld_ShouldReturnCreatedWorld() throws Exception {
        // Given
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"key\": \"value\"}");
        when(objectMapper.readValue(eq("{\"key\": \"value\"}"), any(TypeReference.class)))
                .thenReturn(Map.of("key", "value"));
        when(worldRepository.save(any(WorldEntity.class))).thenReturn(worldEntity);

        // When
        WorldDto result = worldTerrainService.createWorld(worldDto);

        // Then
        assertNotNull(result);
        assertEquals("Test World", result.getName());
        assertEquals("A test world", result.getDescription());
        verify(worldRepository).save(any(WorldEntity.class));
    }

    @Test
    void getWorld_ShouldReturnWorld_WhenExists() throws Exception {
        // Given
        when(worldRepository.findById("world-1")).thenReturn(Optional.of(worldEntity));
        when(objectMapper.readValue(eq("{\"key\": \"value\"}"), any(TypeReference.class)))
                .thenReturn(Map.of("key", "value"));

        // When
        Optional<WorldDto> result = worldTerrainService.getWorld("world-1");

        // Then
        assertTrue(result.isPresent());
        assertEquals("Test World", result.get().getName());
        verify(worldRepository).findById("world-1");
    }

    @Test
    void getWorld_ShouldReturnEmpty_WhenNotExists() {
        // Given
        when(worldRepository.findById("non-existent")).thenReturn(Optional.empty());

        // When
        Optional<WorldDto> result = worldTerrainService.getWorld("non-existent");

        // Then
        assertFalse(result.isPresent());
        verify(worldRepository).findById("non-existent");
    }

    @Test
    void getAllWorlds_ShouldReturnAllWorlds() throws Exception {
        // Given
        when(worldRepository.findAll()).thenReturn(List.of(worldEntity));
        when(objectMapper.readValue(eq("{\"key\": \"value\"}"), any(TypeReference.class)))
                .thenReturn(Map.of("key", "value"));

        // When
        List<WorldDto> result = worldTerrainService.getAllWorlds();

        // Then
        assertEquals(1, result.size());
        assertEquals("Test World", result.getFirst().getName());
        verify(worldRepository).findAll();
    }

    @Test
    void updateWorld_ShouldReturnUpdatedWorld_WhenExists() throws Exception {
        // Given
        WorldDto updateDto = WorldDto.builder()
                .name("Updated World")
                .description("Updated description")
                .properties(Map.of("updated", "true"))
                .build();

        when(worldRepository.findById("world-1")).thenReturn(Optional.of(worldEntity));
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"updated\": \"true\"}");
        when(objectMapper.readValue(eq("{\"key\": \"value\"}"), any(TypeReference.class)))
                .thenReturn(Map.of("updated", "true"));
        when(worldRepository.save(any(WorldEntity.class))).thenReturn(worldEntity);

        // When
        Optional<WorldDto> result = worldTerrainService.updateWorld("world-1", updateDto);

        // Then
        assertTrue(result.isPresent());
        verify(worldRepository).findById("world-1");
        verify(worldRepository).save(any(WorldEntity.class));
    }

    @Test
    void deleteWorld_ShouldReturnTrue_WhenExists() {
        // Given
        when(worldRepository.existsById("world-1")).thenReturn(true);

        // When
        boolean result = worldTerrainService.deleteWorld("world-1");

        // Then
        assertTrue(result);
        verify(worldRepository).existsById("world-1");
        verify(worldRepository).deleteById("world-1");
    }

    @Test
    void deleteWorld_ShouldReturnFalse_WhenNotExists() {
        // Given
        when(worldRepository.existsById("non-existent")).thenReturn(false);

        // When
        boolean result = worldTerrainService.deleteWorld("non-existent");

        // Then
        assertFalse(result);
        verify(worldRepository).existsById("non-existent");
        verify(worldRepository, never()).deleteById(anyString());
    }

    @Test
    void createMaterial_ShouldReturnCreatedMaterial() throws Exception {
        // Given
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"type\": \"natural\"}");
        when(objectMapper.readValue(eq("{\"type\": \"natural\"}"), any(TypeReference.class)))
                .thenReturn(Map.of("type", "natural"));
        when(materialRepository.save(any(MaterialEntity.class))).thenReturn(materialEntity);

        // When
        MaterialDto result = worldTerrainService.createMaterial(materialDto);

        // Then
        assertNotNull(result);
        assertEquals("grass", result.getName());
        assertEquals(false, result.getBlocking());
        verify(materialRepository).save(any(MaterialEntity.class));
    }

    @Test
    void getMaterial_ShouldReturnMaterial_WhenExists() throws Exception {
        // Given
        when(materialRepository.findById(1)).thenReturn(Optional.of(materialEntity));
        when(objectMapper.readValue(eq("{\"type\": \"natural\"}"), any(TypeReference.class)))
                .thenReturn(Map.of("type", "natural"));

        // When
        Optional<MaterialDto> result = worldTerrainService.getMaterial(1);

        // Then
        assertTrue(result.isPresent());
        assertEquals("grass", result.get().getName());
        verify(materialRepository).findById(1);
    }

    @Test
    void getMaterials_ShouldReturnPagedMaterials() throws Exception {
        // Given
        Pageable pageable = PageRequest.of(0, 20);
        Page<MaterialEntity> page = new PageImpl<>(List.of(materialEntity));

        when(materialRepository.findAll(pageable)).thenReturn(page);
        when(objectMapper.readValue(eq("{\"type\": \"natural\"}"), any(TypeReference.class)))
                .thenReturn(Map.of("type", "natural"));

        // When
        Page<MaterialDto> result = worldTerrainService.getMaterials(null, pageable);

        // Then
        assertEquals(1, result.getContent().size());
        assertEquals("grass", result.getContent().getFirst().getName());
        verify(materialRepository).findAll(pageable);
    }

    @Test
    void getMaterials_WithNameFilter_ShouldReturnFilteredMaterials() throws Exception {
        // Given
        Pageable pageable = PageRequest.of(0, 20);
        Page<MaterialEntity> page = new PageImpl<>(List.of(materialEntity));

        when(materialRepository.findByNameContainingIgnoreCase("grass", pageable)).thenReturn(page);
        when(objectMapper.readValue(eq("{\"type\": \"natural\"}"), any(TypeReference.class)))
                .thenReturn(Map.of("type", "natural"));

        // When
        Page<MaterialDto> result = worldTerrainService.getMaterials("grass", pageable);

        // Then
        assertEquals(1, result.getContent().size());
        assertEquals("grass", result.getContent().getFirst().getName());
        verify(materialRepository).findByNameContainingIgnoreCase("grass", pageable);
    }

    @Test
    void createOrUpdateMap_ShouldSaveMapEntity() throws Exception {
        // Given
        TerrainFieldDto field = TerrainFieldDto.builder()
                .x(1).y(1).z(0)
                .materials(List.of(1, 2, 3, 4, 5, 6))
                .opacity(255)
                .sizeZ(1)
                .parameters(Map.of("test", "value"))
                .build();

        TerrainClusterDto cluster = TerrainClusterDto.builder()
                .level(0)
                .x(0).y(0)
                .fields(List.of(field))
                .build();

        MapCreateRequest request = MapCreateRequest.builder()
                .world("world-1")
                .clusters(List.of(cluster))
                .build();

        MapEntity mapEntity = MapEntity.builder()
                .world("world-1")
                .level(0)
                .clusterX(0)
                .clusterY(0)
                .data("[{\"x\":1,\"y\":1}]")
                .build();

        when(mapRepository.findByWorldAndLevelAndClusterXAndClusterY("world-1", 0, 0, 0))
                .thenReturn(Optional.of(mapEntity));
        when(objectMapper.writeValueAsString(any())).thenReturn("[{\"x\":1,\"y\":1}]");
        when(mapRepository.save(any(MapEntity.class))).thenReturn(mapEntity);

        // When
        worldTerrainService.createOrUpdateMap(request);

        // Then
        verify(mapRepository).findByWorldAndLevelAndClusterXAndClusterY("world-1", 0, 0, 0);
        verify(mapRepository).save(any(MapEntity.class));
    }

    @Test
    void getMapCluster_ShouldReturnCluster_WhenExists() throws Exception {
        // Given
        TerrainFieldDto field = TerrainFieldDto.builder()
                .x(1).y(1).z(0)
                .materials(List.of(1, 2, 3, 4, 5, 6))
                .opacity(255)
                .sizeZ(1)
                .parameters(Map.of("test", "value"))
                .build();

        MapEntity mapEntity = MapEntity.builder()
                .world("world-1")
                .level(0)
                .clusterX(0)
                .clusterY(0)
                .data("[{\"x\":1,\"y\":1}]")
                .build();

        when(mapRepository.findByWorldAndLevelAndClusterXAndClusterY("world-1", 0, 0, 0))
                .thenReturn(Optional.of(mapEntity));

        // Mock the ObjectMapper for parseJsonList method specifically
        when(objectMapper.getTypeFactory()).thenReturn(typeFactory);
        when(typeFactory.constructCollectionType(eq(List.class), eq(TerrainFieldDto.class)))
                .thenReturn(mock(com.fasterxml.jackson.databind.type.CollectionType.class));
        when(objectMapper.readValue(eq("[{\"x\":1,\"y\":1}]"), any(com.fasterxml.jackson.databind.JavaType.class)))
                .thenReturn(List.of(field));

        // When
        Optional<TerrainClusterDto> result = worldTerrainService.getMapCluster("world-1", 0, 0, 0);

        // Then
        assertTrue(result.isPresent());
        assertEquals(0, result.get().getLevel());
        assertEquals(0, result.get().getX());
        assertEquals(0, result.get().getY());
        verify(mapRepository).findByWorldAndLevelAndClusterXAndClusterY("world-1", 0, 0, 0);
    }

    @Test
    void deleteLevel_ShouldCallRepository() {
        // When
        worldTerrainService.deleteLevel("world-1", 0);

        // Then
        verify(mapRepository).deleteByWorldAndLevel("world-1", 0);
    }
}
