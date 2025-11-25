package de.mhus.nimbus.tools.cleanup;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@Slf4j
@RequiredArgsConstructor
public class LocalCleanupApplication implements CommandLineRunner {

    private final MongoCleanupService mongoCleanupService;
    private final ConfidentialCleanupService confidentialCleanupService;

    public static void main(String[] args) {
        SpringApplication.run(LocalCleanupApplication.class, args);
    }

    @Override
    public void run(String... args) {
        log.warn("Starte lokalen Cleanup - ALLE Daten werden gelöscht!");
        int exitCode = 0;
        try {
            mongoCleanupService.cleanup();
            log.info("Mongo Cleanup abgeschlossen.");
        } catch (Exception e) {
            log.error("Cleanup fehlgeschlagen: {}", e.toString());
            exitCode = 1;
        }
        try {
            confidentialCleanupService.cleanup();
            log.info("Confidential Cleanup abgeschlossen.");
        } catch (Exception e) {
            log.error("Cleanup fehlgeschlagen: {}", e.toString());
            exitCode = 1;
        }
        log.info("Beende Anwendung mit Exit-Code {}", exitCode);
        System.exit(exitCode);
    }
}
