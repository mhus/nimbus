package de.mhus.nimbus.worldlife.controller;

import de.mhus.nimbus.shared.character.CharacterType;
import de.mhus.nimbus.shared.avro.CharacterOperationMessage;
import de.mhus.nimbus.worldlife.entity.WorldCharacter;
import de.mhus.nimbus.worldlife.service.WorldLifeService;
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
 * REST Controller for World Life operations
 */
@RestController
@RequestMapping("/api/worldlife")
@CrossOrigin(origins = "*")
public class WorldLifeController {

    private static final Logger LOGGER = LoggerFactory.getLogger(WorldLifeController.class);

    private final WorldLifeService worldLifeService;

    @Autowired
    public WorldLifeController(WorldLifeService worldLifeService) {
        this.worldLifeService = worldLifeService;
    }

    /**
     * Get service information
     */
    @GetMapping("/info")
    public ResponseEntity<String> getServiceInfo() {
        try {
            LOGGER.debug("REST: Getting service info");
            String info = worldLifeService.getServiceInfo();
            return ResponseEntity.ok(info);
        } catch (Exception e) {
            LOGGER.error("Failed to get service info", e);
            return ResponseEntity.internalServerError().body("Service unavailable");
        }
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        try {
            boolean healthy = worldLifeService.isHealthy();
            if (healthy) {
                return ResponseEntity.ok("World Life Service is healthy");
            } else {
                return ResponseEntity.status(503).body("World Life Service is unhealthy");
            }
        } catch (Exception e) {
            LOGGER.error("Health check failed", e);
            return ResponseEntity.status(503).body("Health check failed");
        }
    }

