package de.mhus.nimbus.generator.simple;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main application class for the Nimbus Simple World Generator service.
 * Provides REST API endpoints for generating complete worlds.
 * Includes component scanning for common module beans.
 */
@SpringBootApplication(scanBasePackages = {
    "de.mhus.nimbus.generator.simple",
    "de.mhus.nimbus.common"
})
public class SimpleGeneratorApplication {

    public static void main(String[] args) {
        SpringApplication.run(SimpleGeneratorApplication.class, args);
    }
}
