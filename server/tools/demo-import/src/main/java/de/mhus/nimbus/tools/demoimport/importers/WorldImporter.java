package de.mhus.nimbus.tools.demoimport.importers;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.mhus.nimbus.generated.types.WorldInfo;
import de.mhus.nimbus.tools.demoimport.ImportStats;
import de.mhus.nimbus.world.shared.world.WWorld;
import de.mhus.nimbus.world.shared.world.WWorldService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Imports World configuration from test_server data.
 * Reads from: {data-path}/worlds/main/info.json
 *
 * Creates WWorld entity with WorldInfo publicData.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class WorldImporter {

    private final WWorldService worldService;
    private final ObjectMapper objectMapper;

    @Value("${import.data-path:../../client/packages/test_server/data}")
    private String dataPath;

    public ImportStats importAll() throws Exception {
        log.info("Starting World import from: {}/worlds/main/info.json", dataPath);

        ImportStats stats = new ImportStats();
        Path worldInfoPath = Path.of(dataPath, "worlds", "main", "info.json");

        if (!Files.exists(worldInfoPath)) {
            log.warn("World info.json not found: {}", worldInfoPath);
            return stats;
        }

        try {
            // Read and parse JSON
            File infoFile = worldInfoPath.toFile();
            WorldInfo worldInfo = objectMapper.readValue(infoFile, WorldInfo.class);

            if (worldInfo.getWorldId() == null || worldInfo.getWorldId().isBlank()) {
                log.error("World info.json has no worldId");
                stats.incrementFailure();
                return stats;
            }

            // Create new world
            WWorld world = WWorld.builder()
                    .worldId(worldInfo.getWorldId())
                    .publicData(worldInfo)
                    .regionId(null)
                    .enabled(true)
                    .groundLevel(0)           // Default ground level
                    .waterLevel(null)         // No water by default
                    .groundBlockType("w:310") // Grass block
                    .waterBlockType("core:water")
                    .build();
            world.touchForCreate();

            worldService.createWorld(worldInfo.getWorldId(), worldInfo);
            stats.incrementSuccess();

            log.info("Created world: {} ({})", worldInfo.getWorldId(), worldInfo.getName());

        } catch (Exception e) {
            log.error("Failed to import world from info.json", e);
            stats.incrementFailure();
        }

        log.info("World import completed: {} imported, {} skipped, {} failed",
                stats.getSuccessCount(), stats.getSkippedCount(), stats.getFailureCount());

        return stats;
    }
}
