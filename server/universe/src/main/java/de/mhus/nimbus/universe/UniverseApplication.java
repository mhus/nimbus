package de.mhus.nimbus.universe;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import de.mhus.nimbus.universe.security.JwtProperties;

@SpringBootApplication
@EnableMongoAuditing
@ComponentScan(basePackages = {"de.mhus.nimbus.universe", "de.mhus.nimbus.shared"})
@EnableConfigurationProperties(JwtProperties.class)
public class UniverseApplication {

    public static void main(String[] args) {
        SpringApplication.run(UniverseApplication.class, args);
    }

}
