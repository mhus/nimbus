package de.mhus.nimbus.shared.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for voxel operations via Kafka
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VoxelOperationMessage {

    public enum OperationType {
        SAVE, DELETE, BATCH_SAVE, CLEAR_CHUNK
    }

    private String messageId;
    private OperationType operation;
    private String worldId;
    private VoxelData voxelData;
    private ChunkData chunkData;
    private BatchData batchData;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VoxelData {
        private int x;
        private int y;
        private int z;
        private String voxelJson; // JSON representation of the voxel
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChunkData {
        private int chunkX;
        private int chunkY;
        private int chunkZ;
        private String chunkJson; // JSON representation of the chunk
        private boolean includeEmpty = false;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BatchData {
        private String voxelsJson; // JSON array of voxels
    }
}
