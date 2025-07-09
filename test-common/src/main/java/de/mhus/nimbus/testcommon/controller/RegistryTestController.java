package de.mhus.nimbus.testcommon.controller;

import de.mhus.nimbus.common.client.RegistryClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.CompletableFuture;

/**
 * REST Controller for testing RegistryClient functionality
 */
@RestController
@RequestMapping("/api/test/registry")
public class RegistryTestController {

    private static final Logger LOGGER = LoggerFactory.getLogger(RegistryTestController.class);

    private final RegistryClient registryClient;

    @Autowired
    public RegistryTestController(RegistryClient registryClient) {
        this.registryClient = registryClient;
    }

    /**
     * Test planet registration
     */
    @PostMapping("/planet")
    public ResponseEntity<String> registerPlanet(
            @RequestParam String planetName,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String galaxy,
            @RequestParam(required = false) String sector) {
        try {
            LOGGER.info("REST: Registering planet: {}", planetName);
            CompletableFuture<Void> future;
            if (description != null || galaxy != null || sector != null) {
                future = registryClient.registerPlanet(planetName, description, galaxy, sector);
            } else {
                future = registryClient.registerPlanet(planetName);
            }
            return ResponseEntity.ok("Planet registration request sent for: " + planetName);
        } catch (Exception e) {
            LOGGER.error("Failed to register planet", e);
            return ResponseEntity.internalServerError().body("Failed to register planet: " + e.getMessage());
        }
    }

    /**
     * Test planet unregistration
     */
    @DeleteMapping("/planet/{planetName}")
    public ResponseEntity<String> unregisterPlanet(
            @PathVariable String planetName,
            @RequestParam(required = false) String reason) {
        try {
            LOGGER.info("REST: Unregistering planet: {}", planetName);
            CompletableFuture<Void> future;
            if (reason != null) {
                future = registryClient.unregisterPlanet(planetName, reason);
            } else {
                future = registryClient.unregisterPlanet(planetName);
            }
            return ResponseEntity.ok("Planet unregistration request sent for: " + planetName);
        } catch (Exception e) {
            LOGGER.error("Failed to unregister planet", e);
            return ResponseEntity.internalServerError().body("Failed to unregister planet: " + e.getMessage());
        }
    }

    /**
     * Test planet lookup by name
     */
    @GetMapping("/planet/{planetName}")
    public ResponseEntity<String> lookupPlanetByName(@PathVariable String planetName) {
        try {
            LOGGER.info("REST: Looking up planet by name: {}", planetName);
            CompletableFuture<Void> future = registryClient.lookupPlanetByName(planetName);
            return ResponseEntity.ok("Planet lookup request sent for: " + planetName);
        } catch (Exception e) {
            LOGGER.error("Failed to lookup planet by name", e);
            return ResponseEntity.internalServerError().body("Failed to lookup planet: " + e.getMessage());
        }
    }

    /**
     * Test world registration
     */
    @PostMapping("/world")
    public ResponseEntity<String> registerWorld(
            @RequestParam String worldId,
            @RequestParam String worldName,
            @RequestParam String planetName,
            @RequestParam String managementUrl,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String worldType) {
        try {
            LOGGER.info("REST: Registering world: {} on planet: {}", worldId, planetName);
            CompletableFuture<Void> future;
            if (description != null || worldType != null) {
                future = registryClient.registerWorld(worldId, worldName, planetName, managementUrl, description, worldType);
            } else {
                future = registryClient.registerWorld(worldId, worldName, planetName, managementUrl);
            }
            return ResponseEntity.ok("World registration request sent for: " + worldId);
        } catch (Exception e) {
            LOGGER.error("Failed to register world", e);
            return ResponseEntity.internalServerError().body("Failed to register world: " + e.getMessage());
        }
    }

    /**
     * Test world unregistration
     */
    @DeleteMapping("/world/{worldId}")
    public ResponseEntity<String> unregisterWorld(
            @PathVariable String worldId,
            @RequestParam(required = false) String planetName,
            @RequestParam(required = false) String reason) {
        try {
            LOGGER.info("REST: Unregistering world: {}", worldId);
            CompletableFuture<Void> future;
            if (planetName != null && reason != null) {
                future = registryClient.unregisterWorld(worldId, planetName, reason);
            } else {
                future = registryClient.unregisterWorld(worldId);
            }
            return ResponseEntity.ok("World unregistration request sent for: " + worldId);
        } catch (Exception e) {
            LOGGER.error("Failed to unregister world", e);
            return ResponseEntity.internalServerError().body("Failed to unregister world: " + e.getMessage());
        }
    }
}
