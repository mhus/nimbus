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

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GeneratorServiceTest {

    @Mock
    private WorldGeneratorRepository worldGeneratorRepository;

    @Mock
    private WorldGeneratorPhaseRepository phaseRepository;

    @InjectMocks
    private GeneratorService generatorService;

    private WorldGenerator testWorldGenerator;
    private List<WorldGeneratorPhase> testPhases;

    @BeforeEach
    void setUp() {
        testWorldGenerator = WorldGenerator.builder()
                .id(1L)
                .worldId("test-world-001")
                .name("Test Fantasy World")
                .description("Eine Testwelt für Unit Tests")
                .status(WorldGenerator.GenerationStatus.PENDING)
                .parameters(Map.of(
                        "worldSize", "medium",
                        "biomeVariety", "high",
                        "magicLevel", "medium"
                ))
                .totalPhases(8)
                .completedPhases(0)
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
                .build();
    }

    @Test
    void createWorldGenerator_ShouldCreateWithAllPhases() {
        // Given
        when(worldGeneratorRepository.save(any(WorldGenerator.class))).thenReturn(testWorldGenerator);
        when(phaseRepository.saveAll(anyList())).thenReturn(testPhases);

        // When
        WorldGenerator result = generatorService.createWorldGenerator(
                "test-world-001",
                "Test Fantasy World",
                "Eine Testwelt für Unit Tests",
                Map.of("worldSize", "medium")
        );

        // Then
        assertNotNull(result);
        assertEquals("test-world-001", result.getWorldId());
        assertEquals("Test Fantasy World", result.getName());
        assertEquals(WorldGenerator.GenerationStatus.PENDING, result.getStatus());
        verify(worldGeneratorRepository).save(any(WorldGenerator.class));
        verify(phaseRepository).saveAll(anyList());
    }

    @Test
    void getAllWorldGenerators_ShouldReturnAllGenerators() {
        // Given
        List<WorldGenerator> expectedGenerators = Arrays.asList(testWorldGenerator);
        when(worldGeneratorRepository.findAll()).thenReturn(expectedGenerators);

        // When
        List<WorldGenerator> result = generatorService.getAllWorldGenerators();

        // Then
        assertEquals(expectedGenerators, result);
        verify(worldGeneratorRepository).findAll();
    }

    @Test
    void getWorldGeneratorById_ShouldReturnGenerator() {
        // Given
        when(worldGeneratorRepository.findById(1L)).thenReturn(Optional.of(testWorldGenerator));

        // When
        Optional<WorldGenerator> result = generatorService.getWorldGeneratorById(1L);

        // Then
        assertTrue(result.isPresent());
        assertEquals(testWorldGenerator, result.get());
        verify(worldGeneratorRepository).findById(1L);
    }

    @Test
    void getWorldGeneratorByWorldId_ShouldReturnGenerator() {
        // Given
        when(worldGeneratorRepository.findByWorldId("test-world-001")).thenReturn(Optional.of(testWorldGenerator));

        // When
        Optional<WorldGenerator> result = generatorService.getWorldGeneratorByWorldId("test-world-001");

        // Then
        assertTrue(result.isPresent());
        assertEquals(testWorldGenerator, result.get());
        verify(worldGeneratorRepository).findByWorldId("test-world-001");
    }

    @Test
    void startGeneration_ShouldUpdateStatusAndStartFirstPhase() {
        // Given
        testWorldGenerator.setPhases(testPhases);
        when(worldGeneratorRepository.findById(1L)).thenReturn(Optional.of(testWorldGenerator));
        when(worldGeneratorRepository.save(any(WorldGenerator.class))).thenReturn(testWorldGenerator);
        when(phaseRepository.save(any(WorldGeneratorPhase.class))).thenReturn(testPhases.get(0));

        // When
        boolean result = generatorService.startGeneration(1L);

        // Then
        assertTrue(result);
        assertEquals(WorldGenerator.GenerationStatus.RUNNING, testWorldGenerator.getStatus());
        assertNotNull(testWorldGenerator.getStartedAt());
        verify(worldGeneratorRepository).save(testWorldGenerator);
        verify(phaseRepository).save(any(WorldGeneratorPhase.class));
    }

    @Test
    void completePhase_ShouldMarkPhaseAsCompletedAndStartNext() {
        // Given
        WorldGeneratorPhase firstPhase = testPhases.get(0);
        WorldGeneratorPhase secondPhase = testPhases.get(1);

        firstPhase.setStatus(WorldGeneratorPhase.PhaseStatus.RUNNING);
        firstPhase.setStartedAt(LocalDateTime.now().minusMinutes(5));

        testWorldGenerator.setPhases(testPhases);
        testWorldGenerator.setCurrentPhase(firstPhase.getPhaseType());

        when(phaseRepository.findById(1L)).thenReturn(Optional.of(firstPhase));
        when(phaseRepository.save(any(WorldGeneratorPhase.class))).thenReturn(firstPhase);
        when(worldGeneratorRepository.save(any(WorldGenerator.class))).thenReturn(testWorldGenerator);

        // When
        boolean result = generatorService.completePhase(1L, "Phase completed successfully");

        // Then
        assertTrue(result);
        assertEquals(WorldGeneratorPhase.PhaseStatus.COMPLETED, firstPhase.getStatus());
        assertNotNull(firstPhase.getCompletedAt());
        assertEquals(100, firstPhase.getProgressPercentage());
        assertEquals("Phase completed successfully", firstPhase.getResultSummary());
        verify(phaseRepository, times(2)).save(any(WorldGeneratorPhase.class)); // firstPhase + secondPhase
        verify(worldGeneratorRepository).save(testWorldGenerator);
    }

    @Test
    void completeGeneration_ShouldMarkGeneratorAsCompleted() {
        // Given
        testPhases.forEach(phase -> phase.setStatus(WorldGeneratorPhase.PhaseStatus.COMPLETED));
        testWorldGenerator.setPhases(testPhases);
        testWorldGenerator.setStatus(WorldGenerator.GenerationStatus.RUNNING);
        testWorldGenerator.setCompletedPhases(7);

        when(worldGeneratorRepository.findById(1L)).thenReturn(Optional.of(testWorldGenerator));
        when(worldGeneratorRepository.save(any(WorldGenerator.class))).thenReturn(testWorldGenerator);

        // When
        boolean result = generatorService.completeGeneration(1L);

        // Then
        assertTrue(result);
        assertEquals(WorldGenerator.GenerationStatus.COMPLETED, testWorldGenerator.getStatus());
        assertNotNull(testWorldGenerator.getCompletedAt());
        assertEquals(8, testWorldGenerator.getCompletedPhases());
        verify(worldGeneratorRepository).save(testWorldGenerator);
    }

    @Test
    void failPhase_ShouldMarkPhaseAndGeneratorAsFailed() {
        // Given
        WorldGeneratorPhase failingPhase = testPhases.get(2);
        failingPhase.setStatus(WorldGeneratorPhase.PhaseStatus.RUNNING);
        testWorldGenerator.setStatus(WorldGenerator.GenerationStatus.RUNNING);

        when(phaseRepository.findById(3L)).thenReturn(Optional.of(failingPhase));
        when(phaseRepository.save(any(WorldGeneratorPhase.class))).thenReturn(failingPhase);
        when(worldGeneratorRepository.save(any(WorldGenerator.class))).thenReturn(testWorldGenerator);

        // When
        boolean result = generatorService.failPhase(3L, "Terrain generation failed due to invalid parameters");

        // Then
        assertTrue(result);
        assertEquals(WorldGeneratorPhase.PhaseStatus.FAILED, failingPhase.getStatus());
        assertEquals("Terrain generation failed due to invalid parameters", failingPhase.getErrorMessage());
        assertEquals(WorldGenerator.GenerationStatus.FAILED, testWorldGenerator.getStatus());
        verify(phaseRepository).save(failingPhase);
        verify(worldGeneratorRepository).save(testWorldGenerator);
    }

    @Test
    void getPhasesByWorldGenerator_ShouldReturnAllPhases() {
        // Given
        when(phaseRepository.findByWorldGeneratorIdOrderByPhaseOrder(1L)).thenReturn(testPhases);

        // When
        List<WorldGeneratorPhase> result = generatorService.getPhasesByWorldGenerator(1L);

        // Then
        assertEquals(testPhases, result);
        assertEquals(8, result.size());
        verify(phaseRepository).findByWorldGeneratorIdOrderByPhaseOrder(1L);
    }

    @Test
    void updatePhaseProgress_ShouldUpdateProgressPercentage() {
        // Given
        WorldGeneratorPhase runningPhase = testPhases.get(1);
        runningPhase.setStatus(WorldGeneratorPhase.PhaseStatus.RUNNING);

        when(phaseRepository.findById(2L)).thenReturn(Optional.of(runningPhase));
        when(phaseRepository.save(any(WorldGeneratorPhase.class))).thenReturn(runningPhase);

        // When
        boolean result = generatorService.updatePhaseProgress(2L, 75);

        // Then
        assertTrue(result);
        assertEquals(75, runningPhase.getProgressPercentage());
        verify(phaseRepository).save(runningPhase);
    }

    @Test
    void getWorldGeneratorsByStatus_ShouldReturnFilteredGenerators() {
        // Given
        List<WorldGenerator> completedGenerators = Arrays.asList(testWorldGenerator);
        when(worldGeneratorRepository.findByStatus(WorldGenerator.GenerationStatus.COMPLETED))
                .thenReturn(completedGenerators);

        // When
        List<WorldGenerator> result = generatorService.getWorldGeneratorsByStatus(
                WorldGenerator.GenerationStatus.COMPLETED);

        // Then
        assertEquals(completedGenerators, result);
        verify(worldGeneratorRepository).findByStatus(WorldGenerator.GenerationStatus.COMPLETED);
    }

    @Test
    void deleteWorldGenerator_ShouldDeleteGeneratorAndPhases() {
        // Given
        when(worldGeneratorRepository.findById(1L)).thenReturn(Optional.of(testWorldGenerator));

        // When
        boolean result = generatorService.deleteWorldGenerator(1L);

        // Then
        assertTrue(result);
        verify(worldGeneratorRepository).delete(testWorldGenerator);
    }

    @Test
    void calculateProgressPercentage_ShouldReturnCorrectPercentage() {
        // Given
        testWorldGenerator.setTotalPhases(8);
        testWorldGenerator.setCompletedPhases(3);

        // When
        double progress = testWorldGenerator.getProgressPercentage();

        // Then
        assertEquals(37.5, progress, 0.01);
    }

    @Test
    void phaseTypes_ShouldHaveCorrectDisplayNames() {
        // Then
        assertEquals("Initialisierung",
                WorldGeneratorPhase.PHASE_DISPLAY_NAMES.get(WorldGeneratorPhase.PHASE_INITIALIZATION));
        assertEquals("Asset/Material-Generierung",
                WorldGeneratorPhase.PHASE_DISPLAY_NAMES.get(WorldGeneratorPhase.PHASE_ASSET_MATERIAL_GENERATION));
        assertEquals("Kontinent-Generierung",
                WorldGeneratorPhase.PHASE_DISPLAY_NAMES.get(WorldGeneratorPhase.PHASE_CONTINENT_GENERATION));
        assertEquals("Terrain-Generierung",
                WorldGeneratorPhase.PHASE_DISPLAY_NAMES.get(WorldGeneratorPhase.PHASE_TERRAIN_GENERATION));
        assertEquals("Historische Generierung",
                WorldGeneratorPhase.PHASE_DISPLAY_NAMES.get(WorldGeneratorPhase.PHASE_HISTORICAL_GENERATION));
        assertEquals("Struktur-Generierung",
                WorldGeneratorPhase.PHASE_DISPLAY_NAMES.get(WorldGeneratorPhase.PHASE_STRUCTURE_GENERATION));
        assertEquals("Item-Generierung",
                WorldGeneratorPhase.PHASE_DISPLAY_NAMES.get(WorldGeneratorPhase.PHASE_ITEM_GENERATION));
        assertEquals("Quest-Generierung",
                WorldGeneratorPhase.PHASE_DISPLAY_NAMES.get(WorldGeneratorPhase.PHASE_QUEST_GENERATION));
    }

    @Test
    void getDefaultPhaseTypes_ShouldReturnAllPhaseTypes() {
        // When
        String[] phaseTypes = WorldGeneratorPhase.getDefaultPhaseTypes();

        // Then
        assertEquals(8, phaseTypes.length);
        assertEquals(WorldGeneratorPhase.PHASE_INITIALIZATION, phaseTypes[0]);
        assertEquals(WorldGeneratorPhase.PHASE_ASSET_MATERIAL_GENERATION, phaseTypes[1]);
        assertEquals(WorldGeneratorPhase.PHASE_CONTINENT_GENERATION, phaseTypes[2]);
        assertEquals(WorldGeneratorPhase.PHASE_TERRAIN_GENERATION, phaseTypes[3]);
        assertEquals(WorldGeneratorPhase.PHASE_HISTORICAL_GENERATION, phaseTypes[4]);
        assertEquals(WorldGeneratorPhase.PHASE_STRUCTURE_GENERATION, phaseTypes[5]);
        assertEquals(WorldGeneratorPhase.PHASE_ITEM_GENERATION, phaseTypes[6]);
        assertEquals(WorldGeneratorPhase.PHASE_QUEST_GENERATION, phaseTypes[7]);
    }

    @Test
    void getPhaseDisplayName_ShouldReturnCorrectDisplayName() {
        // Given
        WorldGeneratorPhase phase = createTestPhase(WorldGeneratorPhase.PHASE_TERRAIN_GENERATION, 1);

        // When
        String displayName = phase.getPhaseDisplayName();

        // Then
        assertEquals("Terrain-Generierung", displayName);
    }

    @Test
    void getPhaseDisplayName_ShouldReturnPhaseTypeForUnknownPhase() {
        // Given
        WorldGeneratorPhase phase = createTestPhase("CUSTOM_PHASE", 1);

        // When
        String displayName = phase.getPhaseDisplayName();

        // Then
        assertEquals("CUSTOM_PHASE", displayName);
    }
}
