package de.mhus.nimbus.tools.demoimport;

import de.mhus.nimbus.tools.demoimport.importers.AssetImporter;
import de.mhus.nimbus.tools.demoimport.importers.BackdropImporter;
import de.mhus.nimbus.tools.demoimport.importers.BlockTypeImporter;
import de.mhus.nimbus.tools.demoimport.importers.EntityImporter;
import de.mhus.nimbus.tools.demoimport.importers.EntityModelImporter;
import de.mhus.nimbus.tools.demoimport.importers.ItemTypeImporter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * Demo Import Tool - Imports test_server data into MongoDB.
 *
 * Usage:
 *   java -jar demo-import.jar --import.source-path=/path/to/test_server/files
 *
 * Imports:
 * - BlockTypes (614 files from blocktypes/)
 * - ItemTypes (5 files from itemtypes/)
 * - EntityModels (4 files from entitymodels/)
 * - Backdrops (9 files from backdrops/)
 * - Entities (player templates from entity/)
 * - Assets (641+ binary files with .info metadata from assets/)
 */
@SpringBootApplication
@ComponentScan(basePackages = {
        "de.mhus.nimbus.tools.demoimport",
        "de.mhus.nimbus.world.shared",
        "de.mhus.nimbus.shared"
})
@RequiredArgsConstructor
@Slf4j
public class DemoImportApplication implements CommandLineRunner {

    private final BlockTypeImporter blockTypeImporter;
    private final ItemTypeImporter itemTypeImporter;
    private final EntityModelImporter entityModelImporter;
    private final BackdropImporter backdropImporter;
    private final EntityImporter entityImporter;
    private final AssetImporter assetImporter;

    public static void main(String[] args) {
        System.exit(SpringApplication.exit(SpringApplication.run(DemoImportApplication.class, args)));
    }

    @Override
    public void run(String... args) throws Exception {
        log.info("=".repeat(60));
        log.info("Demo Import Tool - Starting");
        log.info("=".repeat(60));

        long startTime = System.currentTimeMillis();
        ImportStats totalStats = new ImportStats();

        try {
            // Import BlockTypes
            log.info("");
            log.info("=== Phase 1: BlockTypes ===");
            ImportStats blockTypeStats = blockTypeImporter.importAll();
            totalStats.merge(blockTypeStats);

            // Import ItemTypes
            log.info("");
            log.info("=== Phase 2: ItemTypes ===");
            ImportStats itemTypeStats = itemTypeImporter.importAll();
            totalStats.merge(itemTypeStats);

            // Import EntityModels
            log.info("");
            log.info("=== Phase 3: EntityModels ===");
            ImportStats entityModelStats = entityModelImporter.importAll();
            totalStats.merge(entityModelStats);

            // Import Backdrops
            log.info("");
            log.info("=== Phase 4: Backdrops ===");
            ImportStats backdropStats = backdropImporter.importAll();
            totalStats.merge(backdropStats);

            // Import Entities
            log.info("");
            log.info("=== Phase 5: Entities ===");
            ImportStats entityStats = entityImporter.importAll();
            totalStats.merge(entityStats);

            // Import Assets (can be slow - 641+ files)
            log.info("");
            log.info("=== Phase 6: Assets ===");
            log.info("This may take a while (641+ files)...");
            ImportStats assetStats = assetImporter.importAll();
            totalStats.merge(assetStats);

            // Summary
            long duration = System.currentTimeMillis() - startTime;
            log.info("");
            log.info("=".repeat(60));
            log.info("Import Completed Successfully");
            log.info("=".repeat(60));
            log.info("Total entities: {} imported, {} skipped, {} failed",
                    totalStats.getSuccessCount(),
                    totalStats.getSkippedCount(),
                    totalStats.getFailureCount());
            log.info("Duration: {}ms ({} seconds)", duration, duration / 1000.0);
            log.info("=".repeat(60));

        } catch (Exception e) {
            log.error("Import failed with error", e);
            throw e;
        }
    }
}
