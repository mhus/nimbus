package de.mhus.nimbus.generator.simple.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;

/**
 * Response DTO for world generation.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorldGenerationResponse {

    /**
     * Generated world name
     */
    private String worldName;

    /**
     * Generation status
     */
    private GenerationStatus status;

    /**
     * Seed used for generation
     */
    private Long seed;

    /**
     * World type that was generated
     */
    private WorldGenerationRequest.WorldType worldType;

    /**
     * Actual world size generated
     */
    private WorldGenerationRequest.WorldSize worldSize;

    /**
     * Generation start time
     */
    private LocalDateTime generationStartTime;

    /**
     * Generation completion time
     */
    private LocalDateTime generationEndTime;

    /**
     * Duration in milliseconds
     */
    private Long durationMs;

    /**
     * Number of chunks generated
     */
    private int chunksGenerated;

    /**
     * Number of voxels generated
     */
    private long voxelsGenerated;

    /**
     * Generation statistics
     */
    @Builder.Default
    private GenerationStats stats = new GenerationStats();

    /**
     * Any warnings or messages during generation
     */
    @Builder.Default
    private List<String> messages = new ArrayList<>();

    /**
     * Error message if generation failed
     */
    private String errorMessage;

    /**
     * Generation status enum
     */
    public enum GenerationStatus {
        STARTED,
        IN_PROGRESS,
        COMPLETED,
        FAILED,
        CANCELLED
    }

    /**
     * Generation statistics
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class GenerationStats {
        @Builder.Default
        private long terrainVoxels = 0;
        @Builder.Default
        private long waterVoxels = 0;
        @Builder.Default
        private long airVoxels = 0;
        @Builder.Default
        private long structureVoxels = 0;
        @Builder.Default
        private int biomesGenerated = 0;
        @Builder.Default
        private int structuresGenerated = 0;
    }
}
