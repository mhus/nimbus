package de.mhus.nimbus.shared.config;

import de.mhus.nimbus.shared.util.IdentityServiceUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class for shared components.
 * Provides beans for JWT token validation and other shared utilities.
 */
@Configuration
public class SharedConfig {

    /**
     * Provides IdentityServiceUtils bean for JWT token validation.
     * @return IdentityServiceUtils instance
     */
    @Bean
    public IdentityServiceUtils identityServiceUtils() {
        return new IdentityServiceUtils();
    }
}
