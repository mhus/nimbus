package de.mhus.nimbus.voxelworld.controller;

import de.mhus.nimbus.shared.voxel.Voxel;
import de.mhus.nimbus.shared.voxel.VoxelChunk;
import de.mhus.nimbus.voxelworld.service.VoxelWorldService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * REST Controller for VoxelWorld operations
 */
@RestController
@RequestMapping("/api/voxelworld")
@CrossOrigin(origins = "*")
public class VoxelWorldController {

    private static final Logger LOGGER = LoggerFactory.getLogger(VoxelWorldController.class);

    private final VoxelWorldService voxelWorldService;

    @Autowired
    public VoxelWorldController(VoxelWorldService voxelWorldService) {
        this.voxelWorldService = voxelWorldService;
    }

    /**
     * Save a voxel
     */
    @PostMapping("/worlds/{worldId}/voxels")
    public ResponseEntity<String> saveVoxel(@PathVariable String worldId, @RequestBody Voxel voxel) {
        try {
            LOGGER.info("REST: Saving voxel at ({}, {}, {}) in world {}",
                       voxel.getX(), voxel.getY(), voxel.getZ(), worldId);

            voxelWorldService.saveVoxel(worldId, voxel);
            return ResponseEntity.ok("Voxel saved successfully");
        } catch (Exception e) {
            LOGGER.error("Failed to save voxel", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to save voxel: " + e.getMessage());
        }
    }

    /**
     * Get a voxel by coordinates
     */
    @GetMapping("/worlds/{worldId}/voxels/{x}/{y}/{z}")
    public ResponseEntity<Voxel> getVoxel(@PathVariable String worldId,
                                         @PathVariable int x,
                                         @PathVariable int y,
                                         @PathVariable int z) {
        try {
            LOGGER.debug("REST: Getting voxel at ({}, {}, {}) in world {}", x, y, z, worldId);

            Optional<Voxel> voxel = voxelWorldService.getVoxel(worldId, x, y, z);

            if (voxel.isPresent()) {
                return ResponseEntity.ok(voxel.get());
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            LOGGER.error("Failed to get voxel", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Delete a voxel
     */
    @DeleteMapping("/worlds/{worldId}/voxels/{x}/{y}/{z}")
    public ResponseEntity<String> deleteVoxel(@PathVariable String worldId,
                                             @PathVariable int x,
                                             @PathVariable int y,
                                             @PathVariable int z) {
        try {
            LOGGER.info("REST: Deleting voxel at ({}, {}, {}) in world {}", x, y, z, worldId);

            boolean deleted = voxelWorldService.deleteVoxel(worldId, x, y, z);

            if (deleted) {
                return ResponseEntity.ok("Voxel deleted successfully");
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            LOGGER.error("Failed to delete voxel", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to delete voxel: " + e.getMessage());
        }
    }

    /**
     * Load a chunk
     */
    @GetMapping("/worlds/{worldId}/chunks/{chunkX}/{chunkY}/{chunkZ}")
    public ResponseEntity<VoxelChunk> loadChunk(@PathVariable String worldId,
                                               @PathVariable int chunkX,
                                               @PathVariable int chunkY,
                                               @PathVariable int chunkZ) {
        try {
            LOGGER.debug("REST: Loading chunk ({}, {}, {}) in world {}", chunkX, chunkY, chunkZ, worldId);

            VoxelChunk chunk = voxelWorldService.loadChunk(worldId, chunkX, chunkY, chunkZ);
            return ResponseEntity.ok(chunk);
        } catch (Exception e) {
            LOGGER.error("Failed to load chunk", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Load a complete chunk with all voxels and metadata
     */
    @GetMapping("/worlds/{worldId}/chunks/{chunkX}/{chunkY}/{chunkZ}/full")
    public ResponseEntity<VoxelChunk> loadFullChunk(@PathVariable String worldId,
                                                   @PathVariable int chunkX,
                                                   @PathVariable int chunkY,
                                                   @PathVariable int chunkZ,
                                                   @RequestParam(defaultValue = "false") boolean includeEmpty) {
        try {
            LOGGER.info("REST: Loading full chunk ({}, {}, {}) in world {} (includeEmpty: {})",
                       chunkX, chunkY, chunkZ, worldId, includeEmpty);

            VoxelChunk chunk = voxelWorldService.loadFullChunk(worldId, chunkX, chunkY, chunkZ, includeEmpty);

            LOGGER.debug("REST: Successfully loaded full chunk with {} voxels", chunk.getVoxelCount());
            return ResponseEntity.ok(chunk);
        } catch (Exception e) {
            LOGGER.error("Failed to load full chunk ({}, {}, {}) in world {}: {}",
                        chunkX, chunkY, chunkZ, worldId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Load a complete chunk with all voxels (convenience endpoint)
     */
    @GetMapping("/worlds/{worldId}/chunks/{chunkX}/{chunkY}/{chunkZ}/complete")
    public ResponseEntity<VoxelChunk> loadCompleteChunk(@PathVariable String worldId,
                                                       @PathVariable int chunkX,
                                                       @PathVariable int chunkY,
                                                       @PathVariable int chunkZ) {
        try {
            LOGGER.info("REST: Loading complete chunk ({}, {}, {}) in world {}",
                       chunkX, chunkY, chunkZ, worldId);

            VoxelChunk chunk = voxelWorldService.loadFullChunk(worldId, chunkX, chunkY, chunkZ);

            // Add additional metadata to response headers
            return ResponseEntity.ok()
                    .header("X-Chunk-Voxel-Count", String.valueOf(chunk.getVoxelCount()))
                    .header("X-Chunk-Generated", String.valueOf(chunk.isGenerated()))
                    .header("X-Chunk-Modified", String.valueOf(chunk.isModified()))
                    .header("X-Load-Time", String.valueOf(System.currentTimeMillis()))
                    .body(chunk);
        } catch (Exception e) {
            LOGGER.error("Failed to load complete chunk ({}, {}, {}) in world {}: {}",
                        chunkX, chunkY, chunkZ, worldId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Save a chunk
     */
    @PostMapping("/worlds/{worldId}/chunks")
    public ResponseEntity<String> saveChunk(@PathVariable String worldId, @RequestBody VoxelChunk chunk) {
        try {
            LOGGER.info("REST: Saving chunk ({}, {}, {}) in world {}",
                       chunk.getChunkX(), chunk.getChunkY(), chunk.getChunkZ(), worldId);

            int savedCount = voxelWorldService.saveChunk(worldId, chunk);
            return ResponseEntity.ok("Chunk saved successfully. " + savedCount + " voxels saved.");
        } catch (Exception e) {
            LOGGER.error("Failed to save chunk", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to save chunk: " + e.getMessage());
        }
    }

    /**
     * Clear a chunk
     */
    @DeleteMapping("/worlds/{worldId}/chunks/{chunkX}/{chunkY}/{chunkZ}")
    public ResponseEntity<String> clearChunk(@PathVariable String worldId,
                                           @PathVariable int chunkX,
                                           @PathVariable int chunkY,
                                           @PathVariable int chunkZ) {
        try {
            LOGGER.info("REST: Clearing chunk ({}, {}, {}) in world {}", chunkX, chunkY, chunkZ, worldId);

            long deletedCount = voxelWorldService.clearChunk(worldId, chunkX, chunkY, chunkZ);
            return ResponseEntity.ok("Chunk cleared successfully. " + deletedCount + " voxels deleted.");
        } catch (Exception e) {
            LOGGER.error("Failed to clear chunk", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to clear chunk: " + e.getMessage());
        }
    }

    /**
     * Get voxels in a coordinate range
     */
    @GetMapping("/worlds/{worldId}/voxels/range")
    public ResponseEntity<List<Voxel>> getVoxelsInRange(@PathVariable String worldId,
                                                       @RequestParam int minX, @RequestParam int maxX,
                                                       @RequestParam int minY, @RequestParam int maxY,
                                                       @RequestParam int minZ, @RequestParam int maxZ) {
        try {
            LOGGER.debug("REST: Getting voxels in range ({},{},{}) to ({},{},{}) in world {}",
                        minX, minY, minZ, maxX, maxY, maxZ, worldId);

            List<Voxel> voxels = voxelWorldService.getVoxelsInRange(worldId, minX, maxX, minY, maxY, minZ, maxZ);
            return ResponseEntity.ok(voxels);
        } catch (Exception e) {
            LOGGER.error("Failed to get voxels in range", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get world statistics
     */
    @GetMapping("/worlds/{worldId}/stats")
    public ResponseEntity<WorldStats> getWorldStats(@PathVariable String worldId) {
        try {
            LOGGER.debug("REST: Getting stats for world {}", worldId);

            long voxelCount = voxelWorldService.getVoxelCount(worldId);
            WorldStats stats = new WorldStats(worldId, voxelCount);

            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            LOGGER.error("Failed to get world stats", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Batch save voxels
     */
    @PostMapping("/worlds/{worldId}/voxels/batch")
    public ResponseEntity<String> saveVoxelsBatch(@PathVariable String worldId, @RequestBody List<Voxel> voxels) {
        try {
            LOGGER.info("REST: Batch saving {} voxels in world {}", voxels.size(), worldId);

            int savedCount = voxelWorldService.saveVoxels(worldId, voxels);
            return ResponseEntity.ok("Batch save completed. " + savedCount + " voxels saved.");
        } catch (Exception e) {
            LOGGER.error("Failed to batch save voxels", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to batch save voxels: " + e.getMessage());
        }
    }

    /**
     * Check if voxel exists
     */
    @GetMapping("/worlds/{worldId}/voxels/{x}/{y}/{z}/exists")
    public ResponseEntity<Boolean> voxelExists(@PathVariable String worldId,
                                              @PathVariable int x,
                                              @PathVariable int y,
                                              @PathVariable int z) {
        try {
            boolean exists = voxelWorldService.voxelExists(worldId, x, y, z);
            return ResponseEntity.ok(exists);
        } catch (Exception e) {
            LOGGER.error("Failed to check voxel existence", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * World statistics DTO
     */
    public static class WorldStats {
        private String worldId;
        private long voxelCount;
        private LocalDateTime timestamp;

        public WorldStats(String worldId, long voxelCount) {
            this.worldId = worldId;
            this.voxelCount = voxelCount;
            this.timestamp = LocalDateTime.now();
        }

        // Getters and setters
        public String getWorldId() { return worldId; }
        public void setWorldId(String worldId) { this.worldId = worldId; }

        public long getVoxelCount() { return voxelCount; }
        public void setVoxelCount(long voxelCount) { this.voxelCount = voxelCount; }

        public LocalDateTime getTimestamp() { return timestamp; }
        public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    }
}
