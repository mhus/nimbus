package de.mhus.nimbus.tools.demoimport;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * Demo Import Tool - Automated migration from test_server to MongoDB.
 *
 * Usage (no parameters needed):
 *   cd server/tools/demo-import
 *   mvn spring-boot:run
 *
 * OR:
 *   java -jar demo-import.jar
 *
 * Imports from:
 * - ../../client/packages/test_server/files/     (templates)
 * - ../../client/packages/test_server/data/      (world data)
 *
 * What gets imported:
 * - World configuration (main world from data/worlds/main/info.json)
 * - BlockTypes (614 templates)
 * - ItemTypes (5 templates)
 * - EntityModels (4 templates)
 * - Backdrops (9 configs)
 * - Entity templates (from files/entity/)
 * - World entity instances (from data/worlds/main/entities/)
 * - Assets (641+ files with .info metadata)
 *
 * Re-runnable: Can be executed multiple times (fresh import each time).
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

    private final MasterImporter masterImporter;

    public static void main(String[] args) {
        System.exit(SpringApplication.exit(SpringApplication.run(DemoImportApplication.class, args)));
    }

    @Override
    public void run(String... args) throws Exception {
        masterImporter.importAll();
    }
}
