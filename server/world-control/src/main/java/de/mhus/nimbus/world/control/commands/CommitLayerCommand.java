package de.mhus.nimbus.world.control.commands;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.mhus.nimbus.world.control.service.ChunkUpdateService;
import de.mhus.nimbus.world.control.service.EditService;
import de.mhus.nimbus.world.control.service.EditState;
import de.mhus.nimbus.world.shared.commands.Command;
import de.mhus.nimbus.world.shared.commands.Command.CommandResult;
import de.mhus.nimbus.world.shared.commands.CommandContext;
import de.mhus.nimbus.world.shared.layer.*;
import de.mhus.nimbus.world.shared.redis.WorldRedisService;
import de.mhus.nimbus.generated.types.Block;
import de.mhus.nimbus.world.shared.world.BlockUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * CommitLayer command - commits overlay edits to layer storage.
 * Reads overlays from Redis and saves them to the selected layer.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CommitLayerCommand implements Command {

    private final EditService editService;
    private final WLayerService layerService;
    private final WLayerModelRepository modelRepository;
    private final WorldRedisService redisService;
    private final ChunkUpdateService chunkUpdateService;
    private final ObjectMapper objectMapper;

    @Override
    public String getName() {
        return "CommitLayer";
    }

    @Override
    public CommandResult execute(CommandContext context, List<String> args) {
        String worldId = context.getWorldId();
        String sessionId = context.getSessionId();

        if (sessionId == null || sessionId.isBlank()) {
            return CommandResult.error(-2, "Session ID required");
        }

        // Get edit state to know which layer is being edited
        EditState state = editService.getEditState(worldId, sessionId);

        if (state.getSelectedLayer() == null) {
            return CommandResult.error(-1, "No layer selected. Cannot commit without layer selection.");
        }

        if (!state.isEditMode()) {
            log.warn("Edit mode not active for session {}, proceeding anyway for auto-save", sessionId);
        }

        try {
            // 1. Scan Redis for all overlay keys
            Set<String> overlayKeys = redisService.getOverlayKeys(worldId, sessionId);

            if (overlayKeys.isEmpty()) {
                log.info("No overlays to commit for session {}", sessionId);
                return CommandResult.success("No overlays to commit");
            }

            log.info("Found {} overlay keys to commit for session {}", overlayKeys.size(), sessionId);

            // 2. Group by chunk: Extract chunk coordinates from key pattern
            //    Key format: world:{worldId}:overlay:{sessionId}:{cx}:{cz}
            Map<String, List<String>> chunkToKeys = groupOverlaysByChunk(overlayKeys, worldId);

            log.info("Grouped overlays into {} chunks", chunkToKeys.size());

            // 3. Get layer
            Optional<WLayer> layerOpt = layerService.findLayer(worldId, state.getSelectedLayer());
            if (layerOpt.isEmpty()) {
                return CommandResult.error(-1, "Layer not found: " + state.getSelectedLayer());
            }

            WLayer layer = layerOpt.get();
            String layerDataId = layer.getLayerDataId();

            // 3a. For MODEL layers: validate model selection
            if (layer.getLayerType() == LayerType.MODEL) {
                log.info("Layer is MODEL type, checking model selection...");

                if (state.getSelectedModelId() == null) {
                    log.warn("No model selected for MODEL layer");
                    return CommandResult.error(-1, "No model selected. MODEL layers require a model selection.");
                }

                log.info("Selected model ID: {}", state.getSelectedModelId());

                // Validate model exists
                Optional<WLayerModel> modelOpt = modelRepository.findById(state.getSelectedModelId());
                if (modelOpt.isEmpty()) {
                    log.warn("Model not found: {}", state.getSelectedModelId());
                    return CommandResult.error(-1, "Selected model not found: " + state.getSelectedModelId());
                }

                WLayerModel model = modelOpt.get();
                log.info("Found model: id={}, layerDataId={}, mountPoint=({},{},{})",
                        model.getId(), model.getLayerDataId(), model.getMountX(), model.getMountY(), model.getMountZ());

                if (!model.getLayerDataId().equals(layerDataId)) {
                    log.warn("Model layerDataId mismatch: expected={}, actual={}", layerDataId, model.getLayerDataId());
                    return CommandResult.error(-1, "Model does not belong to selected layer");
                }

                log.info("Calling commitToModel with {} overlay keys", overlayKeys.size());
                // Commit to WLayerModel instead of WLayerTerrain
                return commitToModel(worldId, sessionId, model, overlayKeys, state.getSelectedGroup());
            }

            // 4. For each chunk: load, merge, save, mark dirty
            int committedChunks = 0;
            int errorChunks = 0;

            for (Map.Entry<String, List<String>> entry : chunkToKeys.entrySet()) {
                String chunkKey = entry.getKey();

                try {
                    // Parse chunk coordinates
                    String[] coords = chunkKey.split(":");
                    if (coords.length != 2) {
                        log.warn("Invalid chunk key format: {}", chunkKey);
                        errorChunks++;
                        continue;
                    }

                    int cx = Integer.parseInt(coords[0]);
                    int cz = Integer.parseInt(coords[1]);

                    // Load existing layer chunk or create new
                    LayerChunkData chunkData = layerService.loadTerrainChunk(layerDataId, chunkKey)
                            .orElse(new LayerChunkData());

                    // Ensure blocks list exists
                    if (chunkData.getBlocks() == null) {
                        chunkData.setBlocks(new ArrayList<>());
                    }

                    // Get all overlay blocks for this chunk
                    Map<Object, Object> overlays = redisService.getOverlayBlocks(worldId, sessionId, cx, cz);

                    if (overlays == null || overlays.isEmpty()) {
                        log.debug("No overlays found for chunk {}", chunkKey);
                        continue;
                    }

                    // Merge overlays into chunkData
                    int mergedBlocks = mergeOverlaysIntoChunkData(chunkData, overlays, state.getSelectedGroup());

                    log.debug("Merged {} overlay blocks into chunk {}", mergedBlocks, chunkKey);

                    // Save chunk
                    layerService.saveTerrainChunk(worldId, layerDataId, chunkKey, chunkData);

                    // Update chunk async (will check lock and either update immediately or mark dirty)
                    chunkUpdateService.updateChunkAsync(worldId, chunkKey, "layer_commit");

                    committedChunks++;

                } catch (Exception e) {
                    log.error("Failed to commit chunk {}: {}", chunkKey, e.getMessage(), e);
                    errorChunks++;
                }
            }

            // 5. Delete overlays from Redis
            long deleted = redisService.deleteAllOverlays(worldId, sessionId);

            log.info("Layer commit completed: committedChunks={}, errorChunks={}, deletedKeys={}",
                    committedChunks, errorChunks, deleted);

            // 6. Keep edit mode active (user can continue editing)

            String message = String.format("Committed %d chunks (errors: %d), deleted %d overlay keys",
                    committedChunks, errorChunks, deleted);

            return errorChunks > 0
                    ? CommandResult.error(-1, message)
                    : CommandResult.success(message);

        } catch (Exception e) {
            log.error("Layer commit failed for session {}: {}", sessionId, e.getMessage(), e);
            return CommandResult.error(-4, "Commit failed: " + e.getMessage());
        }
    }

    /**
     * Group overlay keys by chunk.
     * Key format: world:{worldId}:overlay:{sessionId}:{cx}:{cz}
     * Returns map: {cx}:{cz} -> list of keys
     */
    private Map<String, List<String>> groupOverlaysByChunk(Set<String> overlayKeys, String worldId) {
        Map<String, List<String>> chunkToKeys = new HashMap<>();

        String prefix = "world:" + worldId + ":overlay:";

        for (String key : overlayKeys) {
            if (!key.startsWith(prefix)) {
                log.warn("Unexpected key format: {}", key);
                continue;
            }

            // Extract: {sessionId}:{cx}:{cz}
            String suffix = key.substring(prefix.length());
            String[] parts = suffix.split(":");

            if (parts.length >= 3) {
                // parts[0] = sessionId, parts[1] = cx, parts[2] = cz
                String chunkKey = parts[1] + ":" + parts[2];
                chunkToKeys.computeIfAbsent(chunkKey, k -> new ArrayList<>()).add(key);
            } else {
                log.warn("Invalid key format (not enough parts): {}", key);
            }
        }

        return chunkToKeys;
    }

    /**
     * Merge overlays into chunk data.
     * Returns number of blocks merged.
     */
    private int mergeOverlaysIntoChunkData(
            LayerChunkData chunkData,
            Map<Object, Object> overlays,
            int group
    ) {
        // Build position index
        Map<String, LayerBlock> blockIndex = new HashMap<>();

        if (chunkData.getBlocks() != null) {
            for (LayerBlock lb : chunkData.getBlocks()) {
                if (lb.getBlock() != null && lb.getBlock().getPosition() != null) {
                    String key = positionKey(
                            (int) lb.getBlock().getPosition().getX(),
                            (int) lb.getBlock().getPosition().getY(),
                            (int) lb.getBlock().getPosition().getZ()
                    );
                    blockIndex.put(key, lb);
                }
            }
        }

        int mergedCount = 0;

        // Apply overlays
        for (Map.Entry<Object, Object> entry : overlays.entrySet()) {
            String posKey = (String) entry.getKey();
            String blockJson = (String) entry.getValue();

            try {
                Block block = objectMapper.readValue(blockJson, Block.class);

                if (BlockUtil.isAirType(block.getBlockTypeId())) {
                    // AIR = remove block
                    if (blockIndex.remove(posKey) != null) {
                        mergedCount++;
                        log.trace("Removed block at {}", posKey);
                    }
                } else {
                    // Add/replace block
                    LayerBlock layerBlock = LayerBlock.builder()
                            .block(block)
                            .group(group)
                            .weight(0)
                            .override(true)
                            .build();

                    blockIndex.put(posKey, layerBlock);
                    mergedCount++;
                    log.trace("Added/replaced block at {} with type {}", posKey, block.getBlockTypeId());
                }

            } catch (Exception e) {
                log.warn("Failed to parse overlay block at {}: {} - {}", posKey, blockJson, e.getMessage());
            }
        }

        // Update chunk data
        chunkData.setBlocks(new ArrayList<>(blockIndex.values()));

        return mergedCount;
    }

    /**
     * Commit overlays to WLayerModel (for MODEL type layers).
     * Merges overlay blocks into model.content as relative positions from mount point.
     */
    private CommandResult commitToModel(
            String worldId,
            String sessionId,
            WLayerModel model,
            Set<String> overlayKeys,
            int group
    ) {
        try {
            log.info("Committing overlays to MODEL layer model: modelId={}, mountPoint=({},{},{})",
                    model.getId(), model.getMountX(), model.getMountY(), model.getMountZ());

            // Get all overlay blocks (from all chunks)
            Map<String, Block> allOverlays = new HashMap<>();

            for (String overlayKey : overlayKeys) {
                // Extract cx and cz from key: world:{worldId}:overlay:{sessionId}:{cx}:{cz}
                String prefix = "world:" + worldId + ":overlay:";
                if (!overlayKey.startsWith(prefix)) continue;

                String suffix = overlayKey.substring(prefix.length());
                String[] parts = suffix.split(":");
                if (parts.length < 3) continue;

                int cx = Integer.parseInt(parts[1]);
                int cz = Integer.parseInt(parts[2]);

                // Get overlay blocks for this chunk
                Map<Object, Object> chunkOverlays = redisService.getOverlayBlocks(worldId, sessionId, cx, cz);
                if (chunkOverlays == null) continue;

                for (Map.Entry<Object, Object> entry : chunkOverlays.entrySet()) {
                    String posKey = (String) entry.getKey();
                    String blockJson = (String) entry.getValue();

                    try {
                        Block block = objectMapper.readValue(blockJson, Block.class);
                        allOverlays.put(posKey, block);
                    } catch (Exception e) {
                        log.warn("Failed to parse overlay block: {}", e.getMessage());
                    }
                }
            }

            log.info("Collected {} overlay blocks from Redis", allOverlays.size());

            // Build position index from existing model content
            List<LayerBlock> content = model.getContent() != null
                    ? new ArrayList<>(model.getContent())
                    : new ArrayList<>();

            Map<String, LayerBlock> contentIndex = new HashMap<>();
            for (LayerBlock lb : content) {
                if (lb.getBlock() != null && lb.getBlock().getPosition() != null) {
                    String key = positionKey(
                            (int) lb.getBlock().getPosition().getX(),
                            (int) lb.getBlock().getPosition().getY(),
                            (int) lb.getBlock().getPosition().getZ()
                    );
                    contentIndex.put(key, lb);
                }
            }

            int mergedCount = 0;

            // Apply overlays - convert world coordinates to relative positions
            for (Map.Entry<String, Block> entry : allOverlays.entrySet()) {
                String posKey = entry.getKey();
                Block block = entry.getValue();

                // Convert world position to relative position (from mount point)
                int worldX = (int) block.getPosition().getX();
                int worldY = (int) block.getPosition().getY();
                int worldZ = (int) block.getPosition().getZ();

                // Step 1: Translate to mount point
                int relX = worldX - model.getMountX();
                int relY = worldY - model.getMountY();
                int relZ = worldZ - model.getMountZ();

                // Step 2: Apply rotation (if any)
                int[] rotated = applyRotation(relX, relZ, model.getRotation());
                int finalX = rotated[0];
                int finalZ = rotated[1];
                int finalY = relY; // Y doesn't change with rotation

                String relPosKey = positionKey(finalX, finalY, finalZ);

                // Update block position to relative (with rotation applied)
                Block relBlock = BlockUtil.cloneBlock(block);
                relBlock.setPosition(de.mhus.nimbus.generated.types.Vector3.builder()
                        .x((double) finalX)
                        .y((double) finalY)
                        .z((double) finalZ)
                        .build());

                if (BlockUtil.isAirType(block.getBlockTypeId())) {
                    // AIR = remove block
                    if (contentIndex.remove(relPosKey) != null) {
                        mergedCount++;
                        log.trace("Removed block at relative position {}", relPosKey);
                    }
                } else {
                    // Add/replace block
                    LayerBlock layerBlock = LayerBlock.builder()
                            .block(relBlock)
                            .group(group)
                            .weight(0)
                            .override(true)
                            .build();

                    contentIndex.put(relPosKey, layerBlock);
                    mergedCount++;
                    log.trace("Added/replaced block at relative position {} with type {}", relPosKey, block.getBlockTypeId());
                }
            }

            // Update model content
            List<LayerBlock> newContent = new ArrayList<>(contentIndex.values());
            log.info("Updating model content: oldSize={}, newSize={}",
                    model.getContent() != null ? model.getContent().size() : 0,
                    newContent.size());

            model.setContent(newContent);
            model.touchUpdate();
            WLayerModel savedModel = modelRepository.save(model);

            log.info("Model saved: id={}, contentSize={}", savedModel.getId(),
                    savedModel.getContent() != null ? savedModel.getContent().size() : 0);

            // Delete overlays from Redis
            long deleted = redisService.deleteAllOverlays(worldId, sessionId);

            log.info("Model commit completed: modelId={}, mergedBlocks={}, deletedKeys={}, finalContentSize={}",
                    model.getId(), mergedCount, deleted, newContent.size());

            String message = String.format("Committed %d blocks to model, deleted %d overlay keys",
                    mergedCount, deleted);

            return CommandResult.success(message);

        } catch (Exception e) {
            log.error("Model commit failed for session {}: {}", sessionId, e.getMessage(), e);
            return CommandResult.error(-4, "Model commit failed: " + e.getMessage());
        }
    }

    /**
     * Apply rotation to X,Z coordinates.
     * Rotation is in 90-degree steps: 0=0°, 1=90°, 2=180°, 3=270°
     *
     * @param x X coordinate (relative)
     * @param z Z coordinate (relative)
     * @param rotation Rotation steps (0-3)
     * @return [newX, newZ]
     */
    private int[] applyRotation(int x, int z, int rotation) {
        int steps = rotation % 4; // Normalize to 0-3

        switch (steps) {
            case 0: // 0° - no rotation
                return new int[]{x, z};
            case 1: // 90° clockwise: (x,z) → (-z,x)
                return new int[]{-z, x};
            case 2: // 180°: (x,z) → (-x,-z)
                return new int[]{-x, -z};
            case 3: // 270° clockwise (= 90° counter-clockwise): (x,z) → (z,-x)
                return new int[]{z, -x};
            default:
                return new int[]{x, z};
        }
    }

    private String positionKey(int x, int y, int z) {
        return x + ":" + y + ":" + z;
    }

    @Override
    public String getHelp() {
        return "Commit overlay edits to the selected layer";
    }

    @Override
    public boolean requiresSession() {
        return false;  // sessionId in context
    }
}
