package de.mhus.nimbus.tools.cleanup;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class LocalCleanupApplication implements CommandLineRunner {

    private static final Logger LOG = LoggerFactory.getLogger(LocalCleanupApplication.class);
    private final MongoCleanupService cleanupService;

    public LocalCleanupApplication(MongoCleanupService cleanupService) {
        this.cleanupService = cleanupService;
    }

    public static void main(String[] args) {
        SpringApplication.run(LocalCleanupApplication.class, args);
    }

    @Override
    public void run(String... args) {
        LOG.warn("Starte lokalen Cleanup - ALLE Daten werden gelöscht!");
        int exitCode = 0;
        try {
            cleanupService.cleanup();
            LOG.info("Cleanup abgeschlossen.");
        } catch (Exception e) {
            LOG.error("Cleanup fehlgeschlagen: {}", e.toString());
            exitCode = 1;
        }
        LOG.info("Beende Anwendung mit Exit-Code {}", exitCode);
        System.exit(exitCode);
    }
}
