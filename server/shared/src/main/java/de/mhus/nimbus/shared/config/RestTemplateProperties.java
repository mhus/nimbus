package de.mhus.nimbus.shared.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "shared.rest")
@Data
public class RestTemplateProperties {

    private long connectTimeout = 5000;
    private long readTimeout = 10000;

}

