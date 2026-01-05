package de.mhus.nimbus.world.generator.blocks.generator;

import de.mhus.nimbus.generated.types.Block;
import de.mhus.nimbus.shared.utils.FastNoiseLite;
import de.mhus.nimbus.world.shared.job.JobExecutionException;
import de.mhus.nimbus.world.shared.layer.LayerBlock;
import de.mhus.nimbus.world.shared.world.FlatPosition;
import de.mhus.nimbus.world.shared.world.WHexGrid;
import de.mhus.nimbus.world.shared.world.WWorld;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Hilly terrain generator with more pronounced hills.
 * Uses larger noise scales and steeper slopes than NormalWorldGenerator.
 */
@Component
@Slf4j
public class HillyWorldGenerator extends AbstractTerrainGenerator {

    public static final String PARAM_HILL_HEIGHT = "hillHeight";
    public static final String PARAM_BASE_HEIGHT = "baseHeight";
    public static final String PARAM_WATER_LEVEL = "waterLevel";
    public static final String PARAM_SEED = "seed";
    public static final String PARAM_GRASS_BLOCK_TYPE_ID = "grassBlockTypeId";
    public static final String PARAM_DIRT_BLOCK_TYPE_ID = "dirtBlockTypeId";
    public static final String PARAM_SAND_BLOCK_TYPE_ID = "sandBlockTypeId";
    public static final String PARAM_WATER_BLOCK_TYPE_ID = "waterBlockTypeId";

    private static final double SCALE_1 = 0.008;
    private static final double SCALE_2 = 0.04;
    private static final double SCALE_3 = 0.08;
    private static final double WEIGHT_1 = 0.7;
    private static final double WEIGHT_2 = 0.2;
    private static final double WEIGHT_3 = 0.1;

    private int waterLevel = 62;
    private int baseHeight = 64;
    private int hillHeight = 64;
    private String grassBlockTypeId = "w/310";
    private String dirtBlockTypeId = "w/279";
    private String sandBlockTypeId = "w/520";
    private String waterBlockTypeId = "w/5000";

    private FastNoiseLite noise;

    @Override
    public String getExecutorName() {
        return "hilly-world-generator";
    }

    @Override
    protected void configureGenerator(Map<String, String> genParams) {
        waterLevel = getIntParameter(genParams, PARAM_WATER_LEVEL, 62);
        baseHeight = getIntParameter(genParams, PARAM_BASE_HEIGHT, 64);
        hillHeight = getIntParameter(genParams, PARAM_HILL_HEIGHT, 64);
        long seed = getLongParameter(genParams, PARAM_SEED, System.currentTimeMillis());

        grassBlockTypeId = getParameter(genParams, PARAM_GRASS_BLOCK_TYPE_ID, "w/310");
        dirtBlockTypeId = getParameter(genParams, PARAM_DIRT_BLOCK_TYPE_ID, "w/279");
        sandBlockTypeId = getParameter(genParams, PARAM_SAND_BLOCK_TYPE_ID, "w/520");
        waterBlockTypeId = getParameter(genParams, PARAM_WATER_BLOCK_TYPE_ID, "w/5000");

        noise = new FastNoiseLite((int) seed);
        noise.SetNoiseType(FastNoiseLite.NoiseType.OpenSimplex2);
        noise.SetFrequency(1.0f);

        log.info("HillyWorldGenerator configured: water={}, base={}, hillHeight={}, seed={}",
                waterLevel, baseHeight, hillHeight, seed);
    }

    @Override
    protected int getTerrainHeight(int worldX, int worldZ) {
        double noise1 = noise.GetNoise((float) (worldX * SCALE_1), (float) (worldZ * SCALE_1));
        double noise2 = noise.GetNoise((float) (worldX * SCALE_2), (float) (worldZ * SCALE_2));
        double noise3 = noise.GetNoise((float) (worldX * SCALE_3), (float) (worldZ * SCALE_3));

        double combined = noise1 * WEIGHT_1 + noise2 * WEIGHT_2 + noise3 * WEIGHT_3;
        double height = baseHeight + combined * hillHeight;

        return (int) Math.floor(height);
    }

