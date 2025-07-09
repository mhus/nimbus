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
            LOGGER.info("REST: Deleting voxel at ({},{},{}) in world: {}", x, y, z, worldId);
            CompletableFuture<Void> future = worldVoxelClient.deleteVoxel(worldId, x, y, z);
            return ResponseEntity.ok("Voxel deletion request sent for world: " + worldId + " at (" + x + "," + y + "," + z + ")");
        } catch (Exception e) {
            LOGGER.error("Failed to delete voxel", e);
            return ResponseEntity.internalServerError().body("Failed to delete voxel: " + e.getMessage());
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
            LOGGER.info("REST: Clearing chunk ({},{},{}) in world: {}", chunkX, chunkY, chunkZ, worldId);
            CompletableFuture<Void> future = worldVoxelClient.clearChunk(worldId, chunkX, chunkY, chunkZ);
            return ResponseEntity.ok("Chunk clear request sent for world: " + worldId + " chunk (" + chunkX + "," + chunkY + "," + chunkZ + ")");
        } catch (Exception e) {
            LOGGER.error("Failed to clear chunk", e);
            return ResponseEntity.internalServerError().body("Failed to clear chunk: " + e.getMessage());
        }
    }

    /**
     * Test chunk loading
     */
    @GetMapping("/chunk/{worldId}")
    public ResponseEntity<String> loadChunk(
            @PathVariable String worldId,
            @RequestParam int chunkX,
            @RequestParam int chunkY,
            @RequestParam int chunkZ) {
        try {
            LOGGER.info("REST: Loading chunk ({},{},{}) in world: {}", chunkX, chunkY, chunkZ, worldId);
            CompletableFuture<Void> future = worldVoxelClient.loadChunk(worldId, chunkX, chunkY, chunkZ);
            return ResponseEntity.ok("Chunk load request sent for world: " + worldId + " chunk (" + chunkX + "," + chunkY + "," + chunkZ + ")");
        } catch (Exception e) {
            LOGGER.error("Failed to load chunk", e);
            return ResponseEntity.internalServerError().body("Failed to load chunk: " + e.getMessage());
        }
    }

    /**
     * Test full chunk loading
     */
    @GetMapping("/chunk/{worldId}/full")
    public ResponseEntity<String> loadFullChunk(
            @PathVariable String worldId,
            @RequestParam int chunkX,
            @RequestParam int chunkY,
            @RequestParam int chunkZ,
            @RequestParam(defaultValue = "false") boolean includeEmpty) {
        try {
            LOGGER.info("REST: Loading full chunk ({},{},{}) in world: {}, includeEmpty: {}",
                       chunkX, chunkY, chunkZ, worldId, includeEmpty);
            CompletableFuture<Void> future = worldVoxelClient.loadFullChunk(worldId, chunkX, chunkY, chunkZ, includeEmpty);
            return ResponseEntity.ok("Full chunk load request sent for world: " + worldId +
                                   " chunk (" + chunkX + "," + chunkY + "," + chunkZ + ") includeEmpty: " + includeEmpty);
        } catch (Exception e) {
            LOGGER.error("Failed to load full chunk", e);
            return ResponseEntity.internalServerError().body("Failed to load full chunk: " + e.getMessage());
        }
    }
}
