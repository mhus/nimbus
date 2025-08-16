package de.mhus.nimbus.worldgenerator.controller;

import de.mhus.nimbus.world.dto.AddPhaseRequest;
import de.mhus.nimbus.world.dto.CreateWorldGeneratorRequest;
import de.mhus.nimbus.worldgenerator.entity.WorldGenerator;
import de.mhus.nimbus.worldgenerator.entity.WorldGeneratorPhase;
import de.mhus.nimbus.worldgenerator.service.GeneratorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/generator")
@RequiredArgsConstructor
@Slf4j
public class GeneratorController {

    private final GeneratorService generatorService;

    @PostMapping("/create")
    public ResponseEntity<WorldGenerator> createWorldGenerator(@RequestBody CreateWorldGeneratorRequest request) {
        try {
            WorldGenerator worldGenerator = generatorService.createWorldGenerator(
                    request.getName(),
                    request.getDescription(),
                    request.getParameters()
            );
            return ResponseEntity.ok(worldGenerator);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/{id}/phases")
    public ResponseEntity<WorldGeneratorPhase> addPhase(@PathVariable Long id, @RequestBody AddPhaseRequest request) {
        try {
            WorldGeneratorPhase phase = generatorService.addPhase(
                    id,
                    request.getProcessor(),
                    request.getName(),
                    request.getDescription(),
                    request.getPhaseOrder(),
                    request.getParameters()
            );
            return ResponseEntity.ok(phase);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/{id}/start")
    public ResponseEntity<Void> startGeneration(@PathVariable Long id) {
        try {
            generatorService.startGeneration(id);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping
    public ResponseEntity<List<WorldGenerator>> getAllWorldGenerators() {
        List<WorldGenerator> generators = generatorService.getAllWorldGenerators();
        return ResponseEntity.ok(generators);
    }

    @GetMapping("/{id}")
    public ResponseEntity<WorldGenerator> getWorldGenerator(@PathVariable Long id) {
        Optional<WorldGenerator> generator = generatorService.getWorldGenerator(id);
        return generator.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/by-name/{name}")
    public ResponseEntity<WorldGenerator> getWorldGeneratorByName(@PathVariable String name) {
        Optional<WorldGenerator> generator = generatorService.getWorldGeneratorByName(name);
        return generator.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<WorldGenerator>> getWorldGeneratorsByStatus(@PathVariable String status) {
        List<WorldGenerator> generators = generatorService.getWorldGeneratorsByStatus(status);
        return ResponseEntity.ok(generators);
    }

    @GetMapping("/{id}/phases")
    public ResponseEntity<List<WorldGeneratorPhase>> getPhases(@PathVariable Long id) {
        List<WorldGeneratorPhase> phases = generatorService.getPhases(id);
        return ResponseEntity.ok(phases);
    }

    @GetMapping("/{id}/phases/active")
    public ResponseEntity<List<WorldGeneratorPhase>> getActivePhases(@PathVariable Long id) {
        List<WorldGeneratorPhase> phases = generatorService.getActivePhases(id);
        return ResponseEntity.ok(phases);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteWorldGenerator(@PathVariable Long id) {
        try {
            generatorService.deleteWorldGenerator(id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/phases/{phaseId}/archive")
    public ResponseEntity<Void> archivePhase(@PathVariable Long phaseId) {
        try {
            generatorService.archivePhase(phaseId);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
