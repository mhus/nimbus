package de.mhus.nimbus.tools.demoimport.importers;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.mhus.nimbus.generated.types.ChunkData;
import de.mhus.nimbus.tools.demoimport.ImportStats;
import de.mhus.nimbus.world.shared.world.WChunkService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

/**
 * Imports Chunks from test_server data.
 * Reads from: {data-path}/worlds/main/chunks/chunk_*.json
 *
 * Creates WChunk entities with ChunkData publicData.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ChunkImporter {

    private final WChunkService chunkService;
    private final ObjectMapper objectMapper;

    @Value("${import.data-path:../../client/packages/test_server/data}")
    private String dataPath;

    @Value("${import.world-id:main}")
    private String worldId;

    public ImportStats importAll() throws Exception {
        log.info("Starting Chunk import from: {}/worlds/{}/chunks/", dataPath, worldId);

        ImportStats stats = new ImportStats();
        Path chunksDir = Path.of(dataPath, "worlds", worldId, "chunks");

        if (!Files.exists(chunksDir) || !Files.isDirectory(chunksDir)) {
            log.warn("Chunks directory not found: {}", chunksDir);
            return stats;
        }

        // Read all chunk_*.json files
        try (Stream<Path> files = Files.list(chunksDir)) {
            files.filter(path -> path.getFileName().toString().matches("chunk_-?\\d+_-?\\d+\\.json"))
                    .sorted()
                    .forEach(chunkPath -> {
                        try {
                            importChunk(chunkPath.toFile(), stats);
                        } catch (Exception e) {
                            log.error("Failed to import chunk: {}", chunkPath.getFileName(), e);
                            stats.incrementFailure();
                        }
                    });
        }

        log.info("Chunk import completed: {} imported, {} skipped, {} failed",
                stats.getSuccessCount(), stats.getSkippedCount(), stats.getFailureCount());

        return stats;
    }

    private void importChunk(File chunkFile, ImportStats stats) throws Exception {
        // Parse JSON to ChunkData
        ChunkData chunkData = objectMapper.readValue(chunkFile, ChunkData.class);

        // Create chunkKey (cx:cz format)
        String chunkKey = chunkData.getCx() + ":" + chunkData.getCz();

        // Check if chunk already exists (create=false to prevent default chunk generation)
        if (chunkService.loadChunkData(worldId, worldId, chunkKey, false).isPresent()) {
            log.trace("Chunk already exists: {} - skipping", chunkKey);
            stats.incrementSkipped();
            return;
        }

        // Save chunk via service (regionId = worldId for main world)
        chunkService.saveChunk(worldId, worldId, chunkKey, chunkData);
        stats.incrementSuccess();

        log.debug("Imported chunk: {} ({} blocks)", chunkKey,
                chunkData.getBlocks() != null ? chunkData.getBlocks().size() : 0);
    }
}
