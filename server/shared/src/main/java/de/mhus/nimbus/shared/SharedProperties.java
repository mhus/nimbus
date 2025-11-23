package de.mhus.nimbus.shared;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "shared")
@Data
public class SharedProperties {

    private int restConnectTimeout = 5000;
    private int restReadTimeout = 10000;

}

