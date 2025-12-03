package de.mhus.nimbus.tools.demoimport;

import de.mhus.nimbus.tools.demoimport.importers.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Master Importer - Orchestrates all imports in correct order.
 * Can be run multiple times (clears old data before import).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MasterImporter {

    private final WorldImporter worldImporter;
    private final BlockTypeImporter blockTypeImporter;
    private final ItemTypeImporter itemTypeImporter;
    private final EntityModelImporter entityModelImporter;
    private final BackdropImporter backdropImporter;
    private final EntityImporter entityImporter;
    private final WorldEntityImporter worldEntityImporter;
    private final AssetImporter assetImporter;
    private final ChunkImporter chunkImporter;
    private final ItemImporter itemImporter;

    /**
     * Execute complete import from test_server.
     *
     * Import order:
     * 1. World configuration (main world)
     * 2. Templates (BlockTypes, ItemTypes, EntityModels, Backdrops)
     * 3. Entity templates (from files/entity/)
     * 4. World entity instances (from data/worlds/main/entities/)
     * 5. Chunks (from data/worlds/main/chunks/)
     * 6. Item positions (from data/worlds/main/items.json)
     * 7. Assets (from files/assets/)
     */
    public ImportStats importAll() throws Exception {
        log.info("=".repeat(70));
        log.info("MASTER IMPORTER - Starting complete test_server migration");
        log.info("=".repeat(70));
        log.info("");

        long startTime = System.currentTimeMillis();
        ImportStats totalStats = new ImportStats();

        try {
            // Phase 1: World Configuration
            log.info(">>> Phase 1: World Configuration");
            ImportStats worldStats = worldImporter.importAll();
            totalStats.merge(worldStats);
            log.info("");

            // Phase 2: Templates
            log.info(">>> Phase 2: Entity Templates");
            log.info("");

            log.info("--- BlockTypes ---");
            ImportStats blockTypeStats = blockTypeImporter.importAll();
            totalStats.merge(blockTypeStats);
            log.info("");

            log.info("--- ItemTypes ---");
            ImportStats itemTypeStats = itemTypeImporter.importAll();
            totalStats.merge(itemTypeStats);
            log.info("");

            log.info("--- EntityModels ---");
            ImportStats entityModelStats = entityModelImporter.importAll();
            totalStats.merge(entityModelStats);
            log.info("");

            log.info("--- Backdrops ---");
            ImportStats backdropStats = backdropImporter.importAll();
            totalStats.merge(backdropStats);
            log.info("");

            // Phase 3: Entity Templates (from files/entity/)
            log.info(">>> Phase 3: Entity Templates");
            ImportStats entityTemplateStats = entityImporter.importAll();
            totalStats.merge(entityTemplateStats);
            log.info("");

            // Phase 4: World Entity Instances (from data/worlds/main/entities/)
            log.info(">>> Phase 4: World Entity Instances");
            ImportStats worldEntityStats = worldEntityImporter.importAll();
            totalStats.merge(worldEntityStats);
            log.info("");

            // Phase 5: Chunks (from data/worlds/main/chunks/)
            log.info(">>> Phase 5: Chunks (201+ files)");
            ImportStats chunkStats = chunkImporter.importAll();
            totalStats.merge(chunkStats);
            log.info("");

            // Phase 6: Item Positions (from data/worlds/main/items.json)
            log.info(">>> Phase 6: Item Positions");
            ImportStats itemStats = itemImporter.importAll();
            totalStats.merge(itemStats);
            log.info("");

            // Phase 7: Assets (can be slow)
            log.info(">>> Phase 7: Assets (641+ files - may take a while)");
            ImportStats assetStats = assetImporter.importAll();
            totalStats.merge(assetStats);
            log.info("");

            // Summary
            long duration = System.currentTimeMillis() - startTime;
            double seconds = duration / 1000.0;

            log.info("=".repeat(70));
            log.info("IMPORT COMPLETED SUCCESSFULLY");
            log.info("=".repeat(70));
            log.info("Total entities:  {} imported", totalStats.getSuccessCount());
            log.info("Skipped:         {} (validation errors)", totalStats.getSkippedCount());
            log.info("Failed:          {} (critical errors)", totalStats.getFailureCount());
            log.info("Duration:        {:.2f} seconds", seconds);
            log.info("=".repeat(70));

            return totalStats;

        } catch (Exception e) {
            log.error("=".repeat(70));
            log.error("IMPORT FAILED WITH ERROR");
            log.error("=".repeat(70), e);
            throw e;
        }
    }
}
