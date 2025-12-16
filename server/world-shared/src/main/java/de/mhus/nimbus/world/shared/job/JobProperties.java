package de.mhus.nimbus.world.shared.job;

import jakarta.validation.constraints.Positive;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration properties for Job System.
 * Loaded from application.yaml with prefix 'world.job'.
 */
@Component
@ConfigurationProperties(prefix = "world.job")
@Data
@Validated
public class JobProperties {

    /**
     * Enable/disable job processing scheduler.
     * Default: true
     */
    private boolean processingEnabled = true;

    /**
     * Job processing interval in milliseconds.
     * Default: 5000ms (5 seconds)
     */
    @Positive
    private long processingIntervalMs = 5000;

    /**
     * Maximum jobs to process per scheduler cycle.
     * Prevents overload if many jobs are pending.
     * Default: 10
     */
    @Positive
    private int maxJobsPerCycle = 10;

    /**
     * Enable/disable job cleanup scheduler.
     * Default: true
     */
    private boolean cleanupEnabled = true;

    /**
     * Job cleanup interval in milliseconds.
     * Default: 3600000ms (1 hour)
     */
    @Positive
    private long cleanupIntervalMs = 3600000;

    /**
     * Retention time for completed/failed jobs in hours.
     * Jobs older than this are deleted by cleanup scheduler.
     * Default: 24 hours
     */
    @Positive
    private long retentionHours = 24;

    /**
     * Use hard delete (remove from DB) vs soft delete (set enabled=false).
     * Default: false (soft delete)
     */
    private boolean hardDelete = false;
}
