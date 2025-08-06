package de.mhus.nimbus.server.shared.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * Configuration class for server-shared components.
 */
@Configuration
public class ServerSharedConfig {

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
