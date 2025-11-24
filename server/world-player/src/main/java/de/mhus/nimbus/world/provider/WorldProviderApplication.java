package de.mhus.nimbus.world.provider;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@ConfigurationPropertiesScan
@EnableScheduling
@ComponentScan(basePackages = {"de.mhus.nimbus.world.provider","de.mhus.nimbus.world.shared","de.mhus.nimbus.shared"})
public class WorldProviderApplication {
    public static void main(String[] args) {
        SpringApplication.run(WorldProviderApplication.class, args);
    }
}
