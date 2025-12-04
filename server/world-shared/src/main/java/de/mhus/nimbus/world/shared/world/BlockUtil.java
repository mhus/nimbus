package de.mhus.nimbus.world.shared.world;

import de.mhus.nimbus.generated.types.Block;
import de.mhus.nimbus.generated.types.Vector3;
import lombok.experimental.UtilityClass;

/**
 * Utility methods for Block operations.
 */
@UtilityClass
public class BlockUtil {

    /**
     * Check if block type represents AIR (empty space).
     * AIR types: "0", "w:0", null, empty string
     *
     * @param blockTypeId Block type identifier
     * @return true if block type is AIR
     */
    public static boolean isAirType(String blockTypeId) {
        if (blockTypeId == null || blockTypeId.isEmpty()) {
            return true;
        }
        return "0".equals(blockTypeId) || "w:0".equals(blockTypeId);
    }

    /**
     * Generate position key for Redis hash storage.
     * Format: "x:y:z" (e.g., "15:64:23")
     *
     * @param position Vector3 position
     * @return Position key string
     */
    public static String positionKey(Vector3 position) {
        if (position == null) {
            return "0:0:0";
        }
        return String.format("%d:%d:%d",
                (int) position.getX(),
                (int) position.getY(),
                (int) position.getZ());
    }

    /**
     * Generate position key from coordinates.
     *
     * @param x X coordinate
     * @param y Y coordinate
     * @param z Z coordinate
     * @return Position key string
     */
    public static String positionKey(int x, int y, int z) {
        return x + ":" + y + ":" + z;
    }

    /**
     * Extract position key from Block.
     *
     * @param block Block instance
     * @return Position key string
     */
    public static String positionKey(Block block) {
        return block != null ? positionKey(block.getPosition()) : "0:0:0";
    }
}
