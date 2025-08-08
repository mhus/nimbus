package de.mhus.nimbus.registry;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main application class for the Registry Service.
 * Configures and starts the Spring Boot application.
 */
@SpringBootApplication(scanBasePackages = {
    "de.mhus.nimbus.registry",
    "de.mhus.nimbus.server.shared",
    "de.mhus.nimbus.shared"
})
public class RegistryApplication {

    public static void main(String[] args) {
        SpringApplication.run(RegistryApplication.class, args);
    }
}
