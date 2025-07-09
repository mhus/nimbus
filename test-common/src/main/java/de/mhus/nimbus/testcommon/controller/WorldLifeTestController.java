package de.mhus.nimbus.testcommon.controller;

import de.mhus.nimbus.common.client.WorldLifeClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.CompletableFuture;

/**
 * REST Controller for testing WorldLifeClient functionality
 */
@RestController
@RequestMapping("/api/test/world-life")
public class WorldLifeTestController {

    private static final Logger LOGGER = LoggerFactory.getLogger(WorldLifeTestController.class);

    private final WorldLifeClient worldLifeClient;

    @Autowired
    public WorldLifeTestController(WorldLifeClient worldLifeClient) {
        this.worldLifeClient = worldLifeClient;
    }

    /**
     * Test character lookup by ID
     */
    @GetMapping("/character/{characterId}")
    public ResponseEntity<String> lookupCharacterById(@PathVariable Long characterId) {
        try {
            LOGGER.info("REST: Looking up character by ID: {}", characterId);
            CompletableFuture<Void> future = worldLifeClient.lookupCharacterById(characterId);
            return ResponseEntity.ok("Character lookup request sent for ID: " + characterId);
        } catch (Exception e) {
            LOGGER.error("Failed to lookup character by ID", e);
            return ResponseEntity.internalServerError().body("Failed to lookup character: " + e.getMessage());
        }
    }

    /**
     * Test character lookup by name
     */
    @GetMapping("/character/name/{characterName}")
    public ResponseEntity<String> lookupCharacterByName(@PathVariable String characterName) {
        try {
            LOGGER.info("REST: Looking up character by name: {}", characterName);
            CompletableFuture<Void> future = worldLifeClient.lookupCharacterByName(characterName);
            return ResponseEntity.ok("Character lookup request sent for name: " + characterName);
        } catch (Exception e) {
            LOGGER.error("Failed to lookup character by name", e);
            return ResponseEntity.internalServerError().body("Failed to lookup character: " + e.getMessage());
        }
    }

    /**
     * Test character lookup by user ID
     */
    @GetMapping("/characters/user/{userId}")
    public ResponseEntity<String> lookupCharactersByUserId(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "true") boolean activeOnly) {
        try {
            LOGGER.info("REST: Looking up characters for user ID: {}, activeOnly: {}", userId, activeOnly);
            CompletableFuture<Void> future = worldLifeClient.lookupCharactersByUserId(userId, activeOnly);
            return ResponseEntity.ok("Characters lookup request sent for user ID: " + userId + " (activeOnly: " + activeOnly + ")");
        } catch (Exception e) {
            LOGGER.error("Failed to lookup characters by user ID", e);
            return ResponseEntity.internalServerError().body("Failed to lookup characters: " + e.getMessage());
        }
    }

    /**
     * Test character lookup by world ID
     */
    @GetMapping("/characters/world/{worldId}")
    public ResponseEntity<String> lookupCharactersByWorldId(
            @PathVariable String worldId,
            @RequestParam(defaultValue = "true") boolean activeOnly) {
        try {
            LOGGER.info("REST: Looking up characters for world ID: {}, activeOnly: {}", worldId, activeOnly);
            CompletableFuture<Void> future = worldLifeClient.lookupCharactersByWorldId(worldId, activeOnly);
            return ResponseEntity.ok("Characters lookup request sent for world ID: " + worldId + " (activeOnly: " + activeOnly + ")");
        } catch (Exception e) {
            LOGGER.error("Failed to lookup characters by world ID", e);
            return ResponseEntity.internalServerError().body("Failed to lookup characters: " + e.getMessage());
        }
    }
}
