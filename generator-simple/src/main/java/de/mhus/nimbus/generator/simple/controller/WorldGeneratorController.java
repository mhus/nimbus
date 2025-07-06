package de.mhus.nimbus.generator.simple.controller;

import de.mhus.nimbus.generator.simple.dto.WorldGenerationRequest;
import de.mhus.nimbus.generator.simple.dto.WorldGenerationResponse;
import de.mhus.nimbus.generator.simple.service.WorldGeneratorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.CompletableFuture;

/**
 * REST Controller for world generation operations.
 * Provides endpoints for generating complete worlds with various configurations.
 */
@RestController
@RequestMapping("/api/v1/generator")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "World Generator", description = "Simple world generation API")
public class WorldGeneratorController {

    private final WorldGeneratorService worldGeneratorService;

    /**
     * Generate a complete world with the specified name and configuration
     */
    @PostMapping("/generate")
    @Operation(summary = "Generate a complete world",
               description = "Creates a new world with the specified name and generation parameters")
    public ResponseEntity<WorldGenerationResponse> generateWorld(@RequestBody WorldGenerationRequest request) {
        LOGGER.info("Received world generation request for: {}", request.getWorldName());

        if (request.getWorldName() == null || request.getWorldName().trim().isEmpty()) {
            LOGGER.warn("World generation request missing world name");
            return ResponseEntity.badRequest().build();
        }

        try {
            WorldGenerationResponse response = worldGeneratorService.generateWorld(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            LOGGER.error("Error generating world: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Generate a world asynchronously
     */
    @PostMapping("/generate/async")
    @Operation(summary = "Generate a world asynchronously",
               description = "Starts world generation in the background and returns immediately")
    public ResponseEntity<CompletableFuture<WorldGenerationResponse>> generateWorldAsync(
            @RequestBody WorldGenerationRequest request) {
        LOGGER.info("Received async world generation request for: {}", request.getWorldName());

        if (request.getWorldName() == null || request.getWorldName().trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        try {
            CompletableFuture<WorldGenerationResponse> future = worldGeneratorService.generateWorldAsync(request);
            return ResponseEntity.accepted().body(future);
        } catch (Exception e) {
            LOGGER.error("Error starting async world generation: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Generate a simple flat world with just the world name
     */
    @PostMapping("/generate/simple/{worldName}")
    @Operation(summary = "Generate a simple flat world",
               description = "Creates a simple flat world with the specified name using default settings")
    public ResponseEntity<WorldGenerationResponse> generateSimpleWorld(
            @Parameter(description = "Name of the world to generate")
            @PathVariable String worldName) {
        LOGGER.info("Received simple world generation request for: {}", worldName);

        WorldGenerationRequest request = WorldGenerationRequest.builder()
                .worldName(worldName)
                .worldType(WorldGenerationRequest.WorldType.FLAT)
                .build();

        try {
            WorldGenerationResponse response = worldGeneratorService.generateWorld(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            LOGGER.error("Error generating simple world: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Generate a world with specific type and size via URL parameters
     */
    @PostMapping("/generate/quick")
    @Operation(summary = "Quick world generation",
               description = "Generate a world with parameters passed as URL parameters")
    public ResponseEntity<WorldGenerationResponse> generateQuickWorld(
            @Parameter(description = "Name of the world")
            @RequestParam String worldName,
            @Parameter(description = "Type of world to generate")
            @RequestParam(defaultValue = "NORMAL") WorldGenerationRequest.WorldType worldType,
            @Parameter(description = "World width in chunks")
            @RequestParam(defaultValue = "16") int width,
            @Parameter(description = "World height in chunks")
            @RequestParam(defaultValue = "16") int height,
            @Parameter(description = "Random seed for generation")
            @RequestParam(required = false) Long seed) {

        LOGGER.info("Received quick world generation request for: {} (type: {}, size: {}x{})",
                   worldName, worldType, width, height);

        WorldGenerationRequest request = WorldGenerationRequest.builder()
                .worldName(worldName)
                .worldType(worldType)
                .worldSize(new WorldGenerationRequest.WorldSize(width, height))
                .seed(seed)
                .build();

        try {
            WorldGenerationResponse response = worldGeneratorService.generateWorld(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            LOGGER.error("Error generating quick world: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get available world types
     */
    @GetMapping("/types")
    @Operation(summary = "Get available world types",
               description = "Returns a list of all supported world generation types")
    public ResponseEntity<WorldGenerationRequest.WorldType[]> getWorldTypes() {
        return ResponseEntity.ok(WorldGenerationRequest.WorldType.values());
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    @Operation(summary = "Health check",
               description = "Returns the health status of the world generator service")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("World Generator Service is running");
    }
}
