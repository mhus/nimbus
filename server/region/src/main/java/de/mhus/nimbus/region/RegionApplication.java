package de.mhus.nimbus.region;

import de.mhus.nimbus.shared.SharedProperties;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ReflectiveScan;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@SpringBootApplication
@EnableMongoAuditing
@ReflectiveScan(basePackages = {"de.mhus.nimbus.region", "de.mhus.nimbus.shared"})
@ComponentScan(basePackages = {"de.mhus.nimbus.region", "de.mhus.nimbus.shared"})
@EnableMongoRepositories(basePackages = {"de.mhus.nimbus.region", "de.mhus.nimbus.shared"})
@EnableConfigurationProperties({RegionProperties.class, SharedProperties.class})
@OpenAPIDefinition(info = @Info(title = "Universe API", version = "v1", description = "API for user, favorites and auth", contact = @Contact(name="Nimbus"), license = @License(name="Apache-2.0")))
public class RegionApplication {

    public static void main(String[] args) {
        SpringApplication.run(RegionApplication.class, args);
    }

}
