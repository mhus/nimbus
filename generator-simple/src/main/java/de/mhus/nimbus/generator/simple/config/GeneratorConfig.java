package de.mhus.nimbus.generator.simple.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import lombok.Data;

/**
 * Configuration properties for the world generator service.
 */
@Configuration
@ConfigurationProperties(prefix = "world.generator")
@Data
public class GeneratorConfig {

    /**
     * Default chunk size (16x16 voxels)
     */
    private int defaultChunkSize = 16;

    /**
     * Default world height in voxels
     */
    private int defaultWorldHeight = 64;

    /**
     * Maximum world size in chunks
     */
    private int maxWorldSize = 100;

    /**
     * Enable asynchronous world generation
     */
    private boolean enableAsync = true;

    /**
     * Thread pool size for async generation
     */
    private int asyncThreadPoolSize = 4;

    /**
     * Maximum generation time in milliseconds before timeout
     */
    private long maxGenerationTimeMs = 300000; // 5 minutes
}
