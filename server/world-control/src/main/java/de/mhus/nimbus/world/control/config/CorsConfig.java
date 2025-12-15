package de.mhus.nimbus.world.control.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.List;

/**
 * CORS configuration for world-control REST endpoints.
 * Allows requests from localhost frontend during development.
 */
@Configuration
public class CorsConfig {

    @Value("${world.cors.allowed-origins:*}")
    private List<String> allowedOrigins;

    @Bean
    public CorsFilter corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();

        // Allow credentials
        config.setAllowCredentials(true);

        // Allow origins
        allowedOrigins.forEach(config::addAllowedOriginPattern);

        // Allow all headers
        config.addAllowedHeader("*");

        // Allow all HTTP methods
        config.addAllowedMethod("*");

        // Apply to all endpoints
        source.registerCorsConfiguration("/**", config);

        return new CorsFilter(source);
    }
}
