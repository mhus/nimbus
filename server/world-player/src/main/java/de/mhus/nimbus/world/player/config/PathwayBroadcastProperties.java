package de.mhus.nimbus.world.player.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "world.player")
@Data
public class PathwayBroadcastProperties {

    /**
     * Interval in milliseconds for broadcasting entity pathways to Redis.
     * Default: 100ms (10 times per second)
     */
    private int pathwayBroadcastIntervalMs = 100;

    /**
     * Timeout in milliseconds after which an entity is considered inactive.
     * Inactive entities will not generate pathways.
     * Default: 200ms
     */
    private int entityUpdateTimeoutMs = 200;

    /**
     * Time in milliseconds to predict future position based on velocity.
     * Default: 100ms
     */
    private int pathwayPredictionTimeMs = 100;
}