    /**
     * Create a new character
     */
    @PostMapping("/worlds/{worldId}/characters")
    public ResponseEntity<WorldCharacter> createCharacter(@PathVariable String worldId,
                                                         @RequestBody CreateCharacterRequest request) {
        try {
            LOGGER.info("REST: Creating character of type {} at ({}, {}, {}) in world {}",
                       request.getCharacterType(), request.getX(), request.getY(), request.getZ(), worldId);

            WorldCharacter character = worldLifeService.createCharacter(
                worldId, request.getCharacterType(), request.getX(), request.getY(), request.getZ(), request.getName());

            // Set displayName if provided
            if (request.getDisplayName() != null) {
                worldLifeService.updateCharacterInfo(character.getId(), request.getName(), request.getDisplayName(), null);
                // Reload the character to get the updated version
                character = worldLifeService.getCharacter(character.getId()).orElse(character);
            }

            return ResponseEntity.status(HttpStatus.CREATED).body(character);
        } catch (Exception e) {
            LOGGER.error("Failed to create character", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get a character by ID
     */
    @GetMapping("/characters/{characterId}")
    public ResponseEntity<WorldCharacter> getCharacter(@PathVariable Long characterId) {
        try {
            LOGGER.debug("REST: Getting character with ID {}", characterId);

            Optional<WorldCharacter> character = worldLifeService.getCharacter(characterId);

            if (character.isPresent()) {
                return ResponseEntity.ok(character.get());
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            LOGGER.error("Failed to get character", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get all characters in a world
     */
    @GetMapping("/worlds/{worldId}/characters")
    public ResponseEntity<List<WorldCharacter>> getCharactersInWorld(@PathVariable String worldId,
                                                                   @RequestParam(required = false) CharacterType type,
                                                                   @RequestParam(defaultValue = "false") boolean activeOnly) {
        try {
            LOGGER.debug("REST: Getting characters in world {} (type: {}, activeOnly: {})", worldId, type, activeOnly);

            List<WorldCharacter> characters;
            if (type != null) {
                characters = worldLifeService.getCharactersByType(worldId, type);
            } else if (activeOnly) {
                characters = worldLifeService.getActiveCharacters(worldId);
            } else {
                characters = worldLifeService.getCharactersInWorld(worldId);
            }

            return ResponseEntity.ok(characters);
        } catch (Exception e) {
            LOGGER.error("Failed to get characters in world", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Update character position
     */
    @PutMapping("/characters/{characterId}/position")
    public ResponseEntity<WorldCharacter> updateCharacterPosition(@PathVariable Long characterId,
                                                                 @RequestBody PositionUpdateRequest request) {
        try {
            LOGGER.info("REST: Updating position for character {} to ({}, {}, {})",
                       characterId, request.getX(), request.getY(), request.getZ());

            Optional<WorldCharacter> updated = worldLifeService.updateCharacterPosition(
                characterId, request.getX(), request.getY(), request.getZ());

            if (updated.isPresent()) {
                return ResponseEntity.ok(updated.get());
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            LOGGER.error("Failed to update character position", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Update character health
     */
    @PutMapping("/characters/{characterId}/health")
    public ResponseEntity<WorldCharacter> updateCharacterHealth(@PathVariable Long characterId,
                                                               @RequestBody HealthUpdateRequest request) {
        try {
            LOGGER.info("REST: Updating health for character {} to {}", characterId, request.getHealth());

            Optional<WorldCharacter> updated = worldLifeService.updateCharacterHealth(characterId, request.getHealth());

            if (updated.isPresent()) {
                return ResponseEntity.ok(updated.get());
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            LOGGER.error("Failed to update character health", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Update character info
     */
    @PutMapping("/characters/{characterId}/info")
    public ResponseEntity<WorldCharacter> updateCharacterInfo(@PathVariable Long characterId,
                                                             @RequestBody InfoUpdateRequest request) {
        try {
            LOGGER.info("REST: Updating info for character {}", characterId);

            Optional<WorldCharacter> updated = worldLifeService.updateCharacterInfo(
                characterId, request.getName(), request.getDisplayName(), request.getDescription());

            if (updated.isPresent()) {
                return ResponseEntity.ok(updated.get());
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            LOGGER.error("Failed to update character info", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Delete a character
     */
    @DeleteMapping("/characters/{characterId}")
    public ResponseEntity<String> deleteCharacter(@PathVariable Long characterId) {
        try {
            LOGGER.info("REST: Deleting character {}", characterId);

            boolean deleted = worldLifeService.deleteCharacter(characterId);

            if (deleted) {
                return ResponseEntity.ok("Character deleted successfully");
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            LOGGER.error("Failed to delete character", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to delete character: " + e.getMessage());
        }
    }

    /**
     * Get characters in coordinate range
     */
    @GetMapping("/worlds/{worldId}/characters/range")
    public ResponseEntity<List<WorldCharacter>> getCharactersInRange(@PathVariable String worldId,
                                                                   @RequestParam double minX, @RequestParam double maxX,
                                                                   @RequestParam double minY, @RequestParam double maxY,
                                                                   @RequestParam double minZ, @RequestParam double maxZ) {
        try {
            LOGGER.debug("REST: Getting characters in range ({},{},{}) to ({},{},{}) in world {}",
                        minX, minY, minZ, maxX, maxY, maxZ, worldId);

            List<WorldCharacter> characters = worldLifeService.getCharactersInRange(
                worldId, minX, maxX, minY, maxY, minZ, maxZ);

            return ResponseEntity.ok(characters);
        } catch (Exception e) {
            LOGGER.error("Failed to get characters in range", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get characters within radius
     */
    @GetMapping("/worlds/{worldId}/characters/radius")
    public ResponseEntity<List<WorldCharacter>> getCharactersInRadius(@PathVariable String worldId,
                                                                    @RequestParam double centerX,
                                                                    @RequestParam double centerY,
                                                                    @RequestParam double centerZ,
                                                                    @RequestParam double radius) {
        try {
            LOGGER.debug("REST: Getting characters within radius {} of ({}, {}, {}) in world {}",
                        radius, centerX, centerY, centerZ, worldId);

            List<WorldCharacter> characters = worldLifeService.getCharactersInRadius(
                worldId, centerX, centerY, centerZ, radius);

            return ResponseEntity.ok(characters);
        } catch (Exception e) {
            LOGGER.error("Failed to get characters in radius", e);
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

            long totalCharacters = worldLifeService.getCharactersInWorld(worldId).size();
            long activeCharacters = worldLifeService.getActiveCharacterCount(worldId);
            long players = worldLifeService.getCharacterCountByType(worldId, CharacterType.PLAYER);
            long npcs = worldLifeService.getCharacterCountByType(worldId, CharacterType.NPC);
            long flora = worldLifeService.getCharacterCountByType(worldId, CharacterType.FLORA);
            long fauna = worldLifeService.getCharacterCountByType(worldId, CharacterType.FAUNA);

            WorldStats stats = new WorldStats(worldId, totalCharacters, activeCharacters, players, npcs, flora, fauna);

            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            LOGGER.error("Failed to get world stats", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Clear all characters in a world
     */
    @DeleteMapping("/worlds/{worldId}/characters")
    public ResponseEntity<String> clearWorldCharacters(@PathVariable String worldId) {
        try {
            LOGGER.info("REST: Clearing all characters in world {}", worldId);

            long deletedCount = worldLifeService.clearWorldCharacters(worldId);
            return ResponseEntity.ok("Cleared " + deletedCount + " characters from world " + worldId);
        } catch (Exception e) {
            LOGGER.error("Failed to clear world characters", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to clear characters: " + e.getMessage());
        }
    }

    // DTOs for requests
    public static class CreateCharacterRequest {
        private CharacterType characterType;
        private double x, y, z;
        private String name;
        private String displayName;

        // Getters and setters
        public CharacterType getCharacterType() { return characterType; }
        public void setCharacterType(CharacterType characterType) { this.characterType = characterType; }
        public double getX() { return x; }
        public void setX(double x) { this.x = x; }
        public double getY() { return y; }
        public void setY(double y) { this.y = y; }
        public double getZ() { return z; }
        public void setZ(double z) { this.z = z; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDisplayName() { return displayName; }
        public void setDisplayName(String displayName) { this.displayName = displayName; }
    }

    public static class PositionUpdateRequest {
        private double x, y, z;

        public double getX() { return x; }
        public void setX(double x) { this.x = x; }
        public double getY() { return y; }
        public void setY(double y) { this.y = y; }
        public double getZ() { return z; }
        public void setZ(double z) { this.z = z; }
    }

    public static class HealthUpdateRequest {
        private int health;

        public int getHealth() { return health; }
        public void setHealth(int health) { this.health = health; }
    }

    public static class InfoUpdateRequest {
        private String name;
        private String displayName;
        private String description;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDisplayName() { return displayName; }
        public void setDisplayName(String displayName) { this.displayName = displayName; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
    }

    public static class WorldStats {
        private String worldId;
        private long totalCharacters;
        private long activeCharacters;
        private long players;
        private long npcs;
        private long flora;
        private long fauna;
        private LocalDateTime timestamp;

        public WorldStats(String worldId, long totalCharacters, long activeCharacters,
                         long players, long npcs, long flora, long fauna) {
            this.worldId = worldId;
            this.totalCharacters = totalCharacters;
            this.activeCharacters = activeCharacters;
            this.players = players;
            this.npcs = npcs;
            this.flora = flora;
            this.fauna = fauna;
            this.timestamp = LocalDateTime.now();
        }

        // Getters and setters
        public String getWorldId() { return worldId; }
        public void setWorldId(String worldId) { this.worldId = worldId; }
        public long getTotalCharacters() { return totalCharacters; }
        public void setTotalCharacters(long totalCharacters) { this.totalCharacters = totalCharacters; }
        public long getActiveCharacters() { return activeCharacters; }
        public void setActiveCharacters(long activeCharacters) { this.activeCharacters = activeCharacters; }
        public long getPlayers() { return players; }
        public void setPlayers(long players) { this.players = players; }
        public long getNpcs() { return npcs; }
        public void setNpcs(long npcs) { this.npcs = npcs; }
        public long getFlora() { return flora; }
        public void setFlora(long flora) { this.flora = flora; }
        public long getFauna() { return fauna; }
        public void setFauna(long fauna) { this.fauna = fauna; }
        public LocalDateTime getTimestamp() { return timestamp; }
        public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    }
}
