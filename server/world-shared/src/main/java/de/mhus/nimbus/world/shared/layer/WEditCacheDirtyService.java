package de.mhus.nimbus.world.shared.layer;

import de.mhus.nimbus.world.shared.redis.WorldRedisLockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for managing WEditCacheDirty entities.
 * Handles the work queue for merging cached edits into layers.
 * Scheduled task processes pending dirty entries and commits changes to layers.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WEditCacheDirtyService {

    private final WEditCacheDirtyRepository dirtyRepository;
    private final WEditCacheRepository cacheRepository;
    private final WEditCacheService cacheService;
    private final WDirtyChunkService dirtyChunkService;
    private final WorldRedisLockService lockService;
    private final WLayerService layerService;

    private static final Duration LOCK_TTL = Duration.ofMinutes(5);
    private static final int MAX_ENTRIES_PER_CYCLE = 10;

    /**
     * Mark a layer as dirty (has pending changes in edit cache).
     * If already dirty, does nothing (keeps original timestamp).
     *
     * @param worldId World identifier
     * @param layerDataId Layer data identifier
     */
    @Transactional
    public void markLayerDirty(String worldId, String layerDataId) {
        if (dirtyRepository.existsByWorldIdAndLayerDataId(worldId, layerDataId)) {
            log.debug("Layer already marked dirty: worldId={}, layerDataId={}", worldId, layerDataId);
            return;
        }

        WEditCacheDirty dirty = WEditCacheDirty.builder()
                .worldId(worldId)
                .layerDataId(layerDataId)
                .build();
        dirty.touch();
        dirtyRepository.save(dirty);

        log.info("Marked layer dirty: worldId={}, layerDataId={}", worldId, layerDataId);
    }

    /**
     * Clear dirty flag for a layer (after successful processing).
     *
     * @param worldId World identifier
     * @param layerDataId Layer data identifier
     */
    @Transactional
    public void clearDirty(String worldId, String layerDataId) {
        dirtyRepository.deleteByWorldIdAndLayerDataId(worldId, layerDataId);
        log.debug("Cleared dirty flag: worldId={}, layerDataId={}", worldId, layerDataId);
    }

    /**
     * Check if a layer is marked as dirty.
     *
     * @param worldId World identifier
     * @param layerDataId Layer data identifier
     * @return true if layer is dirty
     */
    @Transactional(readOnly = true)
    public boolean isDirty(String worldId, String layerDataId) {
        return dirtyRepository.existsByWorldIdAndLayerDataId(worldId, layerDataId);
    }

    /**
     * Get all dirty layers for a world.
     *
     * @param worldId World identifier
     * @return List of dirty entries
     */
    @Transactional(readOnly = true)
    public List<WEditCacheDirty> getDirtyLayers(String worldId) {
        return dirtyRepository.findByWorldId(worldId);
    }

    /**
     * Get all dirty layers ordered by age (oldest first).
     *
     * @return List of dirty entries
     */
    @Transactional(readOnly = true)
    public List<WEditCacheDirty> getAllDirtyLayers() {
        return dirtyRepository.findAllByOrderByCreatedAtAsc();
    }

    /**
     * Scheduled task that processes pending dirty layers.
     * Merges cached edits into layers and marks affected chunks dirty.
     * Uses Redis locks to prevent concurrent processing across pods.
     */
    @Scheduled(fixedDelayString = "#{${world.edit-cache.processing-interval-ms:10000}}")
    @ConditionalOnExpression("'${world.edit-cache.processing-enabled:true}' == 'true'")
    public void processEditCacheDirty() {
        try {
            List<WEditCacheDirty> dirtyEntries = dirtyRepository.findAllByOrderByCreatedAtAsc();

            if (dirtyEntries.isEmpty()) {
                log.trace("No dirty edit cache entries to process");
                return;
            }

            log.debug("Found {} dirty edit cache entries", dirtyEntries.size());

            int processed = 0;
            int skipped = 0;
            int failed = 0;

            for (WEditCacheDirty dirty : dirtyEntries) {
                if (processed >= MAX_ENTRIES_PER_CYCLE) {
                    log.debug("Reached max entries per cycle ({}), stopping", MAX_ENTRIES_PER_CYCLE);
                    break;
                }

                String lockKey = "edit-cache-dirty:" + dirty.getWorldId() + ":" + dirty.getLayerDataId();
                String lockToken = lockService.acquireGenericLock(lockKey, LOCK_TTL);

                if (lockToken == null) {
                    log.debug("Layer is locked by another pod, skipping: worldId={}, layerDataId={}",
                            dirty.getWorldId(), dirty.getLayerDataId());
                    skipped++;
                    continue;
                }

                try {
                    processLayer(dirty.getWorldId(), dirty.getLayerDataId());
                    processed++;
                } catch (Exception e) {
                    log.error("Error processing dirty layer: worldId={}, layerDataId={}",
                            dirty.getWorldId(), dirty.getLayerDataId(), e);
                    failed++;
                } finally {
                    lockService.releaseGenericLock(lockKey, lockToken);
                }
            }

            if (processed > 0 || failed > 0) {
                log.info("Edit cache dirty processing cycle: processed={} skipped={} failed={} remaining={}",
                        processed, skipped, failed, dirtyEntries.size() - processed - skipped - failed);
            }

        } catch (Exception e) {
            log.error("Error during edit cache dirty processing cycle", e);
        }
    }

    /**
     * Process a single dirty layer - merge cached edits into layer.
     * This method:
     * 1. Loads all cached blocks for the layer
     * 2. Writes them to WLayerModel
     * 3. Deletes the cached blocks
     * 4. Creates WDirtyChunk entries for affected chunks
     * 5. Removes the dirty flag
     *
     * @param worldId World identifier
     * @param layerDataId Layer data identifier
     */
    @Transactional
    public void processLayer(String worldId, String layerDataId) {
        log.info("Processing dirty layer: worldId={}, layerDataId={}", worldId, layerDataId);

        // Load all cached blocks for this layer
        List<WEditCache> cachedBlocks = cacheService.findByWorldIdAndLayerDataId(worldId, layerDataId);

        if (cachedBlocks.isEmpty()) {
            log.warn("No cached blocks found for dirty layer: worldId={}, layerDataId={}", worldId, layerDataId);
            clearDirty(worldId, layerDataId);
            return;
        }

        log.debug("Found {} cached blocks to merge into layer", cachedBlocks.size());

        // Get affected chunks for marking dirty after merge
        Set<String> affectedChunks = cachedBlocks.stream()
                .map(WEditCache::getChunk)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        // Get layer information for source field
        Optional<WLayer> layerOpt = layerService.findByWorldIdAndLayerDataId(worldId, layerDataId);
        String layerName = layerOpt.map(WLayer::getName).orElse("unknown");

        // TODO: Write blocks to WLayerModel
        // This requires transformation of world coordinates to layer-local coordinates
        // and merging with existing WLayerModel content
        // For now, this is a placeholder - implementation will be added in next step
        log.info("TODO: Write {} blocks to WLayerModel for layerDataId={}", cachedBlocks.size(), layerDataId);

        // TODO: Send block updates to clients with source information
        // Example usage of BlockUpdateService with source field:
        //
        // String source = layerDataId + ":" + layerName;
        // for (WEditCache cache : cachedBlocks) {
        //     Block block = cache.getBlock().getBlock();
        //     blockUpdateService.sendBlockUpdateWithSource(
        //         worldId, sessionId, cache.getX(), 0, cache.getZ(),
        //         block, source, null
        //     );
        // }

        // Check layer type - if MODEL, trigger transfer to WLayerTerrain
        if (layerOpt.isPresent() && layerOpt.get().getLayerType() == LayerType.MODEL) {
            log.info("Layer type is MODEL, will need terrain transfer after WLayerModel update");
            // TODO: Trigger WLayerModel to WLayerTerrain transfer
        }

        // Delete cached blocks after successful merge
        long deletedCount = cacheService.deleteByWorldIdAndLayerDataId(worldId, layerDataId);
        log.debug("Deleted {} cached blocks after merge", deletedCount);

        // Mark affected chunks as dirty for regeneration
        dirtyChunkService.markChunksDirty(worldId, new ArrayList<>(affectedChunks),
                "edit_cache_applied:layer=" + layerDataId);
        log.debug("Marked {} chunks as dirty", affectedChunks.size());

        // Remove dirty flag
        clearDirty(worldId, layerDataId);

        log.info("Successfully processed dirty layer: worldId={}, layerDataId={}, blocks={}, chunks={}",
                worldId, layerDataId, cachedBlocks.size(), affectedChunks.size());
    }

    /**
     * Trigger immediate processing of a specific layer (for "Apply Changes").
     * Marks layer dirty and then processes it immediately.
     *
     * @param worldId World identifier
     * @param layerDataId Layer data identifier
     */
    @Transactional
    public void applyChanges(String worldId, String layerDataId) {
        log.info("Apply changes requested: worldId={}, layerDataId={}", worldId, layerDataId);

        // Check if there are any cached blocks
        long cachedCount = cacheService.countByWorldIdAndLayerDataId(worldId, layerDataId);
        if (cachedCount == 0) {
            log.warn("No cached changes to apply: worldId={}, layerDataId={}", worldId, layerDataId);
            return;
        }

        // Mark dirty and process immediately
        markLayerDirty(worldId, layerDataId);
        processLayer(worldId, layerDataId);
    }

    /**
     * Discard all changes for a layer (for "Discard Changes").
     * Deletes all cached blocks and marks affected chunks dirty for refresh.
     *
     * @param worldId World identifier
     * @param layerDataId Layer data identifier
     * @return Number of discarded blocks
     */
    @Transactional
    public long discardChanges(String worldId, String layerDataId) {
        log.info("Discard changes requested: worldId={}, layerDataId={}", worldId, layerDataId);

        // Get affected chunks before deleting
        List<WEditCache> cachedBlocks = cacheService.findByWorldIdAndLayerDataId(worldId, layerDataId);
        Set<String> affectedChunks = cachedBlocks.stream()
                .map(WEditCache::getChunk)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        // Delete all cached blocks
        long deletedCount = cacheService.deleteByWorldIdAndLayerDataId(worldId, layerDataId);

        // Mark affected chunks dirty to trigger refresh on clients
        if (!affectedChunks.isEmpty()) {
            dirtyChunkService.markChunksDirty(worldId, new ArrayList<>(affectedChunks),
                    "edit_cache_discarded:layer=" + layerDataId);
            log.debug("Marked {} chunks dirty for refresh", affectedChunks.size());
        }

        // Clear dirty flag if exists
        if (isDirty(worldId, layerDataId)) {
            clearDirty(worldId, layerDataId);
        }

        log.info("Discarded {} cached blocks for layer: worldId={}, layerDataId={}, chunks={}",
                deletedCount, worldId, layerDataId, affectedChunks.size());

        return deletedCount;
    }
}
