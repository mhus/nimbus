package de.mhus.nimbus.generator.simple.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI configuration for the Simple World Generator service.
 */
@Configuration
public class OpenApiConfig {

    @Value("${server.port:7083}")
    private int serverPort;

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .servers(List.of(
                        new Server()
                                .url("http://localhost:" + serverPort)
                                .description("Development Server")
                ))
                .info(new Info()
                        .title("Nimbus Simple World Generator API")
                        .description("API für die Generierung von Welten im Nimbus System. " +
                                   "Dieser Service ermöglicht die Erstellung verschiedener Welttypen " +
                                   "mit konfigurierbaren Parametern.")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Nimbus Development Team")
                                .email("dev@mhus.de"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")));
    }
}