    @Override
    protected int generateTerrain(WWorld world, WHexGrid hexGrid, GeneratorContext context)
            throws JobExecutionException {

        int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE;
        int minZ = Integer.MAX_VALUE, maxZ = Integer.MIN_VALUE;

        List<FlatPosition> positions = new ArrayList<>();
        for (FlatPosition pos : hexGrid.getFlatPositionSet(world)) {
            positions.add(pos);
            minX = Math.min(minX, pos.x());
            maxX = Math.max(maxX, pos.x());
            minZ = Math.min(minZ, pos.z());
            maxZ = Math.max(maxZ, pos.z());
        }

        int[][] heightMap = new int[maxX - minX + 3][maxZ - minZ + 3];
        for (int x = minX - 1; x <= maxX + 1; x++) {
            for (int z = minZ - 1; z <= maxZ + 1; z++) {
                heightMap[x - minX + 1][z - minZ + 1] = getTerrainHeight(x, z);
            }
        }

        Map<String, List<LayerBlock>> chunkBlocks = new HashMap<>();

        for (FlatPosition pos : positions) {
            int localX = pos.x() - minX + 1;
            int localZ = pos.z() - minZ + 1;
            int terrainHeight = heightMap[localX][localZ];
            String chunkKey = world.getChunkKey(pos.x(), pos.z());

            String surfaceBlockId = terrainHeight <= waterLevel ? sandBlockTypeId : grassBlockTypeId;
            Block surfaceBlock = createBlock(pos.x(), terrainHeight, pos.z(), surfaceBlockId);
            applyOffsets(surfaceBlock, localX, terrainHeight, localZ, heightMap, terrainHeight);
            chunkBlocks.computeIfAbsent(chunkKey, k -> new ArrayList<>()).add(createLayerBlock(surfaceBlock));

            if (terrainHeight > 0) {
                Block dirtBlock = createBlock(pos.x(), terrainHeight - 1, pos.z(), dirtBlockTypeId);
                applyOffsets(dirtBlock, localX, terrainHeight - 1, localZ, heightMap, terrainHeight);
                chunkBlocks.computeIfAbsent(chunkKey, k -> new ArrayList<>()).add(createLayerBlock(dirtBlock));
            }

            if (terrainHeight < waterLevel) {
                Block waterBlock = createBlock(pos.x(), waterLevel, pos.z(), waterBlockTypeId);
                chunkBlocks.computeIfAbsent(chunkKey, k -> new ArrayList<>()).add(createLayerBlock(waterBlock));
            }
        }

        int totalBlocks = 0;
        for (Map.Entry<String, List<LayerBlock>> entry : chunkBlocks.entrySet()) {
            saveChunk(context.worldId(), context.layerDataId(), entry.getKey(), entry.getValue());
            totalBlocks += entry.getValue().size();
        }

        return totalBlocks;
    }

    private void applyOffsets(Block block, int localX, int y, int localZ, int[][] heightMap, int centerHeight) {
        List<Float> offsets = calculateOffsets(localX, y, localZ, heightMap, centerHeight);
        if (offsets != null && !offsets.isEmpty()) {
            block.setOffsets(offsets);
        }
    }

    @Override
    protected List<Float> calculateOffsets(int x, int y, int z, int[][] heightMap, int centerHeight) {
        int h_nw = getHeight(heightMap, x - 1, z - 1, centerHeight);
        int h_n = getHeight(heightMap, x, z - 1, centerHeight);
        int h_ne = getHeight(heightMap, x + 1, z - 1, centerHeight);
        int h_w = getHeight(heightMap, x - 1, z, centerHeight);
        int h_e = getHeight(heightMap, x + 1, z, centerHeight);
        int h_sw = getHeight(heightMap, x - 1, z + 1, centerHeight);
        int h_s = getHeight(heightMap, x, z + 1, centerHeight);
        int h_se = getHeight(heightMap, x + 1, z + 1, centerHeight);

        List<Float> offsets = new ArrayList<>(Collections.nCopies(24, 0.0f));

        float yDiff = y - centerHeight;
        float bottomOffset = clamp(yDiff * 0.3f);

        offsets.set(1, bottomOffset);
        offsets.set(4, bottomOffset);
        offsets.set(7, bottomOffset);
        offsets.set(10, bottomOffset);

        float topYDiff = y + 1 - centerHeight;
        float topOffset = clamp(topYDiff * 0.3f);

        float h4 = (h_w + h_nw + h_n) / 3.0f;
        offsets.set(13, clamp((h4 - centerHeight) * 0.35f + topOffset));

        float h5 = (h_e + h_ne + h_n) / 3.0f;
        offsets.set(16, clamp((h5 - centerHeight) * 0.35f + topOffset));

        float h6 = (h_e + h_se + h_s) / 3.0f;
        offsets.set(19, clamp((h6 - centerHeight) * 0.35f + topOffset));

        float h7 = (h_w + h_sw + h_s) / 3.0f;
        offsets.set(22, clamp((h7 - centerHeight) * 0.35f + topOffset));

        boolean hasOffset = offsets.stream().anyMatch(o -> Math.abs(o) > 0.001f);
        return hasOffset ? offsets : null;
    }

    private int getHeight(int[][] heightMap, int x, int z, int defaultHeight) {
        try {
            return heightMap[x][z];
        } catch (ArrayIndexOutOfBoundsException e) {
            return defaultHeight;
        }
    }
}
