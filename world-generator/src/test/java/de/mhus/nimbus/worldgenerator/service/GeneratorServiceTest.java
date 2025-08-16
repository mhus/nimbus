package de.mhus.nimbus.worldgenerator.service;

import de.mhus.nimbus.worldgenerator.entity.WorldGenerator;
import de.mhus.nimbus.worldgenerator.entity.WorldGeneratorPhase;
import de.mhus.nimbus.worldgenerator.processor.PhaseProcessor;
import de.mhus.nimbus.worldgenerator.repository.WorldGeneratorRepository;
import de.mhus.nimbus.worldgenerator.repository.WorldGeneratorPhaseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GeneratorServiceTest {

    @Mock
    private WorldGeneratorRepository worldGeneratorRepository;

    @Mock
    private WorldGeneratorPhaseRepository worldGeneratorPhaseRepository;

    @Mock
    private Map<String, PhaseProcessor> phaseProcessors;

    @Mock
    private PhaseProcessor mockProcessor;

    @InjectMocks
    private GeneratorService generatorService;

    private WorldGenerator testWorldGenerator;
    private WorldGeneratorPhase testPhase;

    @BeforeEach
    void setUp() {
        testWorldGenerator = WorldGenerator.builder()
                .id(1L)
                .name("Test World")
                .description("Test Description")
                .status("INITIALIZED")
                .parameters(Map.of("size", "large"))
                .build();

        testPhase = WorldGeneratorPhase.builder()
                .id(1L)
                .worldGenerator(testWorldGenerator)
                .processor("terrainProcessor")
                .name("Terrain Generation")
                .description("Generate basic terrain")
                .phaseOrder(1)
                .status("PENDING")
                .parameters(Map.of("biome", "forest"))
                .build();
    }

    @Test
    void testCreateWorldGenerator_Success() {
        // Given
        when(worldGeneratorRepository.findByName("Test World")).thenReturn(Optional.empty());
        when(worldGeneratorRepository.save(any(WorldGenerator.class))).thenReturn(testWorldGenerator);

        // When
        WorldGenerator result = generatorService.createWorldGenerator(
                "Test World",
                "Test Description",
                Map.of("size", "large")
        );

        // Then
        assertNotNull(result);
        assertEquals("Test World", result.getName());
        assertEquals("Test Description", result.getDescription());
        assertEquals("INITIALIZED", result.getStatus());
        verify(worldGeneratorRepository).save(any(WorldGenerator.class));
    }

    @Test
    void testCreateWorldGenerator_DuplicateName() {
        // Given
        when(worldGeneratorRepository.findByName("Test World")).thenReturn(Optional.of(testWorldGenerator));

        // When & Then
        assertThrows(IllegalArgumentException.class, () ->
            generatorService.createWorldGenerator("Test World", "Test Description", Map.of())
        );

        verify(worldGeneratorRepository, never()).save(any(WorldGenerator.class));
    }

    @Test
    void testAddPhase_Success() {
        // Given
        when(worldGeneratorRepository.findById(1L)).thenReturn(Optional.of(testWorldGenerator));
        when(worldGeneratorPhaseRepository.save(any(WorldGeneratorPhase.class))).thenReturn(testPhase);

        // When
        WorldGeneratorPhase result = generatorService.addPhase(
                1L,
                "terrainProcessor",
                "Terrain Generation",
                "Generate basic terrain",
                1,
                Map.of("biome", "forest")
        );

        // Then
        assertNotNull(result);
        assertEquals("Terrain Generation", result.getName());
        assertEquals("terrainProcessor", result.getProcessor());
        assertEquals("PENDING", result.getStatus());
        verify(worldGeneratorPhaseRepository).save(any(WorldGeneratorPhase.class));
    }

    @Test
    void testAddPhase_WorldGeneratorNotFound() {
        // Given
        when(worldGeneratorRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(IllegalArgumentException.class, () ->
            generatorService.addPhase(1L, "terrainProcessor", "Test Phase", "Description", 1, Map.of())
        );

        verify(worldGeneratorPhaseRepository, never()).save(any(WorldGeneratorPhase.class));
    }

    @Test
    void testStartGeneration_Success() {
        // Given
        testWorldGenerator.setStatus("INITIALIZED");
        when(worldGeneratorRepository.findById(1L)).thenReturn(Optional.of(testWorldGenerator));
        when(worldGeneratorPhaseRepository.findActivePhasesByWorldGeneratorId(1L))
                .thenReturn(List.of(testPhase));
        when(phaseProcessors.get("terrainProcessor")).thenReturn(mockProcessor);

        // When
        generatorService.startGeneration(1L);

        // Then
        verify(worldGeneratorRepository, times(2)).save(testWorldGenerator);
        verify(worldGeneratorPhaseRepository, times(2)).save(testPhase); // Zweimal: IN_PROGRESS und im finally-Block
        assertEquals("COMPLETED", testWorldGenerator.getStatus());
    }

    @Test
    void testProcessPhase_Success() throws Exception {
        // Given
        when(phaseProcessors.get("terrainProcessor")).thenReturn(mockProcessor);
        doNothing().when(mockProcessor).processPhase(any());

        // When
        generatorService.processPhase(testPhase);

        // Then
        assertEquals("COMPLETED", testPhase.getStatus());
        verify(mockProcessor).processPhase(any());
        verify(worldGeneratorPhaseRepository, times(2)).save(testPhase);
    }

    @Test
    void testProcessPhase_ProcessorNotFound() {
        // Given
        when(phaseProcessors.get("terrainProcessor")).thenReturn(null);

        // When & Then
        assertThrows(IllegalArgumentException.class, () ->
            generatorService.processPhase(testPhase)
        );
    }

    @Test
    void testProcessPhase_ProcessorThrowsException() throws Exception {
        // Given
        when(phaseProcessors.get("terrainProcessor")).thenReturn(mockProcessor);
        doThrow(new RuntimeException("Processing error")).when(mockProcessor).processPhase(any());

        // When
        generatorService.processPhase(testPhase);

        // Then
        assertEquals("ERROR", testPhase.getStatus());
        verify(worldGeneratorPhaseRepository, times(2)).save(testPhase); // Einmal für IN_PROGRESS, einmal im finally
        verify(mockProcessor).processPhase(any());
    }

    @Test
    void testGetAllWorldGenerators() {
        // Given
        List<WorldGenerator> generators = List.of(testWorldGenerator);
        when(worldGeneratorRepository.findAllOrderByUpdatedAtDesc()).thenReturn(generators);

        // When
        List<WorldGenerator> result = generatorService.getAllWorldGenerators();

        // Then
        assertEquals(1, result.size());
        assertEquals(testWorldGenerator, result.get(0));
    }

    @Test
    void testGetWorldGenerator() {
        // Given
        when(worldGeneratorRepository.findById(1L)).thenReturn(Optional.of(testWorldGenerator));

        // When
        Optional<WorldGenerator> result = generatorService.getWorldGenerator(1L);

        // Then
        assertTrue(result.isPresent());
        assertEquals(testWorldGenerator, result.get());
    }

    @Test
    void testDeleteWorldGenerator() {
        // When
        generatorService.deleteWorldGenerator(1L);

        // Then
        verify(worldGeneratorRepository).deleteById(1L);
    }

    @Test
    void testArchivePhase() {
        // Given
        when(worldGeneratorPhaseRepository.findById(1L)).thenReturn(Optional.of(testPhase));

        // When
        generatorService.archivePhase(1L);

        // Then
        assertTrue(testPhase.getArchived());
        verify(worldGeneratorPhaseRepository).save(testPhase);
    }
}
