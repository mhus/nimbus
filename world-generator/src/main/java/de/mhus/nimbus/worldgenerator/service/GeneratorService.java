package de.mhus.nimbus.worldgenerator.service;

import de.mhus.nimbus.worldgenerator.entity.WorldGenerator;
import de.mhus.nimbus.worldgenerator.entity.WorldGeneratorPhase;
import de.mhus.nimbus.worldgenerator.repository.WorldGeneratorRepository;
import de.mhus.nimbus.worldgenerator.repository.WorldGeneratorPhaseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
@Slf4j
public class GeneratorService {

    private final WorldGeneratorRepository worldGeneratorRepository;
    private final WorldGeneratorPhaseRepository phaseRepository;

    @Transactional
    public WorldGenerator createWorldGenerator(String worldId, String name, String description, Map<String, String> parameters) {
        log.info("Creating new world generator: {} ({})", name, worldId);

        if (worldGeneratorRepository.findByWorldId(worldId).isPresent()) {
            throw new IllegalArgumentException("World generator with worldId '" + worldId + "' already exists");
        }

        WorldGenerator worldGenerator = WorldGenerator.builder()
                .worldId(worldId)
                .name(name)
                .description(description)
                .status(WorldGenerator.GenerationStatus.PENDING)
                .parameters(parameters)
                .totalPhases(8)
                .completedPhases(0)
                .build();

        WorldGenerator savedGenerator = worldGeneratorRepository.save(worldGenerator);

        // Erstelle alle Standard-Phasen
        List<WorldGeneratorPhase> phases = createDefaultPhases(savedGenerator);
        phaseRepository.saveAll(phases);

        log.info("Created world generator {} with {} phases", savedGenerator.getId(), phases.size());
        return savedGenerator;
    }

    private List<WorldGeneratorPhase> createDefaultPhases(WorldGenerator worldGenerator) {
        String[] phaseTypes = WorldGeneratorPhase.getDefaultPhaseTypes();

        return IntStream.range(0, phaseTypes.length)
                .mapToObj(i -> WorldGeneratorPhase.builder()
                        .worldGenerator(worldGenerator)
                        .phaseType(phaseTypes[i])
                        .phaseOrder(i + 1)
                        .status(WorldGeneratorPhase.PhaseStatus.PENDING)
                        .parameters(new HashMap<>())
                        .progressPercentage(0)
                        .build())
                .toList();
    }

    public List<WorldGenerator> getAllWorldGenerators() {
        return worldGeneratorRepository.findAll();
    }

    public Optional<WorldGenerator> getWorldGeneratorById(Long id) {
        return worldGeneratorRepository.findById(id);
    }

    public Optional<WorldGenerator> getWorldGeneratorByWorldId(String worldId) {
        return worldGeneratorRepository.findByWorldId(worldId);
    }

    public List<WorldGenerator> getWorldGeneratorsByStatus(WorldGenerator.GenerationStatus status) {
        return worldGeneratorRepository.findByStatus(status);
    }

    @Transactional
    public boolean startGeneration(Long worldGeneratorId) {
        log.info("Starting generation for world generator {}", worldGeneratorId);

        Optional<WorldGenerator> optionalGenerator = worldGeneratorRepository.findById(worldGeneratorId);
        if (optionalGenerator.isEmpty()) {
            return false;
        }

        WorldGenerator generator = optionalGenerator.get();
        generator.setStatus(WorldGenerator.GenerationStatus.RUNNING);
        generator.setStartedAt(LocalDateTime.now());
        worldGeneratorRepository.save(generator);

        // Starte erste Phase
        List<WorldGeneratorPhase> phases = getPhasesByWorldGenerator(worldGeneratorId);
        if (!phases.isEmpty()) {
            WorldGeneratorPhase firstPhase = phases.get(0);
            firstPhase.setStatus(WorldGeneratorPhase.PhaseStatus.RUNNING);
            firstPhase.setStartedAt(LocalDateTime.now());
            phaseRepository.save(firstPhase);

            generator.setCurrentPhase(firstPhase.getPhaseType());
            worldGeneratorRepository.save(generator);

            log.info("Started phase {} for world generator {}", firstPhase.getPhaseType(), worldGeneratorId);
        }

        return true;
    }

    @Transactional
    public boolean completePhase(Long phaseId, String resultSummary) {
        log.info("Completing phase {}", phaseId);

        Optional<WorldGeneratorPhase> optionalPhase = phaseRepository.findById(phaseId);
        if (optionalPhase.isEmpty()) {
            return false;
        }

        WorldGeneratorPhase phase = optionalPhase.get();
        phase.setStatus(WorldGeneratorPhase.PhaseStatus.COMPLETED);
        phase.setCompletedAt(LocalDateTime.now());
        phase.setProgressPercentage(100);
        phase.setResultSummary(resultSummary);
        phaseRepository.save(phase);

        // Update World Generator
        WorldGenerator generator = phase.getWorldGenerator();
        generator.setCompletedPhases(generator.getCompletedPhases() + 1);

        // Starte nächste Phase oder beende Generierung
        List<WorldGeneratorPhase> allPhases = getPhasesByWorldGenerator(generator.getId());
        Optional<WorldGeneratorPhase> nextPhase = allPhases.stream()
                .filter(p -> p.getStatus() == WorldGeneratorPhase.PhaseStatus.PENDING)
                .min(Comparator.comparing(WorldGeneratorPhase::getPhaseOrder));

        if (nextPhase.isPresent()) {
            WorldGeneratorPhase next = nextPhase.get();
            next.setStatus(WorldGeneratorPhase.PhaseStatus.RUNNING);
            next.setStartedAt(LocalDateTime.now());
            phaseRepository.save(next);

            generator.setCurrentPhase(next.getPhaseType());
            log.info("Started next phase {} for world generator {}", next.getPhaseType(), generator.getId());
        } else {
            // Alle Phasen abgeschlossen
            generator.setStatus(WorldGenerator.GenerationStatus.COMPLETED);
            generator.setCompletedAt(LocalDateTime.now());
            generator.setCurrentPhase(null);
            log.info("Completed all phases for world generator {}", generator.getId());
        }

        worldGeneratorRepository.save(generator);
        return true;
    }

