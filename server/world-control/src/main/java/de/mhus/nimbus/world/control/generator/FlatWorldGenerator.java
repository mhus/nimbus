package de.mhus.nimbus.world.control.generator;

import de.mhus.nimbus.generated.types.Block;
import de.mhus.nimbus.world.shared.job.JobExecutionException;
import de.mhus.nimbus.world.shared.layer.LayerBlock;
import de.mhus.nimbus.world.shared.world.FlatPosition;
import de.mhus.nimbus.world.shared.world.WHexGrid;
import de.mhus.nimbus.world.shared.world.WWorld;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Flat world terrain generator.
 * Generates simple flat terrain at configurable height.
 */
@Component
@Slf4j
public class FlatWorldGenerator extends AbstractTerrainGenerator {

    public static final String PARAM_GROUND_LEVEL = "groundLevel";
    public static final String PARAM_LAYER_COUNT = "layerCount";
    public static final String PARAM_GROUND_BLOCK_TYPE_ID = "groundBlockTypeId";

    private int groundLevel = 64;
    private int layerCount = 2;
    private String groundBlockTypeId = "w/310";

    @Override
    public String getExecutorName() {
        return "flat-world-generator";
    }

    @Override
    protected void configureGenerator(Map<String, String> genParams) {
        groundLevel = getIntParameter(genParams, PARAM_GROUND_LEVEL, 64);
        layerCount = getIntParameter(genParams, PARAM_LAYER_COUNT, 2);
        groundBlockTypeId = getParameter(genParams, PARAM_GROUND_BLOCK_TYPE_ID, "w/310");

        log.info("FlatWorldGenerator configured: groundLevel={}, layerCount={}, blockType={}",
                groundLevel, layerCount, groundBlockTypeId);
    }

    @Override
    protected int getTerrainHeight(int worldX, int worldZ) {
        return groundLevel;
    }

    @Override
    protected List<Float> calculateOffsets(int x, int y, int z, int[][] heightMap, int centerHeight) {
        return null;
    }

    @Override
    protected int generateTerrain(WWorld world, WHexGrid hexGrid, GeneratorContext context)
            throws JobExecutionException {

        Map<String, List<LayerBlock>> chunkBlocks = new HashMap<>();
        int totalBlocks = 0;

        for (FlatPosition pos : hexGrid.getFlatPositionSet(world)) {
            String chunkKey = world.getChunkKey(pos.x(), pos.z());

            for (int i = 0; i < layerCount; i++) {
                int y = groundLevel - i;

                Block block = createBlock(pos.x(), y, pos.z(), groundBlockTypeId);
                LayerBlock layerBlock = createLayerBlock(block);

                chunkBlocks.computeIfAbsent(chunkKey, k -> new ArrayList<>()).add(layerBlock);
                totalBlocks++;
            }
        }

        for (Map.Entry<String, List<LayerBlock>> entry : chunkBlocks.entrySet()) {
            saveChunk(context.worldId(), context.layerDataId(), entry.getKey(), entry.getValue());
        }

        return totalBlocks;
    }
}
