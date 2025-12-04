package de.mhus.nimbus.world.shared.client;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for inter-server command communication.
 */
@Data
@Component
@ConfigurationProperties(prefix = "world.client")
public class WorldClientProperties {

    /**
     * Base URL for world-player server.
     * Example: http://world-player:9042
     */
    private String playerBaseUrl = "http://localhost:9042";

    /**
     * Base URL for world-life server.
     * Example: http://world-life:9044
     */
    private String lifeBaseUrl = "http://localhost:9044";

    /**
     * Base URL for world-control server.
     * Example: http://world-control:9043
     */
    private String controlBaseUrl = "http://localhost:9043";

    /**
     * Command timeout in milliseconds.
     * Default: 5000ms (5 seconds)
     */
    private long commandTimeoutMs = 5000L;
}
