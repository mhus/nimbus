package de.mhus.nimbus.world.life.scheduled;

import de.mhus.nimbus.world.life.redis.ChunkListRequestPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled task to refresh active chunk list.
 * Requests current chunks from all world-player pods every 5 minutes.
 *
 * This ensures the chunk alive service has accurate data even if:
 * - Chunk registration messages were lost
 * - Player sessions disconnected without unregistering chunks
 * - Redis had temporary failures
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ChunkRefreshTask {

    private final ChunkListRequestPublisher chunkListPublisher;

    /**
     * Request chunk lists from all world-player pods.
     * Scheduled every 5 minutes (configurable via world.life.chunk-refresh-interval-ms).
     */
    @Scheduled(fixedDelayString = "#{${world.life.chunk-refresh-interval-ms:300000}}")
    public void refreshChunkList() {
        log.debug("Starting scheduled chunk list refresh");

        try {
            String requestId = chunkListPublisher.requestChunkLists();

            if (requestId != null) {
                log.info("Chunk refresh initiated: requestId={}", requestId);
            } else {
                log.warn("Failed to initiate chunk refresh");
            }

        } catch (Exception e) {
            log.error("Error during chunk refresh task", e);
        }
    }
}
