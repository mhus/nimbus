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
    private final WLayerModelRepository modelRepository;

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

        // Get layer information
        Optional<WLayer> layerOpt = layerService.findByWorldIdAndLayerDataId(worldId, layerDataId);
        if (layerOpt.isEmpty()) {
            log.error("Layer not found: worldId={}, layerDataId={}", worldId, layerDataId);
            clearDirty(worldId, layerDataId);
            return;
        }
        WLayer layer = layerOpt.get();
        String layerName = layer.getName();

        // Write blocks to WLayerModel (only for MODEL type layers)
        if (layer.getLayerType() == LayerType.MODEL) {
            mergeBlocksIntoLayerModel(worldId, layerDataId, cachedBlocks);
            log.info("Merged {} blocks into WLayerModel for layerDataId={}", cachedBlocks.size(), layerDataId);
        } else {
            log.info("Layer type is {}, skipping WLayerModel merge", layer.getLayerType());
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

    /**
     * Merge cached blocks into WLayerModel.
     * Transforms world coordinates to layer-relative coordinates (with inverse rotation).
     * Merges blocks into existing WLayerModel content.
     *
     * @param worldId World identifier
     * @param layerDataId Layer data identifier
     * @param cachedBlocks List of cached blocks to merge
     */
    private void mergeBlocksIntoLayerModel(String worldId, String layerDataId, List<WEditCache> cachedBlocks) {
        // Get all models for this layer (ordered by overlay order)
        List<WLayerModel> models = modelRepository.findByLayerDataIdOrderByOrder(layerDataId);

        if (models.isEmpty()) {
            log.warn("No models found for layer, cannot merge blocks: layerDataId={}", layerDataId);
            return;
        }

        // For now, we only support single-model layers
        // Multi-model support would require determining which model each block belongs to
        if (models.size() > 1) {
            log.warn("Layer has multiple models ({}), using first model for merge", models.size());
        }

        WLayerModel model = models.get(0);
        log.debug("Merging blocks into model: modelId={}, mountPoint=({},{},{}), rotation={}",
                model.getId(), model.getMountX(), model.getMountY(), model.getMountZ(), model.getRotation());

        // Build position index of existing blocks
        List<LayerBlock> content = model.getContent();
        if (content == null) {
            content = new ArrayList<>();
            model.setContent(content);
        }

        Map<String, LayerBlock> blockIndex = new HashMap<>();
        for (LayerBlock layerBlock : content) {
            if (layerBlock.getBlock() != null && layerBlock.getBlock().getPosition() != null) {
                de.mhus.nimbus.generated.types.Vector3Int pos = layerBlock.getBlock().getPosition();
                String posKey = pos.getX() + ":" + pos.getY() + ":" + pos.getZ();
                blockIndex.put(posKey, layerBlock);
            }
        }

        log.debug("Existing blocks in model: {}", blockIndex.size());

        // Transform and merge each cached block
        int added = 0;
        int updated = 0;
        for (WEditCache cache : cachedBlocks) {
            de.mhus.nimbus.generated.types.Block block = cache.getBlock().getBlock();

            // Transform world coordinates to layer-relative coordinates
            de.mhus.nimbus.generated.types.Vector3Int relativePos = worldToLayerCoordinates(
                    cache.getX(),
                    cache.getBlock().getBlock().getPosition().getY(), // Y from block, not cache
                    cache.getZ(),
                    model.getMountX(),
                    model.getMountY(),
                    model.getMountZ(),
                    model.getRotation()
            );

            // Create new block with relative position
            de.mhus.nimbus.generated.types.Block transformedBlock = cloneBlockWithPosition(block, relativePos);

            // Create LayerBlock
            LayerBlock layerBlock = LayerBlock.builder()
                    .block(transformedBlock)
                    .group(cache.getBlock().getGroup())
                    .metadata(cache.getBlock().getMetadata())
                    .build();

            // Add or update in index
            String posKey = relativePos.getX() + ":" + relativePos.getY() + ":" + relativePos.getZ();
            if (blockIndex.containsKey(posKey)) {
                updated++;
            } else {
                added++;
            }
            blockIndex.put(posKey, layerBlock);
        }

        // Rebuild content list from index
        List<LayerBlock> newContent = new ArrayList<>(blockIndex.values());

        // Update model using layerService
        layerService.updateModel(model.getId(), m -> {
            m.setContent(newContent);
        });

        log.info("Merged blocks into model: modelId={}, added={}, updated={}, total={}",
                model.getId(), added, updated, newContent.size());
    }

    /**
     * Transform world coordinates to layer-relative coordinates.
     * Applies inverse rotation to account for layer model rotation.
     *
     * @param worldX World X coordinate
     * @param worldY World Y coordinate
     * @param worldZ World Z coordinate
     * @param mountX Layer mount X coordinate
     * @param mountY Layer mount Y coordinate
     * @param mountZ Layer mount Z coordinate
     * @param rotation Layer rotation (0-3, in 90-degree steps)
     * @return Relative coordinates
     */
    private de.mhus.nimbus.generated.types.Vector3Int worldToLayerCoordinates(
            int worldX, int worldY, int worldZ,
            int mountX, int mountY, int mountZ,
            int rotation) {

        // 1. Subtract mount point to get offset from mount
        int offsetX = worldX - mountX;
        int offsetY = worldY - mountY;
        int offsetZ = worldZ - mountZ;

        // 2. Apply inverse rotation
        int[] rotated = applyInverseRotation(offsetX, offsetZ, rotation);

        // 3. Create relative position
        de.mhus.nimbus.generated.types.Vector3Int relativePos = new de.mhus.nimbus.generated.types.Vector3Int();
        relativePos.setX(rotated[0]);
        relativePos.setY(offsetY); // Y is not affected by rotation
        relativePos.setZ(rotated[1]);

        return relativePos;
    }

    /**
     * Apply inverse rotation to coordinates.
     * Inverse of the rotation applied when rendering blocks.
     *
     * @param x X coordinate
     * @param z Z coordinate
     * @param rotation Rotation in 90-degree steps (0-3)
     * @return Inverse rotated coordinates [x, z]
     */
    private int[] applyInverseRotation(int x, int z, int rotation) {
        // Normalize rotation to 0-3
        int rot = rotation % 4;
        if (rot < 0) rot += 4;

        // Inverse rotation: apply opposite rotation
        return switch (rot) {
            case 0 -> new int[]{x, z};          // No rotation
            case 1 -> new int[]{z, -x};         // 90° counter-clockwise (inverse of 90° CW)
            case 2 -> new int[]{-x, -z};        // 180° (inverse of 180°)
            case 3 -> new int[]{-z, x};         // 270° counter-clockwise (inverse of 270° CW)
            default -> new int[]{x, z};
        };
    }

    /**
     * Clone a block with a new position.
     *
     * @param original Original block
     * @param newPosition New position
     * @return Cloned block with new position
     */
    private de.mhus.nimbus.generated.types.Block cloneBlockWithPosition(
            de.mhus.nimbus.generated.types.Block original,
            de.mhus.nimbus.generated.types.Vector3Int newPosition) {

        de.mhus.nimbus.generated.types.Block cloned = new de.mhus.nimbus.generated.types.Block();
        cloned.setBlockTypeId(original.getBlockTypeId());
        cloned.setPosition(newPosition);
        cloned.setOffsets(original.getOffsets());
        cloned.setRotation(original.getRotation());
        cloned.setFaceVisibility(original.getFaceVisibility());
        cloned.setCornerHeights(original.getCornerHeights());
        cloned.setStatus(original.getStatus());
        cloned.setModifiers(original.getModifiers());
        cloned.setMetadata(original.getMetadata());
        cloned.setLevel(original.getLevel());
        cloned.setSource(original.getSource());

        return cloned;
    }
}
