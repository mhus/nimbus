package de.mhus.nimbus.world.control.scheduled;

import de.mhus.nimbus.world.control.service.ChunkUpdateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled task to process dirty chunks.
 * Runs at fixed intervals to regenerate chunks affected by layer changes.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ChunkUpdateTask {

    private final ChunkUpdateService chunkUpdateService;

    @Value("${world.control.chunk-update-world-id:main}")
    private String worldId;

    @Value("${world.control.chunk-update-batch-size:10}")
    private int batchSize;

    /**
     * Scheduled task to process dirty chunks.
     * Runs at fixed intervals to regenerate chunks affected by layer changes.
     */
    @Scheduled(fixedDelayString = "#{${world.control.chunk-update-interval-ms:5000}}")
    public void processChunkUpdates() {
        try {
            int processed = chunkUpdateService.processDirtyChunks(worldId, batchSize);

            if (processed > 0) {
                log.info("Chunk update task: processed {} dirty chunks", processed);
            } else {
                log.trace("Chunk update task: no dirty chunks to process");
            }

        } catch (Exception e) {
            log.error("Error during chunk update task", e);
        }
    }
}
