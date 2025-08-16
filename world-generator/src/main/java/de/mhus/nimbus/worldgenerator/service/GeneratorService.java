package de.mhus.nimbus.worldgenerator.service;

import de.mhus.nimbus.worldgenerator.entity.WorldGenerator;
import de.mhus.nimbus.worldgenerator.entity.WorldGeneratorPhase;
import de.mhus.nimbus.worldgenerator.model.PhaseInfo;
import de.mhus.nimbus.worldgenerator.processor.PhaseProcessor;
import de.mhus.nimbus.worldgenerator.repository.WorldGeneratorRepository;
import de.mhus.nimbus.worldgenerator.repository.WorldGeneratorPhaseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class GeneratorService {

    private final WorldGeneratorRepository worldGeneratorRepository;
    private final WorldGeneratorPhaseRepository worldGeneratorPhaseRepository;
    private final Map<String, PhaseProcessor> phaseProcessors;

    @Transactional
    public WorldGenerator createWorldGenerator(String name, String description, Map<String, Object> parameters) {
        log.info("Creating new world generator: {}", name);

        if (worldGeneratorRepository.findByName(name).isPresent()) {
            throw new IllegalArgumentException("World generator with name '" + name + "' already exists");
        }

        WorldGenerator worldGenerator = WorldGenerator.builder()
                .name(name)
                .description(description)
                .status("INITIALIZED")
                .parameters(parameters)
                .build();

        return worldGeneratorRepository.save(worldGenerator);
    }

    @Transactional
    public WorldGeneratorPhase addPhase(Long worldGeneratorId, String processor, String name,
                                       String description, Integer phaseOrder, Map<String, Object> parameters) {
        log.info("Adding phase '{}' to world generator {}", name, worldGeneratorId);

        WorldGenerator worldGenerator = worldGeneratorRepository.findById(worldGeneratorId)
                .orElseThrow(() -> new IllegalArgumentException("World generator not found: " + worldGeneratorId));

        WorldGeneratorPhase phase = WorldGeneratorPhase.builder()
                .worldGenerator(worldGenerator)
                .processor(processor)
                .name(name)
                .description(description)
                .phaseOrder(phaseOrder)
                .status("PENDING")
                .parameters(parameters)
                .build();

        return worldGeneratorPhaseRepository.save(phase);
    }

    @Transactional
    public void startGeneration(Long worldGeneratorId) {
        log.info("Starting generation for world generator {}", worldGeneratorId);

        WorldGenerator worldGenerator = worldGeneratorRepository.findById(worldGeneratorId)
                .orElseThrow(() -> new IllegalArgumentException("World generator not found: " + worldGeneratorId));

        worldGenerator.setStatus("GENERATING");
        worldGeneratorRepository.save(worldGenerator);

        // Process phases in order
        List<WorldGeneratorPhase> phases = worldGeneratorPhaseRepository
                .findActivePhasesByWorldGeneratorId(worldGeneratorId);

        for (WorldGeneratorPhase phase : phases) {
            if ("PENDING".equals(phase.getStatus())) {
                processPhase(phase);
            }
        }

        // Check if all phases are completed
        boolean allCompleted = phases.stream()
                .allMatch(phase -> "COMPLETED".equals(phase.getStatus()));

        if (allCompleted) {
            worldGenerator.setStatus("COMPLETED");
            worldGeneratorRepository.save(worldGenerator);
            log.info("World generation completed for generator {}", worldGeneratorId);
        }
    }

    @Transactional
    public void processPhase(WorldGeneratorPhase phase) {
        log.info("Processing phase: {} with processor: {}", phase.getName(), phase.getProcessor());

        PhaseProcessor processor = phaseProcessors.get(phase.getProcessor());
        if (processor == null) {
            throw new IllegalArgumentException("No processor found for type: " + phase.getProcessor());
        }

        phase.setStatus("IN_PROGRESS");
        worldGeneratorPhaseRepository.save(phase);

        try {
            PhaseInfo phaseInfo = PhaseInfo.builder()
                    .phaseId(phase.getId())
                    .worldGeneratorId(phase.getWorldGenerator().getId())
                    .processor(phase.getProcessor())
                    .name(phase.getName())
                    .description(phase.getDescription())
                    .phaseOrder(phase.getPhaseOrder())
                    .status(phase.getStatus())
                    .parameters(phase.getParameters())
                    .build();

            processor.processPhase(phaseInfo);

            phase.setStatus("COMPLETED");
            log.info("Phase '{}' completed successfully", phase.getName());

        } catch (Exception e) {
            log.error("Error processing phase '{}': {}", phase.getName(), e.getMessage(), e);
            phase.setStatus("ERROR");
        } finally {
            // Stelle sicher, dass die Phase immer gespeichert wird
            worldGeneratorPhaseRepository.save(phase);
        }
    }

    public List<WorldGenerator> getAllWorldGenerators() {
        return worldGeneratorRepository.findAllOrderByUpdatedAtDesc();
    }

    public Optional<WorldGenerator> getWorldGenerator(Long id) {
        return worldGeneratorRepository.findById(id);
    }

    public Optional<WorldGenerator> getWorldGeneratorByName(String name) {
        return worldGeneratorRepository.findByName(name);
    }

    public List<WorldGenerator> getWorldGeneratorsByStatus(String status) {
        return worldGeneratorRepository.findByStatus(status);
    }

    public List<WorldGeneratorPhase> getPhases(Long worldGeneratorId) {
        return worldGeneratorPhaseRepository.findByWorldGeneratorIdOrderByPhaseOrder(worldGeneratorId);
    }

    public List<WorldGeneratorPhase> getActivePhases(Long worldGeneratorId) {
        return worldGeneratorPhaseRepository.findActivePhasesByWorldGeneratorId(worldGeneratorId);
    }

    @Transactional
    public void deleteWorldGenerator(Long id) {
        log.info("Deleting world generator {}", id);
        worldGeneratorRepository.deleteById(id);
    }

    @Transactional
    public void archivePhase(Long phaseId) {
        log.info("Archiving phase {}", phaseId);
        WorldGeneratorPhase phase = worldGeneratorPhaseRepository.findById(phaseId)
                .orElseThrow(() -> new IllegalArgumentException("Phase not found: " + phaseId));

        phase.setArchived(true);
        worldGeneratorPhaseRepository.save(phase);
    }
}
