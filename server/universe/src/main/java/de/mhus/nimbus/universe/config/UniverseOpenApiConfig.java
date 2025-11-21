package de.mhus.nimbus.universe.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI/Swagger Konfiguration für das Universe-Modul.
 *
 * - Registriert ein globales Bearer SecurityScheme (JWT)
 * - Fügt einen globalen SecurityRequirement hinzu, damit das "Authorize"-Popup in Swagger UI erscheint
 */
@Configuration
public class UniverseOpenApiConfig {

    public static final String SECURITY_SCHEME_NAME = "bearerAuth";

    @Bean
    public OpenAPI universeOpenAPI() {
        OpenAPI api = new OpenAPI()
                .info(new Info()
                        .title("Universe API")
                        .version("v1")
                        .description("API des Universe-Moduls"))
                // globaler SecurityRequirement, kann pro Operation überschrieben werden
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME));

        // Bearer SecurityScheme (JWT)
        api.getComponents()
                .addSecuritySchemes(SECURITY_SCHEME_NAME,
                        new SecurityScheme()
                                .name(SECURITY_SCHEME_NAME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT"));
        return api;
    }
}
