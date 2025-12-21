package de.mhus.nimbus.world.control.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.mhus.nimbus.shared.storage.StorageService;
import de.mhus.nimbus.world.shared.layer.*;
import de.mhus.nimbus.world.shared.world.WWorld;
import de.mhus.nimbus.world.shared.world.WWorldRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.InputStream;
import java.util.*;

/**
 * REST controller for block grid visualization.
 * Provides endpoints to load block coordinates for BlockGridEditor.
 */
@RestController
@RequestMapping("/control/worlds/{worldId}/layers")
@RequiredArgsConstructor
@Slf4j
public class ELayerBlockGridController {

    private final WLayerRepository layerRepository;
    private final WLayerTerrainRepository terrainRepository;
    private final WLayerModelRepository modelRepository;
    private final WWorldRepository worldRepository;
    private final StorageService storageService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Get all block coordinates from WLayerTerrain chunks.
     * Returns only coordinates and optional color for each block.
     *
     * @param worldId World ID
     * @param layerId Layer ID
     * @return List of block coordinates with optional color
     */
    @GetMapping("/{layerId}/terrain/blocks")
    public ResponseEntity<?> getTerrainBlocks(
            @PathVariable String worldId,
            @PathVariable String layerId
    ) {
        log.debug("Loading terrain blocks for worldId={}, layerId={}", worldId, layerId);

        // Load layer
        Optional<WLayer> layerOpt = layerRepository.findById(layerId);
        if (layerOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        WLayer layer = layerOpt.get();
        if (layer.getLayerType() != LayerType.GROUND) {
            return ResponseEntity.badRequest().body(Map.of("error", "Layer is not TERRAIN type"));
        }

        // Load world to get chunkSize
        Optional<WWorld> worldOpt = worldRepository.findByWorldId(worldId);
        if (worldOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        int chunkSize = worldOpt.get().getPublicData().getChunkSize();

        // Load all terrain chunks for this layer
        List<WLayerTerrain> terrainChunks = terrainRepository.findByLayerDataId(layer.getLayerDataId());
        log.debug("Found {} terrain chunks for layerDataId={}", terrainChunks.size(), layer.getLayerDataId());

        // Collect all block coordinates
        List<Map<String, Object>> blockCoordinates = new ArrayList<>();

        for (WLayerTerrain terrain : terrainChunks) {
            if (terrain.getStorageId() == null) {
                continue;
            }

            try {
                // Load chunk data from storage
                InputStream stream = storageService.load(terrain.getStorageId());
                if (stream == null) {
                    log.warn("Chunk data not found for storageId={}", terrain.getStorageId());
                    continue;
                }

                LayerChunkData chunkData = objectMapper.readValue(stream, LayerChunkData.class);

                // Parse chunk key to get chunk coordinates
                String[] parts = terrain.getChunkKey().split(":");
                if (parts.length != 2) {
                    log.warn("Invalid chunk key format: {}", terrain.getChunkKey());
                    continue;
                }

                int chunkX = Integer.parseInt(parts[0]);
                int chunkZ = Integer.parseInt(parts[1]);

                // Calculate world offset for this chunk
                int offsetX = chunkX * chunkSize;
                int offsetZ = chunkZ * chunkSize;

                // Extract blocks from chunk data
                if (chunkData.getBlocks() != null) {
                    for (LayerBlock layerBlock : chunkData.getBlocks()) {
                        if (layerBlock.getBlock() == null) {
                            continue;
                        }

                        var position = layerBlock.getBlock().getPosition();
                        if (position == null) {
                            continue;
                        }

                        // Convert relative position to world position
                        int worldX = offsetX + (int) position.getX();
                        int worldY = (int) position.getY();
                        int worldZ = offsetZ + (int) position.getZ();

                        Map<String, Object> coord = new HashMap<>();
                        coord.put("x", worldX);
                        coord.put("y", worldY);
                        coord.put("z", worldZ);

                        // Optional: Add color based on group
                        if (layerBlock.getGroup() > 0) {
                            coord.put("color", getGroupColor(layerBlock.getGroup()));
                        }

                        blockCoordinates.add(coord);
                    }
                }
            } catch (Exception e) {
                log.error("Failed to load chunk data for storageId={}", terrain.getStorageId(), e);
            }
        }

        log.debug("Returning {} block coordinates", blockCoordinates.size());

        return ResponseEntity.ok(Map.of(
                "blocks", blockCoordinates,
                "count", blockCoordinates.size()
        ));
    }

    /**
     * Get detailed block information from WLayerTerrain.
     *
     * @param worldId World ID
     * @param layerId Layer ID
     * @param x Block X coordinate
     * @param y Block Y coordinate
     * @param z Block Z coordinate
     * @return Block details including LayerBlock wrapper
     */
    @GetMapping("/{layerId}/terrain/block/{x}/{y}/{z}")
    public ResponseEntity<?> getTerrainBlockDetails(
            @PathVariable String worldId,
            @PathVariable String layerId,
            @PathVariable int x,
            @PathVariable int y,
            @PathVariable int z
    ) {
        log.debug("Loading terrain block details for worldId={}, layerId={}, pos=({},{},{})",
                worldId, layerId, x, y, z);

        // Load layer
        Optional<WLayer> layerOpt = layerRepository.findById(layerId);
        if (layerOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        WLayer layer = layerOpt.get();
        if (layer.getLayerType() != LayerType.GROUND) {
            return ResponseEntity.badRequest().body(Map.of("error", "Layer is not TERRAIN type"));
        }

        // Load world to get chunkSize
        Optional<WWorld> worldOpt = worldRepository.findByWorldId(worldId);
        if (worldOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        int chunkSize = worldOpt.get().getPublicData().getChunkSize();

        // Calculate chunk coordinates
        int chunkX = Math.floorDiv(x, chunkSize);
        int chunkZ = Math.floorDiv(z, chunkSize);
        String chunkKey = chunkX + ":" + chunkZ;

        // Load terrain chunk
        Optional<WLayerTerrain> terrainOpt = terrainRepository.findByLayerDataIdAndChunkKey(
                layer.getLayerDataId(), chunkKey);

        if (terrainOpt.isEmpty() || terrainOpt.get().getStorageId() == null) {
            return ResponseEntity.notFound().build();
        }

        WLayerTerrain terrain = terrainOpt.get();

        try {
            // Load chunk data from storage
            InputStream stream = storageService.load(terrain.getStorageId());
            if (stream == null) {
                return ResponseEntity.notFound().build();
            }

            LayerChunkData chunkData = objectMapper.readValue(stream, LayerChunkData.class);

            // Find block at specified position (relative to chunk)
            int relativeX = x - (chunkX * chunkSize);
            int relativeZ = z - (chunkZ * chunkSize);

            if (chunkData.getBlocks() != null) {
                for (LayerBlock layerBlock : chunkData.getBlocks()) {
                    if (layerBlock.getBlock() == null || layerBlock.getBlock().getPosition() == null) {
                        continue;
                    }

                    var pos = layerBlock.getBlock().getPosition();
                    if ((int) pos.getX() == relativeX && (int) pos.getY() == y && (int) pos.getZ() == relativeZ) {
                        // Return LayerBlock wrapper with block and metadata
                        return ResponseEntity.ok(Map.of(
                                "block", layerBlock.getBlock(),
                                "group", layerBlock.getGroup(),
                                "weight", layerBlock.getWeight() != null ? layerBlock.getWeight() : 0,
                                "override", layerBlock.isOverride(),
                                "metadata", layerBlock.getMetadata() != null ? layerBlock.getMetadata() : ""
                        ));
                    }
                }
            }

            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Failed to load block details", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get all block coordinates from WLayerModel.
     * Returns only coordinates and optional color for each block.
     *
     * @param worldId World ID
     * @param layerId Layer ID
     * @param modelId Model ID
     * @return List of block coordinates with optional color
     */
    @GetMapping("/{layerId}/models/{modelId}/blocks")
    public ResponseEntity<?> getModelBlocks(
            @PathVariable String worldId,
            @PathVariable String layerId,
            @PathVariable String modelId
    ) {
        log.debug("Loading model blocks for worldId={}, layerId={}, modelId={}",
                worldId, layerId, modelId);

        // Load layer
        Optional<WLayer> layerOpt = layerRepository.findById(layerId);
        if (layerOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        WLayer layer = layerOpt.get();
        if (layer.getLayerType() != LayerType.MODEL) {
            return ResponseEntity.badRequest().body(Map.of("error", "Layer is not MODEL type"));
        }

        // Load model
        Optional<WLayerModel> modelOpt = modelRepository.findById(modelId);
        if (modelOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        WLayerModel model = modelOpt.get();

        // Collect all block coordinates (relative positions from model)
        List<Map<String, Object>> blockCoordinates = new ArrayList<>();

        if (model.getContent() != null) {
            for (LayerBlock layerBlock : model.getContent()) {
                if (layerBlock.getBlock() == null || layerBlock.getBlock().getPosition() == null) {
                    continue;
                }

                var position = layerBlock.getBlock().getPosition();

                Map<String, Object> coord = new HashMap<>();
                coord.put("x", (int) position.getX());
                coord.put("y", (int) position.getY());
                coord.put("z", (int) position.getZ());

                // Optional: Add color based on group
                if (layerBlock.getGroup() > 0) {
                    coord.put("color", getGroupColor(layerBlock.getGroup()));
                }

                blockCoordinates.add(coord);
            }
        }

        log.debug("Returning {} block coordinates from model", blockCoordinates.size());

        return ResponseEntity.ok(Map.of(
                "blocks", blockCoordinates,
                "count", blockCoordinates.size(),
                "mountPoint", Map.of(
                        "x", model.getMountX(),
                        "y", model.getMountY(),
                        "z", model.getMountZ()
                ),
                "rotation", model.getRotation()
        ));
    }

    /**
     * Get detailed block information from WLayerModel.
     *
     * @param worldId World ID
     * @param layerId Layer ID
     * @param modelId Model ID
     * @param x Block X coordinate (relative to mount point)
     * @param y Block Y coordinate
     * @param z Block Z coordinate (relative to mount point)
     * @return Block details including LayerBlock wrapper
     */
    @GetMapping("/{layerId}/models/{modelId}/block/{x}/{y}/{z}")
    public ResponseEntity<?> getModelBlockDetails(
            @PathVariable String worldId,
            @PathVariable String layerId,
            @PathVariable String modelId,
            @PathVariable int x,
            @PathVariable int y,
            @PathVariable int z
    ) {
        log.debug("Loading model block details for worldId={}, layerId={}, modelId={}, pos=({},{},{})",
                worldId, layerId, modelId, x, y, z);

        // Load layer
        Optional<WLayer> layerOpt = layerRepository.findById(layerId);
        if (layerOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        WLayer layer = layerOpt.get();
        if (layer.getLayerType() != LayerType.MODEL) {
            return ResponseEntity.badRequest().body(Map.of("error", "Layer is not MODEL type"));
        }

        // Load model
        Optional<WLayerModel> modelOpt = modelRepository.findById(modelId);
        if (modelOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        WLayerModel model = modelOpt.get();

        // Find block at specified relative position
        if (model.getContent() != null) {
            for (LayerBlock layerBlock : model.getContent()) {
                if (layerBlock.getBlock() == null || layerBlock.getBlock().getPosition() == null) {
                    continue;
                }

                var pos = layerBlock.getBlock().getPosition();
                if ((int) pos.getX() == x && (int) pos.getY() == y && (int) pos.getZ() == z) {
                    // Return LayerBlock wrapper with block and metadata
                    return ResponseEntity.ok(Map.of(
                            "block", layerBlock.getBlock(),
                            "group", layerBlock.getGroup(),
                            "weight", layerBlock.getWeight() != null ? layerBlock.getWeight() : 0,
                            "override", layerBlock.isOverride(),
                            "metadata", layerBlock.getMetadata() != null ? layerBlock.getMetadata() : ""
                    ));
                }
            }
        }

        return ResponseEntity.notFound().build();
    }

    /**
     * Get color for group ID (simple mapping for visualization).
     */
    private String getGroupColor(int groupId) {
        // Simple color mapping based on group ID
        String[] colors = {
                "#3b82f6", // blue
                "#ef4444", // red
                "#10b981", // green
                "#f59e0b", // amber
                "#8b5cf6", // purple
                "#ec4899", // pink
                "#06b6d4", // cyan
                "#f97316"  // orange
        };
        return colors[groupId % colors.length];
    }
}
