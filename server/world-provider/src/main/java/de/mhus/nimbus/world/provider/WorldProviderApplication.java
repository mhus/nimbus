package de.mhus.nimbus.world.provider;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class WorldProviderApplication {
    public static void main(String[] args) {
        SpringApplication.run(WorldProviderApplication.class, args);
    }
}

