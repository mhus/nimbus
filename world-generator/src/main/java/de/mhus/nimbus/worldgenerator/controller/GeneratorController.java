package de.mhus.nimbus.worldgenerator.controller;

import de.mhus.nimbus.worldgenerator.entity.WorldGenerator;
import de.mhus.nimbus.worldgenerator.entity.WorldGeneratorPhase;
import de.mhus.nimbus.worldgenerator.service.GeneratorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/generator")
@RequiredArgsConstructor
@Slf4j
public class GeneratorController {

    private final GeneratorService generatorService;

    @PostMapping("/worlds")
    public ResponseEntity<WorldGenerator> createWorldGenerator(@RequestBody Map<String, Object> request) {
        try {
            String worldId = (String) request.get("worldId");
            String name = (String) request.get("name");
            String description = (String) request.get("description");
            @SuppressWarnings("unchecked")
            Map<String, String> parameters = (Map<String, String>) request.get("parameters");

            WorldGenerator generator = generatorService.createWorldGenerator(worldId, name, description, parameters);
            return ResponseEntity.status(HttpStatus.CREATED).body(generator);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Error creating world generator", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/worlds")
    public ResponseEntity<List<WorldGenerator>> getAllWorldGenerators(
            @RequestParam(required = false) String status) {
        try {
            List<WorldGenerator> generators;
            if (status != null) {
                WorldGenerator.GenerationStatus generationStatus = WorldGenerator.GenerationStatus.valueOf(status);
                generators = generatorService.getWorldGeneratorsByStatus(generationStatus);
            } else {
                generators = generatorService.getAllWorldGenerators();
            }
            return ResponseEntity.ok(generators);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Error retrieving world generators", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/worlds/{id}")
    public ResponseEntity<WorldGenerator> getWorldGeneratorById(@PathVariable Long id) {
        Optional<WorldGenerator> generator = generatorService.getWorldGeneratorById(id);
        return generator.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/worlds/{id}/status")
    public ResponseEntity<Map<String, Object>> getWorldGeneratorStatus(@PathVariable Long id) {
        Optional<WorldGenerator> optionalGenerator = generatorService.getWorldGeneratorById(id);
        if (optionalGenerator.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        WorldGenerator generator = optionalGenerator.get();
        Map<String, Object> status = Map.of(
                "status", generator.getStatus(),
                "progressPercentage", generator.getProgressPercentage(),
                "completedPhases", generator.getCompletedPhases(),
                "totalPhases", generator.getTotalPhases(),
                "currentPhase", generator.getCurrentPhase() != null ? generator.getCurrentPhase() : ""
        );

        return ResponseEntity.ok(status);
    }

    @PostMapping("/worlds/{id}/start")
    public ResponseEntity<Map<String, String>> startGeneration(@PathVariable Long id) {
        try {
            boolean started = generatorService.startGeneration(id);
            if (started) {
                return ResponseEntity.ok(Map.of("message", "Generation started successfully"));
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "World generator not found"));
            }
        } catch (Exception e) {
            log.error("Error starting generation for world generator {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Internal server error"));
        }
    }

    @GetMapping("/worlds/{id}/phases")
    public ResponseEntity<List<WorldGeneratorPhase>> getPhasesByWorldGenerator(@PathVariable Long id) {
        try {
            List<WorldGeneratorPhase> phases = generatorService.getPhasesByWorldGenerator(id);
            return ResponseEntity.ok(phases);
        } catch (Exception e) {
            log.error("Error retrieving phases for world generator {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/worlds/{id}/phases")
    public ResponseEntity<WorldGeneratorPhase> addPhaseToWorldGenerator(
            @PathVariable Long id, @RequestBody Map<String, Object> request) {
        try {
            String phaseType = (String) request.get("phaseType");
            Integer phaseOrder = (Integer) request.get("phaseOrder");
            @SuppressWarnings("unchecked")
            Map<String, String> parameters = (Map<String, String>) request.get("parameters");

            WorldGeneratorPhase phase = generatorService.addPhaseToWorldGenerator(id, phaseType, phaseOrder, parameters);
            return ResponseEntity.status(HttpStatus.CREATED).body(phase);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Error adding phase to world generator {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PutMapping("/phases/{phaseId}/progress")
    public ResponseEntity<Map<String, String>> updatePhaseProgress(
            @PathVariable Long phaseId, @RequestBody Map<String, Object> request) {
        try {
            Integer progressPercentage = (Integer) request.get("progressPercentage");
            boolean updated = generatorService.updatePhaseProgress(phaseId, progressPercentage);
            if (updated) {
                return ResponseEntity.ok(Map.of("message", "Phase progress updated successfully"));
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            log.error("Error updating phase progress for phase {}", phaseId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/phases/{phaseId}/complete")
    public ResponseEntity<Map<String, String>> completePhase(
            @PathVariable Long phaseId, @RequestBody Map<String, Object> request) {
        try {
            String resultSummary = (String) request.get("resultSummary");
            boolean completed = generatorService.completePhase(phaseId, resultSummary);
            if (completed) {
                return ResponseEntity.ok(Map.of("message", "Phase completed successfully"));
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            log.error("Error completing phase {}", phaseId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/phases/{phaseId}/fail")
    public ResponseEntity<Map<String, String>> failPhase(
            @PathVariable Long phaseId, @RequestBody Map<String, Object> request) {
        try {
            String errorMessage = (String) request.get("errorMessage");
            boolean failed = generatorService.failPhase(phaseId, errorMessage);
            if (failed) {
                return ResponseEntity.ok(Map.of("message", "Phase failed"));
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            log.error("Error failing phase {}", phaseId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/worlds/{id}")
    public ResponseEntity<Map<String, String>> deleteWorldGenerator(@PathVariable Long id) {
        try {
            boolean deleted = generatorService.deleteWorldGenerator(id);
            if (deleted) {
                return ResponseEntity.ok(Map.of("message", "World generator deleted successfully"));
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "World generator not found"));
            }
        } catch (Exception e) {
            log.error("Error deleting world generator {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/phases/{phaseId}/archive")
    public ResponseEntity<Map<String, String>> archivePhase(@PathVariable Long phaseId) {
        try {
            boolean archived = generatorService.archivePhase(phaseId);
            if (archived) {
                return ResponseEntity.ok(Map.of("message", "Phase archived successfully"));
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Phase not found"));
            }
        } catch (Exception e) {
            log.error("Error archiving phase {}", phaseId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
