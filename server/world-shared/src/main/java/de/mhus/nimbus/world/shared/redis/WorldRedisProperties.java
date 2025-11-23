package de.mhus.nimbus.world.shared.redis;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "world.redis")
public class WorldRedisProperties {
    private String host = "localhost";
    private int port = 6379;
    private int database = 0;
    private String password; // optional
    private boolean ssl = false;
}
