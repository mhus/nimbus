package de.mhus.nimbus.universe.security;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class USecurityConfig {

    @Bean
    public FilterRegistrationBean<UUserJwtAuthenticationFilter> jwtFilterRegistration(UUserJwtAuthenticationFilter filter) {
        FilterRegistrationBean<UUserJwtAuthenticationFilter> reg = new FilterRegistrationBean<>();
        reg.setFilter(filter);
        reg.addUrlPatterns("/universe/user/*");
        reg.setOrder(10); // after basic internal filters, before controllers
        reg.setName("userJwtAuthenticationFilter");
        return reg;
    }
}
