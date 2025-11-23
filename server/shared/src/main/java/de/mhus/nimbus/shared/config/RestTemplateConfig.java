package de.mhus.nimbus.shared.config;

import de.mhus.nimbus.shared.security.JwtService;
import de.mhus.nimbus.shared.security.KeyService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
@RequiredArgsConstructor
public class RestTemplateConfig {

    private final RestTemplateProperties props;

    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
                .requestFactory(() -> {
                    var f = new org.springframework.http.client.SimpleClientHttpRequestFactory();
                    // Timeouts in ms
                    f.setConnectTimeout( props.getConnectTimeout());
                    f.setReadTimeout( props.getReadTimeout());
                    return f;
                })
                .build();
    }

}
