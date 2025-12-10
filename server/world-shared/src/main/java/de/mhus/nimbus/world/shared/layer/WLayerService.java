package de.mhus.nimbus.world.shared.layer;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.mhus.nimbus.shared.storage.StorageService;
import de.mhus.nimbus.shared.types.SchemaVersion;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Service for layer management (CRUD operations).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WLayerService {

    public static final String STORAGE_SCHEMA = "WLayerTerrainStorage";
    public static final SchemaVersion STORAGE_SCHEMA_VERSION = SchemaVersion.create("1.0.1");

    private final WLayerRepository layerRepository;
    private final WLayerTerrainRepository terrainRepository;
    private final WLayerModelRepository modelRepository;
    private final WDirtyChunkService dirtyChunkService;
    private final StorageService storageService;
    private final ObjectMapper objectMapper;

    // ==================== LAYER CRUD ====================

    /**
     * Create a new layer.
     *
     * @param worldId         World identifier
     * @param name            Layer name (unique per world)
     * @param layerType       Layer type (TERRAIN or MODEL)
     * @param order           Overlay order
     * @param allChunks       True if affects all chunks
     * @param affectedChunks  List of affected chunks (if allChunks is false)
     * @return Created layer
     */
    @Transactional
    public WLayer createLayer(String worldId, String name, LayerType layerType,
                              int order, boolean allChunks, List<String> affectedChunks) {
        // Validate
        if (worldId == null || worldId.isBlank()) {
            throw new IllegalArgumentException("worldId is required");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name is required");
        }
        if (layerType == null) {
            throw new IllegalArgumentException("layerType is required");
        }

        // Check for duplicate name
        if (layerRepository.findByWorldIdAndName(worldId, name).isPresent()) {
            throw new IllegalArgumentException("Layer with name '" + name + "' already exists in world " + worldId);
        }

        // Generate layerDataId
        String layerDataId = UUID.randomUUID().toString();

        // Create layer
        WLayer layer = WLayer.builder()
                .worldId(worldId)
                .name(name)
                .layerType(layerType)
                .layerDataId(layerDataId)
                .order(order)
                .allChunks(allChunks)
                .affectedChunks(affectedChunks != null ? affectedChunks : List.of())
                .enabled(true)
                .build();
        layer.touchCreate();

        WLayer saved = layerRepository.save(layer);
        log.info("Created layer: world={} name={} type={} order={}", worldId, name, layerType, order);

        return saved;
    }

    /**
     * Update a layer using a Consumer.
     *
     * @param worldId   World identifier
     * @param layerName Layer name
     * @param updater   Consumer to modify the layer
     * @return Updated layer
     */
    @Transactional
    public Optional<WLayer> updateLayer(String worldId, String layerName, Consumer<WLayer> updater) {
        Optional<WLayer> layerOpt = layerRepository.findByWorldIdAndName(worldId, layerName);
        if (layerOpt.isEmpty()) {
            return Optional.empty();
        }

        WLayer layer = layerOpt.get();
        updater.accept(layer);
        layer.touchUpdate();

        WLayer saved = layerRepository.save(layer);
        log.info("Updated layer: world={} name={}", worldId, layerName);

        // Mark affected chunks as dirty
        markAffectedChunksDirty(saved, "layer_updated");

        return Optional.of(saved);
    }

    /**
     * Delete a layer and its associated data.
     *
     * @param worldId   World identifier
     * @param layerName Layer name
     * @return True if deleted
     */
    @Transactional
    public boolean deleteLayer(String worldId, String layerName) {
        Optional<WLayer> layerOpt = layerRepository.findByWorldIdAndName(worldId, layerName);
        if (layerOpt.isEmpty()) {
            return false;
        }

        WLayer layer = layerOpt.get();

        // Mark affected chunks as dirty before deletion
        markAffectedChunksDirty(layer, "layer_deleted");

        // Delete associated data
        if (layer.getLayerType() == LayerType.TERRAIN) {
            deleteTerrainData(layer.getLayerDataId());
        } else if (layer.getLayerType() == LayerType.MODEL) {
            modelRepository.deleteByLayerDataId(layer.getLayerDataId());
        }

        // Delete layer
        layerRepository.delete(layer);
        log.info("Deleted layer: world={} name={} type={}", worldId, layerName, layer.getLayerType());

        return true;
    }

    /**
     * Find a layer by ID.
     */
    @Transactional(readOnly = true)
    public Optional<WLayer> findById(String id) {
        return layerRepository.findById(id);
    }

    /**
     * Find a layer by world and name.
     */
    @Transactional(readOnly = true)
    public Optional<WLayer> findLayer(String worldId, String layerName) {
        return layerRepository.findByWorldIdAndName(worldId, layerName);
    }

    /**
     * Find a layer by world and name (alias for REST API).
     */
    @Transactional(readOnly = true)
    public Optional<WLayer> findByWorldIdAndName(String worldId, String layerName) {
        return layerRepository.findByWorldIdAndName(worldId, layerName);
    }

    /**
     * Find all layers for a world.
     */
    @Transactional(readOnly = true)
    public List<WLayer> findLayersByWorld(String worldId) {
        return layerRepository.findByWorldIdOrderByOrderAsc(worldId);
    }

    /**
     * Find all layers for a world (alias for REST API).
     */
    @Transactional(readOnly = true)
    public List<WLayer> findByWorldId(String worldId) {
        return layerRepository.findByWorldIdOrderByOrderAsc(worldId);
    }

    /**
     * Save a layer.
     */
    @Transactional
    public WLayer save(WLayer layer) {
        return layerRepository.save(layer);
    }

    /**
     * Delete a layer by ID.
     */
    @Transactional
    public void delete(String id) {
        Optional<WLayer> layerOpt = layerRepository.findById(id);
        if (layerOpt.isPresent()) {
            WLayer layer = layerOpt.get();
            // Mark affected chunks as dirty before deletion
            markAffectedChunksDirty(layer, "layer_deleted");

            // Delete associated data
            if (layer.getLayerType() == LayerType.TERRAIN) {
                deleteTerrainData(layer.getLayerDataId());
            } else if (layer.getLayerType() == LayerType.MODEL) {
                modelRepository.deleteByLayerDataId(layer.getLayerDataId());
            }

            // Delete layer
            layerRepository.deleteById(id);
            log.info("Deleted layer: id={} name={} type={}", id, layer.getName(), layer.getLayerType());
        }
    }

    /**
     * Get layers affecting a specific chunk (sorted by order).
     *
     * @param worldId  World identifier
     * @param chunkKey Chunk key
     * @return List of layers sorted by order
     */
    @Transactional(readOnly = true)
    public List<WLayer> getLayersAffectingChunk(String worldId, String chunkKey) {
        return layerRepository.findLayersAffectingChunk(worldId, chunkKey)
                .stream()
                .sorted(Comparator.comparingInt(WLayer::getOrder))
                .collect(Collectors.toList());
    }

    // ==================== TERRAIN LAYER OPERATIONS ====================

    /**
     * Save terrain chunk data.
     *
     * @param worldId     World identifier
     * @param layerDataId Layer data ID
     * @param chunkKey    Chunk key
     * @param data        Layer chunk data
     * @return Saved terrain entity
     */
    @Transactional
    public WLayerTerrain saveTerrainChunk(String worldId, String layerDataId,
                                          String chunkKey, LayerChunkData data) {
        if (data == null) {
            throw new IllegalArgumentException("LayerChunkData is required");
        }

        // Serialize to JSON
        String json;
        try {
            json = objectMapper.writeValueAsString(data);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize ChunkData", e);
        }

        // Find or create entity
        WLayerTerrain entity = terrainRepository
                .findByLayerDataIdAndChunkKey(layerDataId, chunkKey)
                .orElseGet(() -> {
                    WLayerTerrain newEntity = WLayerTerrain.builder()
                            .worldId(worldId)
                            .layerDataId(layerDataId)
                            .chunkKey(chunkKey)
                            .build();
                    newEntity.touchCreate();
                    return newEntity;
                });

        // Store via StorageService
        try (InputStream stream = new ByteArrayInputStream(json.getBytes(java.nio.charset.StandardCharsets.UTF_8))) {
            StorageService.StorageInfo storageInfo;
            if (entity.getStorageId() != null) {
                storageInfo = storageService.update(STORAGE_SCHEMA, STORAGE_SCHEMA_VERSION, entity.getStorageId(), stream);
            } else {
                storageInfo = storageService.store(STORAGE_SCHEMA, STORAGE_SCHEMA_VERSION, worldId, "layer/terrain/" + layerDataId + "/" + chunkKey, stream);
            }
            entity.setStorageId(storageInfo.id());
            log.debug("Terrain chunk stored: layerDataId={} chunkKey={} storageId={} size={}",
                    layerDataId, chunkKey, storageInfo.id(), storageInfo.size());
        } catch (Exception e) {
            throw new IllegalStateException("Failed to store terrain chunk", e);
        }

        entity.touchUpdate();
        WLayerTerrain saved = terrainRepository.save(entity);

        // Mark chunk as dirty
        dirtyChunkService.markChunkDirty(worldId, chunkKey, "terrain_layer_updated");

        return saved;
    }

    /**
     * Load terrain chunk data.
     *
     * @param layerDataId Layer data ID
     * @param chunkKey    Chunk key
     * @return Layer chunk data if found
     */
    @Transactional(readOnly = true)
    public Optional<LayerChunkData> loadTerrainChunk(String layerDataId, String chunkKey) {
        Optional<WLayerTerrain> terrainOpt = terrainRepository
                .findByLayerDataIdAndChunkKey(layerDataId, chunkKey);

        if (terrainOpt.isEmpty()) {
            return Optional.empty();
        }

        WLayerTerrain terrain = terrainOpt.get();
        if (terrain.getStorageId() == null) {
            log.warn("Terrain chunk has no storageId: layerDataId={} chunkKey={}", layerDataId, chunkKey);
            return Optional.empty();
        }

        // Load from storage
        try (InputStream stream = storageService.load(terrain.getStorageId())) {
            if (stream == null) {
                return Optional.empty();
            }
            LayerChunkData chunkData = objectMapper.readValue(stream, LayerChunkData.class);
            return Optional.of(chunkData);
        } catch (Exception e) {
            log.error("Failed to load terrain chunk: layerDataId={} chunkKey={}", layerDataId, chunkKey, e);
            return Optional.empty();
        }
    }

    /**
     * Delete terrain chunk.
     */
    @Transactional
    public boolean deleteTerrainChunk(String layerDataId, String chunkKey) {
        Optional<WLayerTerrain> terrainOpt = terrainRepository
                .findByLayerDataIdAndChunkKey(layerDataId, chunkKey);

        if (terrainOpt.isEmpty()) {
            return false;
        }

        WLayerTerrain terrain = terrainOpt.get();

        // Delete from storage
        if (terrain.getStorageId() != null) {
            try {
                storageService.delete(terrain.getStorageId());
            } catch (Exception e) {
                log.warn("Failed to delete terrain chunk from storage: storageId={}", terrain.getStorageId(), e);
            }
        }

        // Delete entity
        terrainRepository.delete(terrain);

        // Mark chunk as dirty
        dirtyChunkService.markChunkDirty(terrain.getWorldId(), chunkKey, "terrain_chunk_deleted");

        log.debug("Deleted terrain chunk: layerDataId={} chunkKey={}", layerDataId, chunkKey);
        return true;
    }

    /**
     * Delete all terrain data for a layer.
     */
    private void deleteTerrainData(String layerDataId) {
        List<WLayerTerrain> terrains = terrainRepository.findByLayerDataId(layerDataId);
        for (WLayerTerrain terrain : terrains) {
            if (terrain.getStorageId() != null) {
                try {
                    storageService.delete(terrain.getStorageId());
                } catch (Exception e) {
                    log.warn("Failed to delete terrain storage: storageId={}", terrain.getStorageId(), e);
                }
            }
        }
        terrainRepository.deleteByLayerDataId(layerDataId);
        log.debug("Deleted terrain data: layerDataId={} count={}", layerDataId, terrains.size());
    }

    // ==================== MODEL LAYER OPERATIONS ====================

    /**
     * Save model layer content.
     *
     * @param worldId     World identifier
     * @param layerDataId Layer data ID
     * @param content     List of layer blocks with relative positions
     * @return Saved model entity
     */
    @Transactional
    public WLayerModel saveModel(String worldId, String layerDataId, List<LayerBlock> content) {
        if (content == null) {
            throw new IllegalArgumentException("Content is required");
        }

        // Find or create entity
        WLayerModel entity = modelRepository.findByLayerDataId(layerDataId)
                .orElseGet(() -> {
                    WLayerModel newEntity = WLayerModel.builder()
                            .worldId(worldId)
                            .layerDataId(layerDataId)
                            .build();
                    newEntity.touchCreate();
                    return newEntity;
                });

        entity.setContent(content);
        entity.touchUpdate();

        WLayerModel saved = modelRepository.save(entity);
        log.info("Saved model: layerDataId={} blocks={}", layerDataId, content.size());

        // TODO: Calculate affected chunks from mount point + content bounds
        // For now, rely on WLayer.affectedChunks

        return saved;
    }

    /**
     * Load model layer content.
     */
    @Transactional(readOnly = true)
    public Optional<WLayerModel> loadModel(String layerDataId) {
        return modelRepository.findByLayerDataId(layerDataId);
    }

    /**
     * Delete model layer.
     */
    @Transactional
    public boolean deleteModel(String layerDataId) {
        Optional<WLayerModel> modelOpt = modelRepository.findByLayerDataId(layerDataId);
        if (modelOpt.isEmpty()) {
            return false;
        }

        modelRepository.deleteByLayerDataId(layerDataId);
        log.debug("Deleted model: layerDataId={}", layerDataId);
        return true;
    }

    // ==================== HELPER METHODS ====================

    /**
     * Mark affected chunks as dirty when layer changes.
     */
    private void markAffectedChunksDirty(WLayer layer, String reason) {
        if (layer.isAllChunks()) {
            // All chunks affected - warn about performance
            log.warn("Layer affects all chunks, consider full world regeneration: layer={} world={}",
                    layer.getName(), layer.getWorldId());
            // TODO: Implement strategy for marking all chunks dirty
            // Option 1: Mark all existing WChunks dirty
            // Option 2: Set flag in WWorld for full regeneration
        } else {
            dirtyChunkService.markChunksDirty(layer.getWorldId(), layer.getAffectedChunks(), reason);
        }
    }
}
