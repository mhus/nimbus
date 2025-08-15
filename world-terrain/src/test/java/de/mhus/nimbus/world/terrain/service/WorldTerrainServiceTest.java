package de.mhus.nimbus.world.terrain.service;

import de.mhus.nimbus.shared.dto.world.*;
import de.mhus.nimbus.world.terrain.entity.*;
import de.mhus.nimbus.world.terrain.repository.*;
import de.mhus.nimbus.world.terrain.exception.WorldNotFoundException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.time.LocalDateTime;
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

    @InjectMocks
    private WorldTerrainService worldTerrainService;

    private WorldEntity worldEntity;
    private WorldDto worldDto;
    private MaterialEntity materialEntity;
    private MaterialDto materialDto;
    private String materialDtoJson;

    @BeforeEach
    void setUp() throws Exception {
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

        materialDtoJson = "{\"id\":1,\"name\":\"grass\",\"blocking\":false,\"friction\":0.5,\"color\":\"#00FF00\",\"texture\":\"grass.png\",\"soundWalk\":\"grass.wav\",\"properties\":{\"type\":\"natural\"}}";

        materialEntity = MaterialEntity.builder()
                .id(1)
                .name("grass")
                .data(materialDtoJson)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    void createWorld_ShouldReturnWorldDto() throws Exception {
        // Given
        when(worldRepository.save(any(WorldEntity.class))).thenReturn(worldEntity);
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"key\": \"value\"}");
        when(objectMapper.readValue(eq("{\"key\": \"value\"}"), any(TypeReference.class)))
                .thenReturn(Map.of("key", "value"));

        // When
        WorldDto result = worldTerrainService.createWorld(worldDto);

        // Then
        assertNotNull(result);
        assertEquals("Test World", result.getName());
        assertEquals("A test world", result.getDescription());
        verify(worldRepository).save(any(WorldEntity.class));
    }

    @Test
    void getWorld_ShouldReturnWorldDto_WhenWorldExists() throws Exception {
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
    void getWorld_ShouldReturnEmpty_WhenWorldDoesNotExist() {
        // Given
        when(worldRepository.findById("non-existent")).thenReturn(Optional.empty());

        // When
        Optional<WorldDto> result = worldTerrainService.getWorld("non-existent");

        // Then
        assertFalse(result.isPresent());
        verify(worldRepository).findById("non-existent");
    }

    @Test
    void createMaterial_ShouldReturnMaterialDto() throws Exception {
        // Given
        when(objectMapper.writeValueAsString(materialDto)).thenReturn(materialDtoJson);
        when(objectMapper.readValue(materialDtoJson, MaterialDto.class)).thenReturn(materialDto);
        when(materialRepository.save(any(MaterialEntity.class))).thenReturn(materialEntity);

        // When
        MaterialDto result = worldTerrainService.createMaterial(materialDto);

        // Then
        assertNotNull(result);
        assertEquals("grass", result.getName());
        assertEquals(false, result.getBlocking());
        assertEquals(0.5f, result.getFriction());
        verify(materialRepository).save(any(MaterialEntity.class));
        verify(objectMapper).writeValueAsString(materialDto);
        verify(objectMapper).readValue(materialDtoJson, MaterialDto.class);
    }

    @Test
    void getMaterial_ShouldReturnMaterialDto_WhenMaterialExists() throws Exception {
        // Given
        when(materialRepository.findById(1)).thenReturn(Optional.of(materialEntity));
        when(objectMapper.readValue(materialDtoJson, MaterialDto.class)).thenReturn(materialDto);

        // When
        Optional<MaterialDto> result = worldTerrainService.getMaterial(1);

        // Then
        assertTrue(result.isPresent());
        assertEquals("grass", result.get().getName());
        verify(materialRepository).findById(1);
        verify(objectMapper).readValue(materialDtoJson, MaterialDto.class);
    }

    @Test
    void getMaterial_ShouldReturnEmpty_WhenMaterialDoesNotExist() {
        // Given
        when(materialRepository.findById(999)).thenReturn(Optional.empty());

        // When
        Optional<MaterialDto> result = worldTerrainService.getMaterial(999);

        // Then
        assertFalse(result.isPresent());
        verify(materialRepository).findById(999);
    }

    @Test
    void getMaterials_ShouldReturnPageOfMaterialDtos() throws Exception {
        // Given
        Pageable pageable = PageRequest.of(0, 20);
        List<MaterialEntity> entities = Arrays.asList(materialEntity);
        Page<MaterialEntity> entityPage = new PageImpl<>(entities, pageable, 1);

        when(materialRepository.findAll(pageable)).thenReturn(entityPage);
        when(objectMapper.readValue(materialDtoJson, MaterialDto.class)).thenReturn(materialDto);

        // When
        Page<MaterialDto> result = worldTerrainService.getMaterials(pageable);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals("grass", result.getContent().get(0).getName());
        verify(materialRepository).findAll(pageable);
        verify(objectMapper).readValue(materialDtoJson, MaterialDto.class);
    }

    @Test
    void updateMaterial_ShouldReturnUpdatedMaterialDto() throws Exception {
        // Given
        MaterialDto updatedDto = MaterialDto.builder()
                .id(1)
                .name("updated_grass")
                .blocking(true)
                .friction(0.8f)
                .color("#00AA00")
                .texture("updated_grass.png")
                .soundWalk("updated_grass.wav")
                .properties(Map.of("type", "modified"))
                .build();

        String updatedJson = "{\"id\":1,\"name\":\"updated_grass\",\"blocking\":true,\"friction\":0.8,\"color\":\"#00AA00\",\"texture\":\"updated_grass.png\",\"soundWalk\":\"updated_grass.wav\",\"properties\":{\"type\":\"modified\"}}";

        MaterialEntity updatedEntity = MaterialEntity.builder()
                .id(1)
                .name("updated_grass")
                .data(updatedJson)
                .build();

        when(materialRepository.findById(1)).thenReturn(Optional.of(materialEntity));
        when(objectMapper.writeValueAsString(updatedDto)).thenReturn(updatedJson);
        when(materialRepository.save(any(MaterialEntity.class))).thenReturn(updatedEntity);
        when(objectMapper.readValue(updatedJson, MaterialDto.class)).thenReturn(updatedDto);

        // When
        Optional<MaterialDto> result = worldTerrainService.updateMaterial(1, updatedDto);

        // Then
        assertTrue(result.isPresent());
        assertEquals("updated_grass", result.get().getName());
        verify(materialRepository).findById(1);
        verify(materialRepository).save(any(MaterialEntity.class));
        verify(objectMapper).writeValueAsString(updatedDto);
        verify(objectMapper).readValue(updatedJson, MaterialDto.class);
    }

    @Test
    void deleteMaterial_ShouldReturnTrue_WhenMaterialExists() {
        // Given
        when(materialRepository.existsById(1)).thenReturn(true);

        // When
        boolean result = worldTerrainService.deleteMaterial(1);

        // Then
        assertTrue(result);
        verify(materialRepository).existsById(1);
        verify(materialRepository).deleteById(1);
    }

    @Test
    void deleteMaterial_ShouldReturnFalse_WhenMaterialDoesNotExist() {
        // Given
        when(materialRepository.existsById(999)).thenReturn(false);

        // When
        boolean result = worldTerrainService.deleteMaterial(999);

        // Then
        assertFalse(result);
        verify(materialRepository).existsById(999);
        verify(materialRepository, never()).deleteById(999);
    }

    @Test
    void validateWorldExists_ShouldThrowException_WhenWorldDoesNotExist() {
        // Given
        when(worldRepository.existsById("non-existent")).thenReturn(false);

        // When & Then
        assertThrows(WorldNotFoundException.class, () -> {
            worldTerrainService.getMap("non-existent", 0, 0, 0);
        });
    }

    @Test
    void createMap_ShouldValidateWorldExists() {
        // Given
        MapCreateRequest request = MapCreateRequest.builder()
                .world("non-existent")
                .clusters(Arrays.asList())
                .build();

        when(worldRepository.existsById("non-existent")).thenReturn(false);

        // When & Then
        assertThrows(WorldNotFoundException.class, () -> {
            worldTerrainService.createMap(request);
        });

        verify(worldRepository).existsById("non-existent");
    }

    @Test
    void createSprites_ShouldValidateWorldExists() {
        // Given
        SpriteCreateRequest request = SpriteCreateRequest.builder()
                .world("non-existent")
                .level(0)
                .sprites(Arrays.asList())
                .build();

        when(worldRepository.existsById("non-existent")).thenReturn(false);

        // When & Then
        assertThrows(WorldNotFoundException.class, () -> {
            worldTerrainService.createSprites(request);
        });

        verify(worldRepository).existsById("non-existent");
    }
}
