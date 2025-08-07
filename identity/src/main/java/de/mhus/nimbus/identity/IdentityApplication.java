package de.mhus.nimbus.identity;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main application class for the Identity Service.
 * Configures and starts the Spring Boot application.
 */
@SpringBootApplication(scanBasePackages = {
    "de.mhus.nimbus.identity",
    "de.mhus.nimbus.server.shared",
    "de.mhus.nimbus.shared"
})
public class IdentityApplication {

    public static void main(String[] args) {
        SpringApplication.run(IdentityApplication.class, args);
    }
}
