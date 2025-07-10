package de.mhus.nimbus.testcommon.controller;

import de.mhus.nimbus.common.client.WorldVoxelClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.CompletableFuture;

/**
 * REST Controller for testing WorldVoxelClient functionality
 */
@RestController
@RequestMapping("/api/test/world-voxel")
public class WorldVoxelTestController {

    private static final Logger LOGGER = LoggerFactory.getLogger(WorldVoxelTestController.class);

    private final WorldVoxelClient worldVoxelClient;

    @Autowired
    public WorldVoxelTestController(WorldVoxelClient worldVoxelClient) {
        this.worldVoxelClient = worldVoxelClient;
    }

    /**
     * Test voxel deletion
     */
    @DeleteMapping("/voxel/{worldId}")
    public ResponseEntity<String> deleteVoxel(
            @PathVariable String worldId,
            @RequestParam int x,
            @RequestParam int y,
            @RequestParam int z) {
        try {
            LOGGER.info("Testing voxel deletion at ({},{},{}) in world: {}", x, y, z, worldId);

            CompletableFuture<Void> future = worldVoxelClient.deleteVoxel(worldId, x, y, z);

            // Wait for the response
            future.get();

            String result = "Voxel at (" + x + "," + y + "," + z + ") successfully deleted from world '" + worldId + "'";
            LOGGER.info("Voxel deletion completed: {}", result);

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            String errorMessage = "Failed to delete voxel at (" + x + "," + y + "," + z + ") in world '" + worldId + "': " + e.getMessage();
            LOGGER.error(errorMessage, e);
            return ResponseEntity.internalServerError().body(errorMessage);
        }
    }

    /**
     * Test voxel saving
     */
    @PostMapping("/voxel/{worldId}")
    public ResponseEntity<String> saveVoxel(
            @PathVariable String worldId,
            @RequestParam int x,
            @RequestParam int y,
            @RequestParam int z,
            @RequestParam String voxelType) {
        try {
            LOGGER.info("Testing voxel saving at ({},{},{}) in world: {}", x, y, z, worldId);

            // Parse voxelType string to VoxelType enum (default to STONE if invalid)
            de.mhus.nimbus.shared.voxel.VoxelType type;
            try {
                type = de.mhus.nimbus.shared.voxel.VoxelType.valueOf(voxelType.toUpperCase());
            } catch (IllegalArgumentException e) {
                type = de.mhus.nimbus.shared.voxel.VoxelType.STONE; // Default fallback
            }

            de.mhus.nimbus.shared.voxel.Voxel voxel = new de.mhus.nimbus.shared.voxel.Voxel(x, y, z, type);

            CompletableFuture<Void> future = worldVoxelClient.saveVoxel(worldId, voxel);

            // Wait for the response
            future.get();

            String result = "Voxel at (" + x + "," + y + "," + z + ") successfully saved to world '" + worldId + "' with type: " + type.getDisplayName();
            LOGGER.info("Voxel saving completed: {}", result);

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            String errorMessage = "Failed to save voxel at (" + x + "," + y + "," + z + ") in world '" + worldId + "': " + e.getMessage();
            LOGGER.error(errorMessage, e);
            return ResponseEntity.internalServerError().body(errorMessage);
        }
    }

    /**
     * Test chunk clearing
     */
    @DeleteMapping("/chunk/{worldId}")
    public ResponseEntity<String> clearChunk(
            @PathVariable String worldId,
            @RequestParam int chunkX,
            @RequestParam int chunkY,
            @RequestParam int chunkZ) {
        try {
            LOGGER.info("Testing chunk clearing at ({},{},{}) in world: {}", chunkX, chunkY, chunkZ, worldId);

            CompletableFuture<Void> future = worldVoxelClient.clearChunk(worldId, chunkX, chunkY, chunkZ);

            // Wait for the response
            future.get();

            String result = "Chunk at (" + chunkX + "," + chunkY + "," + chunkZ + ") successfully cleared in world '" + worldId + "'";
            LOGGER.info("Chunk clearing completed: {}", result);

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            String errorMessage = "Failed to clear chunk at (" + chunkX + "," + chunkY + "," + chunkZ + ") in world '" + worldId + "': " + e.getMessage();
            LOGGER.error(errorMessage, e);
            return ResponseEntity.internalServerError().body(errorMessage);
        }
    }