    @Transactional
    public boolean completeGeneration(Long worldGeneratorId) {
        log.info("Completing generation for world generator {}", worldGeneratorId);

        Optional<WorldGenerator> optionalGenerator = worldGeneratorRepository.findById(worldGeneratorId);
        if (optionalGenerator.isEmpty()) {
            return false;
        }

        WorldGenerator generator = optionalGenerator.get();
        generator.setStatus(WorldGenerator.GenerationStatus.COMPLETED);
        generator.setCompletedAt(LocalDateTime.now());
        generator.setCompletedPhases(generator.getTotalPhases());
        worldGeneratorRepository.save(generator);

        return true;
    }

    @Transactional
    public boolean failPhase(Long phaseId, String errorMessage) {
        log.error("Failing phase {}: {}", phaseId, errorMessage);

        Optional<WorldGeneratorPhase> optionalPhase = phaseRepository.findById(phaseId);
        if (optionalPhase.isEmpty()) {
            return false;
        }

        WorldGeneratorPhase phase = optionalPhase.get();
        phase.setStatus(WorldGeneratorPhase.PhaseStatus.FAILED);
        phase.setErrorMessage(errorMessage);
        phase.setCompletedAt(LocalDateTime.now());
        phaseRepository.save(phase);

        // Markiere World Generator als fehlgeschlagen
        WorldGenerator generator = phase.getWorldGenerator();
        generator.setStatus(WorldGenerator.GenerationStatus.FAILED);
        generator.setErrorMessage(errorMessage);
        worldGeneratorRepository.save(generator);

        return true;
    }

    public List<WorldGeneratorPhase> getPhasesByWorldGenerator(Long worldGeneratorId) {
        return phaseRepository.findByWorldGeneratorIdOrderByPhaseOrder(worldGeneratorId);
    }

    @Transactional
    public boolean updatePhaseProgress(Long phaseId, Integer progressPercentage) {
        Optional<WorldGeneratorPhase> optionalPhase = phaseRepository.findById(phaseId);
        if (optionalPhase.isEmpty()) {
            return false;
        }

        WorldGeneratorPhase phase = optionalPhase.get();
        phase.setProgressPercentage(progressPercentage);
        phaseRepository.save(phase);

        log.debug("Updated progress for phase {} to {}%", phaseId, progressPercentage);
        return true;
    }

    @Transactional
    public WorldGeneratorPhase addPhaseToWorldGenerator(Long worldGeneratorId,
                                                       String phaseType,
                                                       Integer phaseOrder,
                                                       Map<String, String> parameters) {
        log.info("Adding phase {} to world generator {}", phaseType, worldGeneratorId);

        WorldGenerator generator = worldGeneratorRepository.findById(worldGeneratorId)
                .orElseThrow(() -> new IllegalArgumentException("World generator not found: " + worldGeneratorId));

        WorldGeneratorPhase phase = WorldGeneratorPhase.builder()
                .worldGenerator(generator)
                .phaseType(phaseType)
                .phaseOrder(phaseOrder)
                .status(WorldGeneratorPhase.PhaseStatus.PENDING)
                .parameters(parameters)
                .progressPercentage(0)
                .build();

        return phaseRepository.save(phase);
    }

    @Transactional
    public boolean deleteWorldGenerator(Long worldGeneratorId) {
        log.info("Deleting world generator {}", worldGeneratorId);

        Optional<WorldGenerator> optionalGenerator = worldGeneratorRepository.findById(worldGeneratorId);
        if (optionalGenerator.isEmpty()) {
            return false;
        }

        WorldGenerator generator = optionalGenerator.get();
        worldGeneratorRepository.delete(generator);
        return true;
    }

    @Transactional
    public boolean archivePhase(Long phaseId) {
        log.info("Archiving phase {}", phaseId);

        Optional<WorldGeneratorPhase> optionalPhase = phaseRepository.findById(phaseId);
        if (optionalPhase.isEmpty()) {
            return false;
        }

        WorldGeneratorPhase phase = optionalPhase.get();
        // Für die Archivierung können wir ein spezielles Flag setzen oder den Status ändern
        // Da wir kein archived-Flag in der Entity haben, setzen wir den Status auf SKIPPED
        if (phase.getStatus() == WorldGeneratorPhase.PhaseStatus.PENDING) {
            phase.setStatus(WorldGeneratorPhase.PhaseStatus.SKIPPED);
            phaseRepository.save(phase);
            log.info("Phase {} archived (status set to SKIPPED)", phaseId);
            return true;
        } else {
            log.warn("Cannot archive phase {} with status {}", phaseId, phase.getStatus());
            return false;
        }
    }
}
