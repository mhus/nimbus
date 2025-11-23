package de.mhus.nimbus.shared.config;

import de.mhus.nimbus.shared.SharedProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(SharedProperties.class)
public class RestTemplateConfig {

    private final SharedProperties props;

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
                .requestFactory(() -> {
                    var f = new org.springframework.http.client.SimpleClientHttpRequestFactory();
                    f.setConnectTimeout(props.getRestConnectTimeout());
                    f.setReadTimeout(props.getRestReadTimeout());
                    return f;
                })
                .build();
    }

}