    /**
     * Test chunk saving
     */
    @PostMapping("/chunk/{worldId}")
    public ResponseEntity<String> saveChunk(
            @PathVariable String worldId,
            @RequestParam int chunkX,
            @RequestParam int chunkY,
            @RequestParam int chunkZ) {
        try {
            LOGGER.info("Testing chunk saving at ({},{},{}) in world: {}", chunkX, chunkY, chunkZ, worldId);

            // Create a VoxelChunk object using the available constructor
            de.mhus.nimbus.shared.voxel.VoxelChunk chunk = new de.mhus.nimbus.shared.voxel.VoxelChunk();
            chunk.setChunkX(chunkX);
            chunk.setChunkY(chunkY);
            chunk.setChunkZ(chunkZ);

            CompletableFuture<Void> future = worldVoxelClient.saveChunk(worldId, chunk);

            // Wait for the response
            future.get();

            String result = "Chunk at (" + chunkX + "," + chunkY + "," + chunkZ + ") successfully saved to world '" + worldId + "'";
            LOGGER.info("Chunk saving completed: {}", result);

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            String errorMessage = "Failed to save chunk at (" + chunkX + "," + chunkY + "," + chunkZ + ") in world '" + worldId + "': " + e.getMessage();
            LOGGER.error(errorMessage, e);
            return ResponseEntity.internalServerError().body(errorMessage);
        }
    }

    /**
     * Test batch voxel saving
     */
    @PostMapping("/voxel/batch/{worldId}")
    public ResponseEntity<String> batchSaveVoxels(
            @PathVariable String worldId,
            @RequestParam String voxelList) {
        try {
            LOGGER.info("Testing batch voxel saving in world: {}", worldId);

            // Parse the voxel list - expecting format like "x1,y1,z1,type1;x2,y2,z2,type2"
            java.util.List<de.mhus.nimbus.shared.voxel.VoxelInstance> voxels = new java.util.ArrayList<>();
            String[] voxelEntries = voxelList.split(";");

            for (String entry : voxelEntries) {
                String[] parts = entry.split(",");
                if (parts.length >= 4) {
                    int x = Integer.parseInt(parts[0].trim());
                    int y = Integer.parseInt(parts[1].trim());
                    int z = Integer.parseInt(parts[2].trim());
                    String typeStr = parts[3].trim();

                    // Create a Voxel type for the VoxelInstance
                    de.mhus.nimbus.shared.voxel.Voxel voxelType;
                    try {
                        // Try to create a voxel type based on the type string
                        voxelType = de.mhus.nimbus.shared.voxel.Voxel.builder()
                                .displayName(typeStr)
                                .hardness(3)
                                .build();
                    } catch (Exception e) {
                        // Fallback to default voxel type
                        voxelType = de.mhus.nimbus.shared.voxel.Voxel.builder()
                                .displayName("STONE")
                                .hardness(3)
                                .build();
                    }

                    // Create VoxelInstance with coordinates and voxel type
                    voxels.add(new de.mhus.nimbus.shared.voxel.VoxelInstance(x, y, z, voxelType));
                }
            }

            CompletableFuture<Void> future = worldVoxelClient.batchSaveVoxels(worldId, voxels);

            // Wait for the response
            future.get();

            String result = "Batch operation successfully saved " + voxels.size() + " voxels to world '" + worldId + "'";
            LOGGER.info("Batch voxel saving completed: {}", result);

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            String errorMessage = "Failed to batch save voxels in world '" + worldId + "': " + e.getMessage();
            LOGGER.error(errorMessage, e);
            return ResponseEntity.internalServerError().body(errorMessage);
        }
    }
}
