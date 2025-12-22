package de.mhus.nimbus.tools.demoimport.importers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import de.mhus.nimbus.generated.types.Block;
import de.mhus.nimbus.generated.types.ChunkData;
import de.mhus.nimbus.tools.demoimport.ImportStats;
import de.mhus.nimbus.world.shared.layer.*;
import de.mhus.nimbus.world.shared.world.WChunkService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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
    private final WLayerService layerService;
    private final WDirtyChunkService dirtyChunkService;
    private final ObjectMapper objectMapper;

    @Value("${import.data-path:../../client/packages/test_server/data}")
    private String dataPath;

    @Value("${import.world-id:main}")
    private String worldId;

    @Value("${import.mark-chunks-dirty:true}")
    private boolean markChunksDirty;

    private static final String GROUND_LAYER_NAME = "ground";
    private static final int GROUND_LAYER_ORDER = 10;

    public ImportStats importAll() throws Exception {
        log.info("Starting Chunk import from: {}/worlds/{}/chunks/", dataPath, worldId);

        ImportStats stats = new ImportStats();
        Path chunksDir = Path.of(dataPath, "worlds", worldId, "chunks");

        if (!Files.exists(chunksDir) || !Files.isDirectory(chunksDir)) {
            log.warn("Chunks directory not found: {}", chunksDir);
            return stats;
        }

        // Ensure 'ground' layer exists
        WLayer groundLayer = ensureGroundLayer();

        // Read all chunk_*.json files
        try (Stream<Path> files = Files.list(chunksDir)) {
            files.filter(path -> path.getFileName().toString().matches("chunk_-?\\d+_-?\\d+\\.json"))
                    .sorted()
                    .forEach(chunkPath -> {
                        try {
                            importChunk(chunkPath.toFile(), groundLayer, stats);
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

    /**
     * Ensure 'ground' layer exists.
     * Creates if not exists: Layer 'ground', order 10, type TerrainLayer, allChunks=true.
     */
    private WLayer ensureGroundLayer() {
        Optional<WLayer> existingLayer = layerService.findLayer(worldId, GROUND_LAYER_NAME);

        if (existingLayer.isPresent()) {
            log.info("Ground layer already exists for world: {}", worldId);
            return existingLayer.get();
        }

        // Create ground layer
        WLayer groundLayer = layerService.createLayer(
                worldId,
                GROUND_LAYER_NAME,
                LayerType.TERRAIN,
                GROUND_LAYER_ORDER,
                true,  // allChunks
                List.of(),  // no specific chunks
                false  // baseGround
        );

        log.info("Created ground layer for world: {} (order: {})", worldId, GROUND_LAYER_ORDER);
        return groundLayer;
    }

    private void importChunk(File chunkFile, WLayer groundLayer, ImportStats stats) throws Exception {
        // Read as JsonNode first for transformation
        JsonNode chunkNode = objectMapper.readTree(chunkFile);

        // Transform faceVisibility from {"value": 124} to 124
        transformFaceVisibility(chunkNode);

        // Now deserialize to ChunkData
        ChunkData chunkData = objectMapper.treeToValue(chunkNode, ChunkData.class);

        // Create chunkKey (cx:cz format)
        String chunkKey = chunkData.getCx() + ":" + chunkData.getCz();

        boolean chunkExists = chunkService.loadChunkData(worldId, chunkKey, false).isPresent();
        boolean layerExists = layerService.loadTerrainChunk(groundLayer.getLayerDataId(), chunkKey).isPresent();

        // Check if both already exist
        if (chunkExists && layerExists) {
            log.trace("Chunk and layer already exist: {} - skipping", chunkKey);
            stats.incrementSkipped();
            return;
        }

        // Save chunk if it doesn't exist
        if (!chunkExists) {
            chunkService.saveChunk(worldId, worldId, chunkKey, chunkData);
            log.debug("Imported chunk: {} ({} blocks)", chunkKey,
                    chunkData.getBlocks() != null ? chunkData.getBlocks().size() : 0);
        } else {
            log.debug("Chunk already exists: {} - updating layer only", chunkKey);
        }

        // Create/Update LayerTerrain entity if it doesn't exist
        if (!layerExists) {
            createLayerTerrainChunk(groundLayer, chunkKey, chunkData);
            log.debug("Created layer terrain for existing chunk: {}", chunkKey);

            // Mark chunk as dirty for regeneration if configured
            if (markChunksDirty) {
                dirtyChunkService.markChunkDirty(worldId, chunkKey, "terrain_layer_imported");
                log.trace("Marked chunk dirty for regeneration: {}", chunkKey);
            }
        }

        stats.incrementSuccess();
    }

    /**
     * Create LayerTerrain entity for the ground layer with LayerChunkData.
     * Converts ChunkData blocks to LayerBlocks.
     */
    private void createLayerTerrainChunk(WLayer groundLayer, String chunkKey, ChunkData chunkData) {
        try {
            // Convert ChunkData to LayerChunkData
            LayerChunkData layerChunkData = new LayerChunkData();
            layerChunkData.setCx(chunkData.getCx());
            layerChunkData.setCz(chunkData.getCz());

            // Convert blocks to LayerBlocks
            if (chunkData.getBlocks() != null) {
                List<LayerBlock> layerBlocks = new ArrayList<>();
                for (Block block : chunkData.getBlocks()) {
                    LayerBlock layerBlock = LayerBlock.builder()
                            .block(block)
                            .override(true)  // default override
                            .group(0)        // default group
                            .build();
                    layerBlocks.add(layerBlock);
                }
                layerChunkData.setBlocks(layerBlocks);
            }

            // Copy height data
            if (chunkData.getHeightData() != null) {
                layerChunkData.setHeightData(chunkData.getHeightData());
            }

            // Save to layer terrain
            layerService.saveTerrainChunk(worldId, groundLayer.getLayerDataId(), chunkKey, layerChunkData);

            log.trace("Created LayerTerrain for chunk: {} ({} blocks)",
                    chunkKey, layerChunkData.getBlocks().size());

        } catch (Exception e) {
            log.error("Failed to create LayerTerrain for chunk: {}", chunkKey, e);
            // Don't fail the import, just log the error
        }
    }

    /**
     * Transform faceVisibility from old format {"value": 124} to new format 124.
     * Only transforms if field exists and is in old format.
     * Leaves blocks without faceVisibility unchanged (field stays absent).
     */
    private void transformFaceVisibility(JsonNode chunkNode) {
        JsonNode blocksNode = chunkNode.get("blocks");
        if (blocksNode == null || !blocksNode.isArray()) {
            return;
        }

        int transformCount = 0;
        for (JsonNode blockNode : blocksNode) {
            if (!blockNode.isObject()) continue;

            ObjectNode block = (ObjectNode) blockNode;

            // Only transform if faceVisibility field exists
            if (!block.has("faceVisibility")) {
                continue; // Field not present, skip this block
            }

            JsonNode faceVisNode = block.get("faceVisibility");

            // Check if it's in old format: {"value": X}
            if (faceVisNode.isObject() && faceVisNode.has("value")) {
                JsonNode valueNode = faceVisNode.get("value");
                if (valueNode.isNumber()) {
                    int value = valueNode.asInt();
                    // Replace object with direct int value
                    block.put("faceVisibility", value);
                    transformCount++;
                }
            }
            // If it's already a number, leave it as is
            // If it doesn't exist, we already skipped above
        }

        if (transformCount > 0) {
            log.trace("Transformed {} faceVisibility fields in chunk", transformCount);
        }
    }
}
