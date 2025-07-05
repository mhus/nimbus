package de.mhus.nimbus.common.config;

import de.mhus.nimbus.common.properties.NimbusProperties;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;

/**
 * Auto-Configuration für Nimbus Common Module
 * Folgt Spring Boot Naming Conventions
 */
@AutoConfiguration
@EnableConfigurationProperties(NimbusProperties.class)
@ComponentScan(basePackages = "de.mhus.nimbus.common")
public class NimbusCommonAutoConfiguration {

    // Auto-Configuration wird automatisch durch spring.factories aktiviert
}
