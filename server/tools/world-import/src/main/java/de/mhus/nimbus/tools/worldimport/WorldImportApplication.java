package de.mhus.nimbus.tools.worldimport;

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

/**
 * World Import Application - Imports all world-related MongoDB collections from JSON files with schema migration.
 *
 * <p>This tool imports collections configured in application.yaml from JSON Lines files
 * in the specified input directory. Each entity is automatically migrated to the target
 * schema version using the {@link de.mhus.nimbus.shared.service.SchemaMigrationService}.</p>
 *
 * <p>Configuration:</p>
 * <ul>
 *   <li>import.input-path: Directory containing import files (default: ./export)</li>
 *   <li>import.collections: List of collection names to import (order matters!)</li>
 *   <li>spring.data.mongodb.uri: MongoDB connection URI</li>
 * </ul>
 *
 * <p>Import order is important for referential integrity:</p>
 * <ol>
 *   <li>Shared/Storage collections (assets, storage)</li>
 *   <li>World configuration (worlds)</li>
 *   <li>Templates and types (backdrops, blocktypes, itemtypes, entity_models)</li>
 *   <li>Layers (layers, layer_terrain, layer_models)</li>
 *   <li>World data (chunks, entities, items, item_positions)</li>
 * </ol>
 *
 * <p>Example usage:</p>
 * <pre>
 * mvn spring-boot:run
 * # or
 * java -jar world-import.jar --import.input-path=/data/exports
 * </pre>
 */
@EnableMongoAuditing
@ReflectiveScan(basePackages = {
        "de.mhus.nimbus.tools.worldimport",
        "de.mhus.nimbus.shared",
        "de.mhus.nimbus.world.shared"
})
@EnableMongoRepositories(basePackages = {
        "de.mhus.nimbus.tools.worldimport",
        "de.mhus.nimbus.shared",
        "de.mhus.nimbus.world.shared"
})
@ConfigurationPropertiesScan
@SpringBootApplication
@ComponentScan(basePackages = {
        "de.mhus.nimbus.tools.worldimport",
        "de.mhus.nimbus.shared",
        "de.mhus.nimbus.world.shared"
})
@RequiredArgsConstructor
@Slf4j
public class WorldImportApplication implements CommandLineRunner {

    private final MasterImporter masterImporter;

    public static void main(String[] args) {
        SpringApplication.run(WorldImportApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        log.info("=".repeat(70));
        log.info("WORLD IMPORT APPLICATION - Starting import with schema migration");
        log.info("=".repeat(70));

        try {
            ImportStats stats = masterImporter.importAll();

            log.info("");
            log.info("=".repeat(70));
            log.info("IMPORT COMPLETED");
            log.info("=".repeat(70));
            log.info("Total collections: {}", stats.getTotalCollections());
            log.info("Successful:        {}", stats.getSuccessCount());
            log.info("Failed:            {}", stats.getFailureCount());
            log.info("Total entities:    {}", stats.getTotalEntities());
            log.info("Migrated entities: {}", stats.getTotalMigrated());
            log.info("Duration:          {:.2f} seconds", stats.getDurationMs() / 1000.0);
            log.info("=".repeat(70));

            if (stats.getFailureCount() > 0) {
                log.error("Failed collections: {}", stats.getFailedCollections());
                System.exit(1);
            }

        } catch (Exception e) {
            log.error("=".repeat(70));
            log.error("IMPORT FAILED WITH ERROR", e);
            log.error("=".repeat(70));
            System.exit(1);
        }
        System.exit(0);
    }
}
