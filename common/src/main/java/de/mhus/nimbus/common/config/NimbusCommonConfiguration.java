package de.mhus.nimbus.common.config;

import de.mhus.nimbus.common.properties.NimbusProperties;
import de.mhus.nimbus.common.util.RequestIdUtils;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Clock;

/**
 * Bean Configuration für Nimbus Common Module
 * Folgt Spring Boot Naming Conventions
 */
@Configuration
@EnableConfigurationProperties(NimbusProperties.class)
public class NimbusCommonConfiguration {

    /**
     * RestTemplate Bean für HTTP-Aufrufe zwischen Services
     */
    @Bean
    public RestTemplate nimbusRestTemplate() {
        return new RestTemplate();
    }

    /**
     * Clock Bean für konsistente Zeitverarbeitung
     */
    @Bean
    public Clock nimbusClock() {
        return Clock.systemUTC();
    }

    /**
     * Request ID Utils Bean (wird bereits als @Component definiert, aber hier für Klarheit)
     */
    @Bean
    public RequestIdUtils nimbusRequestIdUtils() {
        return new RequestIdUtils();
    }
}
