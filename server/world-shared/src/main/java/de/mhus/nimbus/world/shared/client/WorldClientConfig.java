package de.mhus.nimbus.world.shared.client;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * Configuration for inter-server REST communication.
 */
@Configuration
public class WorldClientConfig {

    @Bean
    @ConditionalOnMissingBean(name = "worldRestTemplate")
    public RestTemplate worldRestTemplate(WorldClientProperties properties, RestTemplateBuilder builder) {
        // Simple RestTemplate without authentication (internal K8s network)
        return builder
                .setConnectTimeout(Duration.ofMillis(properties.getCommandTimeoutMs()))
                .setReadTimeout(Duration.ofMillis(properties.getCommandTimeoutMs()))
                .build();
    }
}
