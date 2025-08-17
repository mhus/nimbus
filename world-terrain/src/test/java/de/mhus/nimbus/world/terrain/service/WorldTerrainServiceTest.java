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
    private SpriteRepository spriteRepository;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private WorldTerrainService worldTerrainService;

    private WorldEntity worldEntity;
    private WorldDto worldDto;
    private MaterialEntity materialEntity;
    private MaterialDto materialDto;
    private String materialDtoJson;
    private SpriteEntity spriteEntity;
    private SpriteDto spriteDto;
    private String spriteDtoJson;

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

        // Setup sprite test data
        spriteDto = SpriteDto.builder()
                .id("S12345678-1234-1234-1234-123456789abc")
                .x(10)
                .y(15)
                .z(0)
                .sizeX(2)
                .sizeY(2)
                .sizeZ(3)
                .groups(List.of(1L, 2L))
                .reference("tree-001")
                .parameters(Map.of("age", "5", "health", "100", "species", "oak"))
                .rasterType("material")
                .rasterMaterial(4)
                .rasterData(null)
                .focusType("png")
                .focusMaterial(null)
                .focusData("base64EncodedPngData...".getBytes())
                .type("tree")
                .blocking(true)
                .opacity(255)
                .enabled(true)
                .build();

        spriteDtoJson = "{\"id\":\"S12345678-1234-1234-1234-123456789abc\",\"x\":10,\"y\":15,\"z\":0,\"sizeX\":2,\"sizeY\":2,\"sizeZ\":3,\"groups\":[1,2],\"reference\":\"tree-001\",\"parameters\":{\"age\":\"5\",\"health\":\"100\",\"species\":\"oak\"},\"rasterType\":\"material\",\"rasterMaterial\":4,\"rasterData\":null,\"focusType\":\"png\",\"focusMaterial\":null,\"focusData\":\"YmFzZTY0RW5jb2RlZFBuZ0RhdGEuLi4=\",\"type\":\"tree\",\"blocking\":true,\"opacity\":255,\"enabled\":true}";

        spriteEntity = SpriteEntity.builder()
                .id("S12345678-1234-1234-1234-123456789abc")
                .world("world-1")
                .level(0)
                .enabled(true)
                .clusterX0(0)
                .clusterY0(0)
                .clusterX1(null)
                .clusterY1(null)
                .clusterX2(null)
                .clusterY2(null)
                .clusterX3(null)
                .clusterY3(null)
                .data(spriteDtoJson)
                .compressed(null)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .compressedAt(null)
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
        List<MaterialEntity> entities = List.of(materialEntity);
        Page<MaterialEntity> entityPage = new PageImpl<>(entities, pageable, 1);

        when(materialRepository.findAll(pageable)).thenReturn(entityPage);
        when(objectMapper.readValue(materialDtoJson, MaterialDto.class)).thenReturn(materialDto);

        // When
        Page<MaterialDto> result = worldTerrainService.getMaterials(pageable);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals("grass", result.getContent().getFirst().getName());
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
        assertThrows(WorldNotFoundException.class, () ->
            worldTerrainService.getMap("non-existent", 0, 0, 0));
    }

    @Test
    void createMap_ShouldValidateWorldExists() {
        // Given
        MapCreateRequest request = MapCreateRequest.builder()
                .world("non-existent")
                .clusters(List.of())
                .build();

        when(worldRepository.existsById("non-existent")).thenReturn(false);

        // When & Then
        assertThrows(WorldNotFoundException.class, () ->
            worldTerrainService.createMap(request));

        verify(worldRepository).existsById("non-existent");
    }

    @Test
    void createSprites_ShouldValidateWorldExists() {
        // Given
        SpriteCreateRequest request = SpriteCreateRequest.builder()
                .world("non-existent")
                .level(0)
                .sprites(List.of())
                .build();

        when(worldRepository.existsById("non-existent")).thenReturn(false);

        // When & Then
        assertThrows(WorldNotFoundException.class, () ->
            worldTerrainService.createSprites(request));

        verify(worldRepository).existsById("non-existent");
    }

    @Test
    void createSprites_ShouldReturnSpriteIds() throws Exception {
        // Given
        SpriteDto spriteData = SpriteDto.builder()
                .x(10)
                .y(15)
                .z(0)
                .sizeX(2)
                .sizeY(2)
                .sizeZ(3)
                .groups(List.of(1L, 2L))
                .reference("tree-001")
                .parameters(Map.of("age", "5", "health", "100", "species", "oak"))
                .rasterType("material")
                .rasterMaterial(4)
                .focusType("png")
                .focusData("base64EncodedPngData...".getBytes())
                .type("tree")
                .blocking(true)
                .opacity(255)
                .enabled(true)
                .build();

        SpriteCreateRequest request = SpriteCreateRequest.builder()
                .world("world-1")
                .level(0)
                .sprites(List.of(spriteData))
                .build();

        when(worldRepository.existsById("world-1")).thenReturn(true);
        when(objectMapper.writeValueAsString(any(SpriteDto.class))).thenReturn(spriteDtoJson);
        when(spriteRepository.save(any(SpriteEntity.class))).thenReturn(spriteEntity);

        // When
        List<String> result = worldTerrainService.createSprites(request);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue(result.getFirst().startsWith("S"));
        verify(worldRepository).existsById("world-1");
        verify(spriteRepository).save(any(SpriteEntity.class));
    }

    @Test
    void getSprite_ShouldReturnSpriteDto_WhenSpriteExists() throws Exception {
        // Given
        when(spriteRepository.findById("S12345678-1234-1234-1234-123456789abc"))
                .thenReturn(Optional.of(spriteEntity));
        when(objectMapper.readValue(spriteDtoJson, SpriteDto.class)).thenReturn(spriteDto);

        // When
        Optional<SpriteDto> result = worldTerrainService.getSprite("S12345678-1234-1234-1234-123456789abc");

        // Then
        assertTrue(result.isPresent());
        assertEquals("tree", result.get().getType());
        assertEquals(Integer.valueOf(4), result.get().getRasterMaterial());
        assertEquals("png", result.get().getFocusType());
        assertNotNull(result.get().getFocusData());
        assertEquals(true, result.get().getBlocking());
        verify(spriteRepository).findById("S12345678-1234-1234-1234-123456789abc");
        verify(objectMapper).readValue(spriteDtoJson, SpriteDto.class);
    }

    @Test
    void getSprite_ShouldReturnEmpty_WhenSpriteDoesNotExist() {
        // Given
        when(spriteRepository.findById("non-existent")).thenReturn(Optional.empty());

        // When
        Optional<SpriteDto> result = worldTerrainService.getSprite("non-existent");

        // Then
        assertFalse(result.isPresent());
        verify(spriteRepository).findById("non-existent");
    }

    @Test
    void getSpritesInCluster_ShouldReturnSpriteDtos() throws Exception {
        // Given
        List<SpriteEntity> entities = List.of(spriteEntity);
        when(worldRepository.existsById("world-1")).thenReturn(true); // Mock für Welt-Validierung hinzufügen
        when(spriteRepository.findSpritesInCluster(
                "world-1", 0, true, 0, 0)).thenReturn(entities); // Korrekte Repository-Methode verwenden
        when(objectMapper.readValue(spriteDtoJson, SpriteDto.class)).thenReturn(spriteDto);

        // When
        List<SpriteDto> result = worldTerrainService.getSpritesInCluster("world-1", 0, 0, 0);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("tree", result.getFirst().getType());
        verify(worldRepository).existsById("world-1"); // Verifikation hinzufügen
        verify(spriteRepository).findSpritesInCluster(
                "world-1", 0, true, 0, 0); // Korrekte Repository-Methode verifizieren
        verify(objectMapper).readValue(spriteDtoJson, SpriteDto.class);
    }

    @Test
    void updateSprite_ShouldReturnUpdatedSpriteDto() throws Exception {
        // Given
        SpriteDto updatedDto = SpriteDto.builder()
                .id("S12345678-1234-1234-1234-123456789abc")
                .x(10)
                .y(15)
                .z(0)
                .sizeX(2)
                .sizeY(2)
                .sizeZ(4) // Changed
                .groups(List.of(1L, 2L, 3L)) // Changed
                .reference("tree-001")
                .parameters(Map.of("age", "6", "health", "95", "species", "oak", "last_watered", "2025-08-17")) // Changed
                .rasterType("material")
                .rasterMaterial(4)
                .focusType("png")
                .focusData("updatedBase64EncodedPngData...".getBytes()) // Changed
                .type("tree")
                .blocking(true)
                .opacity(240) // Changed
                .enabled(true)
                .build();

        String updatedJson = "{\"id\":\"S12345678-1234-1234-1234-123456789abc\",\"x\":10,\"y\":15,\"z\":0,\"sizeX\":2,\"sizeY\":2,\"sizeZ\":4,\"groups\":[1,2,3],\"reference\":\"tree-001\",\"parameters\":{\"age\":\"6\",\"health\":\"95\",\"species\":\"oak\",\"last_watered\":\"2025-08-17\"},\"rasterType\":\"material\",\"rasterMaterial\":4,\"focusType\":\"png\",\"focusData\":\"dXBkYXRlZEJhc2U2NEVuY29kZWRQbmdEYXRhLi4=\",\"type\":\"tree\",\"blocking\":true,\"opacity\":240,\"enabled\":true}";

        SpriteEntity updatedEntity = SpriteEntity.builder()
                .id("S12345678-1234-1234-1234-123456789abc")
                .world("world-1")
                .level(0)
                .enabled(true)
                .clusterX0(0)
                .clusterY0(0)
                .data(updatedJson)
                .build();

        when(spriteRepository.findById("S12345678-1234-1234-1234-123456789abc"))
                .thenReturn(Optional.of(spriteEntity));
        when(objectMapper.writeValueAsString(updatedDto)).thenReturn(updatedJson);
        when(spriteRepository.save(any(SpriteEntity.class))).thenReturn(updatedEntity);
        when(objectMapper.readValue(updatedJson, SpriteDto.class)).thenReturn(updatedDto);

        // When
        Optional<SpriteDto> result = worldTerrainService.updateSprite("S12345678-1234-1234-1234-123456789abc", updatedDto);

        // Then
        assertTrue(result.isPresent());
        assertEquals(Integer.valueOf(4), result.get().getSizeZ());
        assertEquals(3, result.get().getGroups().size());
        assertEquals(Integer.valueOf(240), result.get().getOpacity());
        verify(spriteRepository).findById("S12345678-1234-1234-1234-123456789abc");
        verify(spriteRepository).save(any(SpriteEntity.class));
        verify(objectMapper).writeValueAsString(updatedDto);
        verify(objectMapper).readValue(updatedJson, SpriteDto.class);
    }

    @Test
    void enableSprite_ShouldReturnEnabledSprite() throws Exception {
        // Given
        SpriteEntity disabledEntity = SpriteEntity.builder()
                .id("S12345678-1234-1234-1234-123456789abc")
                .world("world-1")
                .level(0)
                .enabled(false) // Disabled initially
                .clusterX0(0)
                .clusterY0(0)
                .data(spriteDtoJson)
                .build();

        SpriteEntity enabledEntity = SpriteEntity.builder()
                .id("S12345678-1234-1234-1234-123456789abc")
                .world("world-1")
                .level(0)
                .enabled(true) // Enabled after update
                .clusterX0(0)
                .clusterY0(0)
                .data(spriteDtoJson)
                .build();

        when(spriteRepository.findById("S12345678-1234-1234-1234-123456789abc"))
                .thenReturn(Optional.of(disabledEntity));
        when(spriteRepository.save(any(SpriteEntity.class))).thenReturn(enabledEntity);
        when(objectMapper.readValue(spriteDtoJson, SpriteDto.class)).thenReturn(spriteDto);

        // When
        Optional<SpriteDto> result = worldTerrainService.enableSprite("S12345678-1234-1234-1234-123456789abc");

        // Then
        assertTrue(result.isPresent());
        assertEquals(true, result.get().getEnabled());
        verify(spriteRepository).findById("S12345678-1234-1234-1234-123456789abc");
        verify(spriteRepository).save(any(SpriteEntity.class));
    }

    @Test
    void disableSprite_ShouldReturnDisabledSprite() throws Exception {
        // Given
        SpriteEntity disabledEntity = SpriteEntity.builder()
                .id("S12345678-1234-1234-1234-123456789abc")
                .world("world-1")
                .level(0)
                .enabled(false) // Disabled after update
                .clusterX0(0)
                .clusterY0(0)
                .data(spriteDtoJson)
                .build();

        SpriteDto disabledDto = SpriteDto.builder()
                .id("S12345678-1234-1234-1234-123456789abc")
                .x(10)
                .y(15)
                .z(0)
                .sizeX(2)
                .sizeY(2)
                .sizeZ(3)
                .groups(List.of(1L, 2L))
                .reference("tree-001")
                .parameters(Map.of("age", "5", "health", "100", "species", "oak"))
                .rasterType("material")
                .rasterMaterial(4)
                .focusType("png")
                .focusData("base64EncodedPngData...".getBytes())
                .type("tree")
                .blocking(true)
                .opacity(255)
                .enabled(false) // Disabled
                .build();

        when(spriteRepository.findById("S12345678-1234-1234-1234-123456789abc"))
                .thenReturn(Optional.of(spriteEntity));
        when(spriteRepository.save(any(SpriteEntity.class))).thenReturn(disabledEntity);
        when(objectMapper.readValue(spriteDtoJson, SpriteDto.class)).thenReturn(disabledDto);

        // When
        Optional<SpriteDto> result = worldTerrainService.disableSprite("S12345678-1234-1234-1234-123456789abc");

        // Then
        assertTrue(result.isPresent());
        assertEquals(false, result.get().getEnabled());
        verify(spriteRepository).findById("S12345678-1234-1234-1234-123456789abc");
        verify(spriteRepository).save(any(SpriteEntity.class));
    }

    @Test
    void deleteSprite_ShouldReturnTrue_WhenSpriteExists() {
        // Given
        when(spriteRepository.existsById("S12345678-1234-1234-1234-123456789abc")).thenReturn(true);

        // When
        boolean result = worldTerrainService.deleteSprite("S12345678-1234-1234-1234-123456789abc");

        // Then
        assertTrue(result);
        verify(spriteRepository).existsById("S12345678-1234-1234-1234-123456789abc");
        verify(spriteRepository).deleteById("S12345678-1234-1234-1234-123456789abc");
    }

    @Test
    void deleteSprite_ShouldReturnFalse_WhenSpriteDoesNotExist() {
        // Given
        when(spriteRepository.existsById("non-existent")).thenReturn(false);

        // When
        boolean result = worldTerrainService.deleteSprite("non-existent");

        // Then
        assertFalse(result);
        verify(spriteRepository).existsById("non-existent");
        verify(spriteRepository, never()).deleteById("non-existent");
    }

    @Test
    void createSprites_ShouldHandleMaterialBasedSprite() throws Exception {
        // Given - Sprite with only material references (no raw data)
        SpriteDto materialBasedSprite = SpriteDto.builder()
                .x(20)
                .y(25)
                .z(0)
                .sizeX(1)
                .sizeY(1)
                .sizeZ(1)
                .groups(List.of(3L))
                .reference("rock-001")
                .parameters(Map.of("hardness", "8", "weight", "500"))
                .rasterType("material")
                .rasterMaterial(5) // Material-based raster
                .rasterData(null)
                .focusType("material")
                .focusMaterial(6) // Material-based focus
                .focusData(null)
                .type("rock")
                .blocking(true)
                .opacity(255)
                .enabled(true)
                .build();

        SpriteCreateRequest request = SpriteCreateRequest.builder()
                .world("world-1")
                .level(0)
                .sprites(List.of(materialBasedSprite))
                .build();

        String materialBasedJson = "{\"rasterType\":\"material\",\"rasterMaterial\":5,\"rasterData\":null,\"focusType\":\"material\",\"focusMaterial\":6,\"focusData\":null,\"type\":\"rock\"}";

        when(worldRepository.existsById("world-1")).thenReturn(true);
        when(objectMapper.writeValueAsString(any(SpriteDto.class))).thenReturn(materialBasedJson);
        when(spriteRepository.save(any(SpriteEntity.class))).thenReturn(spriteEntity);

        // When
        List<String> result = worldTerrainService.createSprites(request);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue(result.getFirst().startsWith("S"));
        verify(worldRepository).existsById("world-1");
        verify(spriteRepository).save(any(SpriteEntity.class));
    }
}
