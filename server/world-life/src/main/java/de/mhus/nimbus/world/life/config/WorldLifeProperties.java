package de.mhus.nimbus.world.life.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

/**
 * Configuration properties for World Life service.
 */
@ConfigurationProperties(prefix = "world.life")
@Data
@Validated
public class WorldLifeProperties {


    /**
     * Simulation loop interval in milliseconds.
     * Default: 1000ms (1 second)
     */
    @Positive
    private long simulationIntervalMs = 1000;

    /**
     * Chunk refresh interval in milliseconds.
     * Requests full chunk list from world-player pods.
     * Default: 300000ms (5 minutes)
     * @deprecated Replaced by TTL-based mechanism. world-player now pushes chunks automatically.
     */
    @Deprecated
    @Positive
    private long chunkRefreshIntervalMs = 300000;

    /**
     * Chunk TTL (Time-To-Live) in milliseconds.
     * Chunks without update for this duration are removed.
     * Default: 300000ms (5 minutes)
     */
    @Positive
    private long chunkTtlMs = 300000;

    /**
     * Chunk TTL cleanup task interval in milliseconds.
     * How often to check for and remove stale chunks.
     * Default: 60000ms (1 minute)
     */
    @Positive
    private long chunkTtlCleanupIntervalMs = 60000;

    /**
     * Entity ownership heartbeat interval in milliseconds.
     * Default: 5000ms (5 seconds)
     */
    @Positive
    private long ownershipHeartbeatIntervalMs = 5000;

    /**
     * Entity ownership stale threshold in milliseconds.
     * Entities with no heartbeat for this duration are considered orphaned.
     * Default: 10000ms (10 seconds)
     */
    @Positive
    private long ownershipStaleThresholdMs = 10000;

    /**
     * Orphan detection interval in milliseconds.
     * Default: 30000ms (30 seconds)
     */
    @Positive
    private long orphanDetectionIntervalMs = 30000;

    /**
     * Pathway interval for PreyAnimalBehavior in milliseconds.
     * Default: 5000ms (5 seconds)
     */
    @Positive
    private long pathwayIntervalMs = 5000;
}
