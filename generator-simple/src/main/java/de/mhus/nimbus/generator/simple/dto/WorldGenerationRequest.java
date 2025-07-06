package de.mhus.nimbus.generator.simple.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.util.Map;
import java.util.HashMap;

/**
 * Request DTO for world generation.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorldGenerationRequest {

    /**
     * Name of the world to generate
     */
    private String worldName;

    /**
     * Seed for random generation (optional)
     */
    private Long seed;

    /**
     * World type (e.g., FLAT, NORMAL, AMPLIFIED)
     */
    @Builder.Default
    private WorldType worldType = WorldType.NORMAL;

    /**
     * World size in chunks (default 16x16)
     */
    @Builder.Default
    private WorldSize worldSize = new WorldSize(16, 16);

    /**
     * Generator configuration parameters
     */
    @Builder.Default
    private Map<String, Object> generatorConfig = new HashMap<>();

    /**
     * World types supported by the simple generator
     */
    public enum WorldType {
        FLAT,
        NORMAL,
        AMPLIFIED,
        DESERT,
        FOREST,
        MOUNTAINS,
        OCEAN
    }

    /**
     * World size specification
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WorldSize {
        private int width;  // in chunks
        private int height; // in chunks
    }
}
