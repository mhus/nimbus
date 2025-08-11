package de.mhus.nimbus.world.terrain.service;

import de.mhus.nimbus.shared.dto.terrain.MaterialDto;
import de.mhus.nimbus.world.terrain.entity.Material;
import de.mhus.nimbus.world.terrain.repository.MaterialRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WorldTerrainServiceTest {

    @Mock
    private MaterialRepository materialRepository;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    private WorldTerrainService worldTerrainService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        worldTerrainService = new WorldTerrainService(
            materialRepository,
            null, // mapClusterRepository
            null, // spriteRepository
            null, // assetRepository
            null, // terrainGroupRepository
            objectMapper,
            kafkaTemplate
        );
    }

    @Test
    void createMaterial_ShouldCreateAndReturnMaterial() {
        // Given
        MaterialDto inputDto = MaterialDto.builder()
            .name("grass")
            .blocking(false)
            .friction(0.5f)
            .color("#00FF00")
            .texture("grass.png")
            .soundWalk("grass.wav")
            .properties(Map.of("key", "value"))
            .build();

        Material savedMaterial = Material.builder()
            .id(1)
            .name("grass")
            .blocking(false)
            .friction(0.5f)
            .color("#00FF00")
            .texture("grass.png")
            .soundWalk("grass.wav")
            .properties("{\"key\":\"value\"}")
            .build();

        when(materialRepository.save(any(Material.class))).thenReturn(savedMaterial);

        // When
        MaterialDto result = worldTerrainService.createMaterial(inputDto);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getId());
        assertEquals("grass", result.getName());
        assertEquals(false, result.getBlocking());
        assertEquals(0.5f, result.getFriction());
        assertEquals("#00FF00", result.getColor());
        assertEquals("grass.png", result.getTexture());
        assertEquals("grass.wav", result.getSoundWalk());
        assertEquals(Map.of("key", "value"), result.getProperties());

        verify(materialRepository).save(any(Material.class));
        verify(kafkaTemplate).send(eq("terrain.material.events"), eq("material.created"), any());
    }

    @Test
    void getMaterial_WhenMaterialExists_ShouldReturnMaterial() {
        // Given
        Integer materialId = 1;
        Material material = Material.builder()
            .id(materialId)
            .name("water")
            .blocking(true)
            .friction(0.1f)
            .color("#0000FF")
            .texture("water.png")
            .soundWalk("water.wav")
            .properties("{\"transparent\":\"true\"}")
            .build();

        when(materialRepository.findById(materialId)).thenReturn(Optional.of(material));

        // When
        Optional<MaterialDto> result = worldTerrainService.getMaterial(materialId);

        // Then
        assertTrue(result.isPresent());
        MaterialDto materialDto = result.get();
        assertEquals(materialId, materialDto.getId());
        assertEquals("water", materialDto.getName());
        assertEquals(true, materialDto.getBlocking());
        assertEquals(0.1f, materialDto.getFriction());
        assertEquals("#0000FF", materialDto.getColor());
        assertEquals("water.png", materialDto.getTexture());
        assertEquals("water.wav", materialDto.getSoundWalk());
        assertTrue(materialDto.getProperties().containsKey("transparent"));

        verify(materialRepository).findById(materialId);
    }

    @Test
    void getMaterial_WhenMaterialDoesNotExist_ShouldReturnEmpty() {
        // Given
        Integer materialId = 999;
        when(materialRepository.findById(materialId)).thenReturn(Optional.empty());

        // When
        Optional<MaterialDto> result = worldTerrainService.getMaterial(materialId);

        // Then
        assertFalse(result.isPresent());
        verify(materialRepository).findById(materialId);
    }

    @Test
    void getMaterials_ShouldReturnPagedResults() {
        // Given
        Material material1 = Material.builder().id(1).name("grass").blocking(false).friction(0.5f).build();
        Material material2 = Material.builder().id(2).name("stone").blocking(true).friction(0.8f).build();

        Page<Material> materialPage = new PageImpl<>(List.of(material1, material2));
        when(materialRepository.findAll(any(PageRequest.class))).thenReturn(materialPage);

        // When
        Page<MaterialDto> result = worldTerrainService.getMaterials(0, 20);

        // Then
        assertNotNull(result);
        assertEquals(2, result.getContent().size());
        assertEquals("grass", result.getContent().get(0).getName());
        assertEquals("stone", result.getContent().get(1).getName());

        verify(materialRepository).findAll(any(PageRequest.class));
    }

    @Test
    void updateMaterial_WhenMaterialExists_ShouldUpdateAndReturnMaterial() {
        // Given
        Integer materialId = 1;
        Material existingMaterial = Material.builder()
            .id(materialId)
            .name("oldName")
            .blocking(false)
            .friction(0.3f)
            .build();

        MaterialDto updateDto = MaterialDto.builder()
            .name("newName")
            .blocking(true)
            .friction(0.7f)
            .color("#FF0000")
            .build();

        Material updatedMaterial = Material.builder()
            .id(materialId)
            .name("newName")
            .blocking(true)
            .friction(0.7f)
            .color("#FF0000")
            .build();

        when(materialRepository.findById(materialId)).thenReturn(Optional.of(existingMaterial));
        when(materialRepository.save(any(Material.class))).thenReturn(updatedMaterial);

        // When
        Optional<MaterialDto> result = worldTerrainService.updateMaterial(materialId, updateDto);

        // Then
        assertTrue(result.isPresent());
        MaterialDto materialDto = result.get();
        assertEquals("newName", materialDto.getName());
        assertEquals(true, materialDto.getBlocking());
        assertEquals(0.7f, materialDto.getFriction());
        assertEquals("#FF0000", materialDto.getColor());

        verify(materialRepository).findById(materialId);
        verify(materialRepository).save(any(Material.class));
        verify(kafkaTemplate).send(eq("terrain.material.events"), eq("material.updated"), any());
    }

    @Test
    void updateMaterial_WhenMaterialDoesNotExist_ShouldReturnEmpty() {
        // Given
        Integer materialId = 999;
        MaterialDto updateDto = MaterialDto.builder().name("newName").build();

        when(materialRepository.findById(materialId)).thenReturn(Optional.empty());

        // When
        Optional<MaterialDto> result = worldTerrainService.updateMaterial(materialId, updateDto);

        // Then
        assertFalse(result.isPresent());
        verify(materialRepository).findById(materialId);
        verify(materialRepository, never()).save(any());
        verify(kafkaTemplate, never()).send(anyString(), anyString(), any());
    }

    @Test
    void deleteMaterial_WhenMaterialExists_ShouldDeleteAndReturnTrue() {
        // Given
        Integer materialId = 1;
        when(materialRepository.existsById(materialId)).thenReturn(true);

        // When
        boolean result = worldTerrainService.deleteMaterial(materialId);

        // Then
        assertTrue(result);
        verify(materialRepository).existsById(materialId);
        verify(materialRepository).deleteById(materialId);
        verify(kafkaTemplate).send(eq("terrain.material.events"), eq("material.deleted"), any());
    }

    @Test
    void deleteMaterial_WhenMaterialDoesNotExist_ShouldReturnFalse() {
        // Given
        Integer materialId = 999;
        when(materialRepository.existsById(materialId)).thenReturn(false);

        // When
        boolean result = worldTerrainService.deleteMaterial(materialId);

        // Then
        assertFalse(result);
        verify(materialRepository).existsById(materialId);
        verify(materialRepository, never()).deleteById(anyInt());
        verify(kafkaTemplate, never()).send(anyString(), anyString(), any());
    }
}
