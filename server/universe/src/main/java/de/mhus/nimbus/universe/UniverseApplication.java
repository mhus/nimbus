package de.mhus.nimbus.universe;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import de.mhus.nimbus.universe.security.JwtProperties;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.License;

@SpringBootApplication
@EnableMongoAuditing
@ComponentScan(basePackages = {"de.mhus.nimbus.universe", "de.mhus.nimbus.shared"})
@EnableConfigurationProperties(JwtProperties.class)
@OpenAPIDefinition(info = @Info(title = "Universe API", version = "v1", description = "API for user, favorites and auth", contact = @Contact(name="Nimbus"), license = @License(name="Apache-2.0")))
public class UniverseApplication {

    public static void main(String[] args) {
        SpringApplication.run(UniverseApplication.class, args);
    }

}
