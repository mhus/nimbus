package de.mhus.nimbus.world.control.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Git credentials configuration from application.yaml.
 * Used as fallback when ExternalResourceDTO doesn't provide credentials.
 */
@Configuration
@ConfigurationProperties(prefix = "nimbus.git")
@Data
public class GitCredentialsProperties {

    /**
     * Default Git username for authentication.
     */
    private String username;

    /**
     * Default Git password/token for authentication.
     */
    private String password;

    /**
     * Default Git branch (e.g., "main" or "master").
     */
    private String branch = "main";
}
