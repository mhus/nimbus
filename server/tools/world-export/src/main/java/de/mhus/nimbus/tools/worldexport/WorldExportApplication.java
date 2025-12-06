package de.mhus.nimbus.tools.worldexport;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ReflectiveScan;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * World Export Application - Exports all world-related MongoDB collections to JSON files.
 *
 * <p>This tool exports collections configured in application.yaml to JSON Lines files
 * in the specified output directory.</p>
 *
 * <p>Configuration:</p>
 * <ul>
 *   <li>export.output-path: Directory for export files (default: ./export)</li>
 *   <li>export.collections: List of collection names to export</li>
 *   <li>spring.data.mongodb.uri: MongoDB connection URI</li>
 * </ul>
 *
 * <p>Example usage:</p>
 * <pre>
 * mvn spring-boot:run
 * # or
 * java -jar world-export.jar --export.output-path=/data/exports
 * </pre>
 */
@EnableMongoAuditing
@ReflectiveScan(basePackages = {
        "de.mhus.nimbus.tools.worldexport",
        "de.mhus.nimbus.shared",
        "de.mhus.nimbus.world.shared"
})
@EnableMongoRepositories(basePackages = {
        "de.mhus.nimbus.tools.worldexport",
        "de.mhus.nimbus.shared",
        "de.mhus.nimbus.world.shared"
})
@ConfigurationPropertiesScan
@SpringBootApplication
@ComponentScan(basePackages = {
        "de.mhus.nimbus.tools.worldexport",
        "de.mhus.nimbus.shared",
        "de.mhus.nimbus.world.shared"
})
@RequiredArgsConstructor
@Slf4j
public class WorldExportApplication implements CommandLineRunner {

    private final MasterExporter masterExporter;

    public static void main(String[] args) {
        SpringApplication.run(WorldExportApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        log.info("=".repeat(70));
        log.info("WORLD EXPORT APPLICATION - Starting export");
        log.info("=".repeat(70));

        try {
            ExportStats stats = masterExporter.exportAll();

            log.info("");
            log.info("=".repeat(70));
            log.info("EXPORT COMPLETED");
            log.info("=".repeat(70));
            log.info("Total collections: {}", stats.getTotalCollections());
            log.info("Successful:        {}", stats.getSuccessCount());
            log.info("Failed:            {}", stats.getFailureCount());
            log.info("Total entities:    {}", stats.getTotalEntities());
            log.info("Duration:          {:.2f} seconds", stats.getDurationMs() / 1000.0);
            log.info("=".repeat(70));

            if (stats.getFailureCount() > 0) {
                System.exit(1);
            }

        } catch (Exception e) {
            log.error("=".repeat(70));
            log.error("EXPORT FAILED WITH ERROR", e);
            log.error("=".repeat(70));
            System.exit(1);
        }
        System.exit(0);
    }
}
