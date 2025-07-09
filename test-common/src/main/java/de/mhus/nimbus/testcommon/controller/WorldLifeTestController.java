package de.mhus.nimbus.testcommon.controller;

import de.mhus.nimbus.common.client.WorldLifeClient;
import de.mhus.nimbus.shared.avro.PlayerCharacterLookupResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.CompletableFuture;
import java.util.List;

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
     * Test character lookup by ID with response
     */
    @GetMapping("/character/{characterId}")
    public ResponseEntity<String> lookupCharacterById(@PathVariable Long characterId) {
        try {
            LOGGER.info("Testing character lookup by ID: {}", characterId);

            CompletableFuture<PlayerCharacterLookupResponse> future = worldLifeClient.lookupCharacterByIdWithResponse(characterId);

            // Wait for the response
            PlayerCharacterLookupResponse response = future.get();

            String result = formatCharacterLookupResponse(response, "Character lookup by ID " + characterId);
            LOGGER.info("Character lookup by ID completed: {}", characterId);

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            String errorMessage = "Failed to lookup character by ID " + characterId + ": " + e.getMessage();
            LOGGER.error(errorMessage, e);
            return ResponseEntity.internalServerError().body(errorMessage);
        }
    }

    /**
     * Test character lookup by name with response
     */
    @GetMapping("/character/name/{characterName}")
    public ResponseEntity<String> lookupCharacterByName(@PathVariable String characterName) {
        try {
            LOGGER.info("Testing character lookup by name: {}", characterName);

            CompletableFuture<PlayerCharacterLookupResponse> future = worldLifeClient.lookupCharacterByNameWithResponse(characterName);

            // Wait for the response
            PlayerCharacterLookupResponse response = future.get();

            String result = formatCharacterLookupResponse(response, "Character lookup by name '" + characterName + "'");
            LOGGER.info("Character lookup by name completed: {}", characterName);

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            String errorMessage = "Failed to lookup character by name '" + characterName + "': " + e.getMessage();
            LOGGER.error(errorMessage, e);
            return ResponseEntity.internalServerError().body(errorMessage);
        }
    }

    /**
     * Test character lookup by user ID with response
     */
    @GetMapping("/character/user/{userId}")
    public ResponseEntity<String> lookupCharactersByUserId(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "true") boolean activeOnly) {
        try {
            LOGGER.info("Testing character lookup by user ID: {} (activeOnly: {})", userId, activeOnly);

            CompletableFuture<PlayerCharacterLookupResponse> future = worldLifeClient.lookupCharactersByUserIdWithResponse(userId, activeOnly);

            // Wait for the response
            PlayerCharacterLookupResponse response = future.get();

            String result = formatCharacterLookupResponse(response, "Character lookup by user ID " + userId + " (activeOnly: " + activeOnly + ")");
            LOGGER.info("Character lookup by user ID completed: {}", userId);

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            String errorMessage = "Failed to lookup characters by user ID " + userId + ": " + e.getMessage();
            LOGGER.error(errorMessage, e);
            return ResponseEntity.internalServerError().body(errorMessage);
        }
    }

    /**
     * Test character lookup by world ID
     */
    @GetMapping("/character/world/{worldId}")
    public ResponseEntity<String> lookupCharactersByWorldId(
            @PathVariable String worldId,
            @RequestParam(defaultValue = "true") boolean activeOnly) {
        try {
            LOGGER.info("Testing character lookup by world ID: {} (activeOnly: {})", worldId, activeOnly);

            CompletableFuture<Void> future = worldLifeClient.lookupCharactersByWorldId(worldId, activeOnly);

            // Wait for the response
            future.get();

            String result = "Character lookup by world ID '" + worldId + "' completed successfully (activeOnly: " + activeOnly + ")";
            LOGGER.info("Character lookup by world ID completed: {}", worldId);

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            String errorMessage = "Failed to lookup characters by world ID '" + worldId + "': " + e.getMessage();
            LOGGER.error(errorMessage, e);
            return ResponseEntity.internalServerError().body(errorMessage);
        }
    }

    /**
     * Test character lookup by planet
     */
    @GetMapping("/character/planet/{planetName}")
    public ResponseEntity<String> lookupCharactersByPlanet(
            @PathVariable String planetName,
            @RequestParam(defaultValue = "true") boolean activeOnly) {
        try {
            LOGGER.info("Testing character lookup by planet: {} (activeOnly: {})", planetName, activeOnly);

            CompletableFuture<Void> future = worldLifeClient.lookupCharactersByPlanet(planetName, activeOnly);

            // Wait for the response
            future.get();

            String result = "Character lookup by planet '" + planetName + "' completed successfully (activeOnly: " + activeOnly + ")";
            LOGGER.info("Character lookup by planet completed: {}", planetName);

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            String errorMessage = "Failed to lookup characters by planet '" + planetName + "': " + e.getMessage();
            LOGGER.error(errorMessage, e);
            return ResponseEntity.internalServerError().body(errorMessage);
        }
    }

    /**
     * Test extended character lookup with multiple criteria
     */
    @GetMapping("/character/search")
    public ResponseEntity<String> lookupCharacters(
            @RequestParam(required = false) Long characterId,
            @RequestParam(required = false) String characterName,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String currentPlanet,
            @RequestParam(required = false) String currentWorldId,
            @RequestParam(defaultValue = "true") boolean activeOnly) {
        try {
            LOGGER.info("Testing extended character lookup with multiple criteria");

            CompletableFuture<Void> future = worldLifeClient.lookupCharacters(
                characterId, characterName, userId, currentPlanet, currentWorldId, activeOnly);

            // Wait for the response
            future.get();

            StringBuilder result = new StringBuilder("Extended character lookup completed successfully with criteria: ");
            if (characterId != null) result.append("characterId=").append(characterId).append(" ");
            if (characterName != null) result.append("characterName='").append(characterName).append("' ");
            if (userId != null) result.append("userId=").append(userId).append(" ");
            if (currentPlanet != null) result.append("currentPlanet='").append(currentPlanet).append("' ");
            if (currentWorldId != null) result.append("currentWorldId='").append(currentWorldId).append("' ");
            result.append("activeOnly=").append(activeOnly);

            LOGGER.info("Extended character lookup completed");

            return ResponseEntity.ok(result.toString());

        } catch (Exception e) {
            String errorMessage = "Failed to perform extended character lookup: " + e.getMessage();
            LOGGER.error(errorMessage, e);
            return ResponseEntity.internalServerError().body(errorMessage);
        }
    }

    /**
     * Helper method to format PlayerCharacterLookupResponse as a readable string
     */
    private String formatCharacterLookupResponse(PlayerCharacterLookupResponse response, String operation) {
        if (response == null) {
            return operation + " completed, but no response received";
        }

        StringBuilder result = new StringBuilder(operation + " completed successfully");

        if (response.getStatus() != null) {
            result.append(" - Status: ").append(response.getStatus());
        }

        if (response.getErrorMessage() != null) {
            result.append(" | Error: ").append(response.getErrorMessage());
        }

        if (response.getCharacters() != null && !response.getCharacters().isEmpty()) {
            result.append(" | Found ").append(response.getCharacters().size()).append(" character(s):");

            List<de.mhus.nimbus.shared.avro.PlayerCharacterInfo> characters = response.getCharacters();
            for (int i = 0; i < Math.min(characters.size(), 3); i++) { // Show max 3 characters
                de.mhus.nimbus.shared.avro.PlayerCharacterInfo character = characters.get(i);
                result.append("\n  - ID: ").append(character.getId())
                      .append(", Name: ").append(character.getName());
                if (character.getCurrentPlanet() != null) {
                    result.append(", Planet: ").append(character.getCurrentPlanet());
                }
                if (character.getCurrentWorldId() != null) {
                    result.append(", World: ").append(character.getCurrentWorldId());
                }
                if (character.getCharacterClass() != null) {
                    result.append(", Class: ").append(character.getCharacterClass());
                }
                result.append(", Level: ").append(character.getLevel());
            }

            if (characters.size() > 3) {
                result.append("\n  ... and ").append(characters.size() - 3).append(" more");
            }
        } else {
            result.append(" | No characters found");
        }

        return result.toString();
    }
}
