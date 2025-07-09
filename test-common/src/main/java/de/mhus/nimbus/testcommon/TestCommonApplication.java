package de.mhus.nimbus.testcommon;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main application class for Test Common module
 * Provides REST API endpoints for testing Nimbus common client beans
 */
@SpringBootApplication(scanBasePackages = {
    "de.mhus.nimbus.testcommon",
    "de.mhus.nimbus.common"
})
public class TestCommonApplication {

    public static void main(String[] args) {
        SpringApplication.run(TestCommonApplication.class, args);
    }
}
