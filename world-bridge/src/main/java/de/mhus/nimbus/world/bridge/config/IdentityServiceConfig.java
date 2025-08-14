package de.mhus.nimbus.world.bridge.config;

import de.mhus.nimbus.shared.util.IdentityServiceUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class IdentityServiceConfig {

    @Bean
    public IdentityServiceUtils identityServiceUtils() {
        return new IdentityServiceUtils();
    }
}
