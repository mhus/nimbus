package de.mhus.nimbus.world.control.scheduled;

import de.mhus.nimbus.world.control.service.ChunkUpdateService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled task to process dirty chunks.
 * Runs at fixed intervals to regenerate chunks affected by layer changes.
 */
@Component
@ConditionalOnExpression("'WorldEditor'.equals('${spring.application.name}')")
@RequiredArgsConstructor
@Slf4j
public class ChunkUpdateTask {

    private final ChunkUpdateService chunkUpdateService;

    @Value("${world.control.chunk-update-batch-size:10}")
    private int batchSize;

    @PostConstruct
    public void init() {
        log.info("Chunk update task initialized");
    }

    /**
     * Scheduled task to process dirty chunks.
     * Runs at fixed intervals to regenerate chunks affected by layer changes.
     */
    @Scheduled(fixedDelayString = "#{${world.control.chunk-update-interval-ms:5000}}")
    public void processChunkUpdates() {
        try {
            int processed = chunkUpdateService.processDirtyChunks(batchSize);

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
