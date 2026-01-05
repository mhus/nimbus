package de.mhus.nimbus.world.generator.flat;

import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;

public class WFlat {

    public static final int NOT_SET = 0;

    private String worldId;
    private String layerDataId;
    private String flatId;

    private int mountX;
    private int mountZ;
    @Getter
    private int oceanLevel;
    @Getter @Setter
    private String oceanBlockId;

    @Getter
    private int sizeX;
    @Getter
    private int sizeZ;
    private byte[] levels;
    private byte[] columns;
    private HashMap<String, String> extraBlocks = new HashMap<>(); // for water and ocean ...

    private HashMap<Byte, ColumnDefinition> definitions = new HashMap<>();


    public void initWithSize(int sizeX, int sizeZ) {
        if (sizeX <= 0 || sizeZ <= 0 || sizeX > 400 || sizeZ > 400)
            throw new IllegalArgumentException("Size out of range");
        if (sizeX != 0)
            throw  new IllegalStateException("Already initialized");
        this.sizeX = sizeX;
        this.sizeZ = sizeZ;
        this.levels = new byte[sizeX * sizeZ];
        this.columns = new byte[sizeX * sizeZ];
    }

    public void setLevel(int x, int z, int level) {
        if (x < 0 || z < 0 || x >= sizeX || z >= sizeZ)
            throw new IllegalArgumentException("Coordinates out of range");
        if (level < 0) level = 0;
        if (level > 255) level = 255;
        levels[x + z * sizeX] = (byte)level;
    }

    public int getLevel(int x, int z) {
        if (x < 0 || z < 0 || x >= sizeX || z >= sizeZ)
            throw new IllegalArgumentException("Coordinates out of range");
        return Byte.toUnsignedInt(levels[x + z * sizeX]);
    }

    public void setColumn(int x, int z, int definition) {
        if (x < 0 || z < 0 || x >= sizeX || z >= sizeZ)
            throw new IllegalArgumentException("Coordinates out of range");
        if (definition < 0 || definition > 255)
            throw new IllegalArgumentException("Size out of range");
        columns[x + z * sizeX] = (byte)definition;
    }

    public int getColumn(int x, int z) {
        if (x < 0 || z < 0 || x >= sizeX || z >= sizeZ)
            throw new IllegalArgumentException("Coordinates out of range");
        return Byte.toUnsignedInt(columns[x + z * sizeX]);
    }

    public ColumnDefinition getColumnDefinition(int x, int z) {
        int definition = getColumn(x, z);
        return getDefinition(definition);
    }

    public void setExtraBlock(int x, int y, int z, String blockId) {
        String name = x + ":" + z + ":" + y;
        if (blockId == null)
            extraBlocks.remove(name);
        else
            extraBlocks.put(name, blockId);
    }

    public String getExtraBlock(int x, int y, int z) {
        String name = x + ":" + z + ":" + y;
        return extraBlocks.get(name);
    }

    public String[] getExtraBlocksForColumn(int x, int z) {
        String prefix = x + ":" + z + ":";
        String[] res = new String[256];
        for (String key : extraBlocks.keySet()) {
            if (key.startsWith(prefix)) {
                String yStr = key.substring(prefix.length());
                int y = Integer.parseInt(yStr);
                if (y >= 0 && y < 256)
                    res[y] = extraBlocks.get(key);
            }
        }
        return res;
    }

    public void setDefinition(int id, ColumnDefinition definition) {
        if (id < 0 || id > 255)
            throw new IllegalArgumentException("Definition id out of range");
        if (id == NOT_SET)
            return;
        definitions.put((byte)id, definition);
    }

    public ColumnDefinition getDefinition(int id) {
        if (id < 0 || id > 255)
            throw new IllegalArgumentException("Definition id out of range");
        if (id == NOT_SET)
            return null;
        return definitions.get((byte)id);
    }

    @Data
    @Builder
    public static class ColumnDefinition {
        private String blockId;
        private String nextBlockId;
        private boolean hasOcean;

        /**
         * Returns the blockId for the y - starts at level
         * @param level - value from levels or ocean level
         * @param y - y coordinate
         * @return null or block id
         */
        public String getBlockAt(WFlat flat, int level, int y, String[] extraBlocks) {
            if (y < 0 || y > 255)
                return null;
            // first: my own block
            if (y == level)
                return blockId;
            // second: extra block
            if (extraBlocks != null && extraBlocks[y] != null)
                return extraBlocks[y];
            // third: next block
            if (y < level)
                return nextBlockId != null ? nextBlockId : blockId;
            // finally: ocean block
            if (hasOcean && y == flat.getOceanLevel())
                return flat.getOceanBlockId();
            // or air
            return null;
        }

    }

}
