package de.mhus.nimbus.world.shared.config;

import de.mhus.nimbus.world.shared.auth.WorldAuthorizationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

/**
 * Configuration for world authentication.
 */
@Configuration
@RequiredArgsConstructor
public class WorldAuthConfig {

    private final WorldAuthorizationFilter worldAuthorizationFilter;

    /**
     * Registers the world authorization filter.
     *
     * @return the filter registration bean
     */
    @Bean
    public FilterRegistrationBean<WorldAuthorizationFilter> worldAuthFilterRegistration() {
        FilterRegistrationBean<WorldAuthorizationFilter> registration =
            new FilterRegistrationBean<>(worldAuthorizationFilter);

        registration.addUrlPatterns("/api/*");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 1);
        registration.setName("worldAuthorizationFilter");

        return registration;
    }
}
