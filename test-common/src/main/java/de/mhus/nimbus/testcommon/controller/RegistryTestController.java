package de.mhus.nimbus.testcommon.controller;

import de.mhus.nimbus.common.client.RegistryClient;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
public class RegistryTestController {

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
            log.info("Testing planet registration for: {}", planetName);

            CompletableFuture<Void> future = registryClient.registerPlanet(planetName, description, galaxy, sector);

            // Wait for the response and handle the result
            future.get(); // This blocks until the response is received

            String result = "Planet '" + planetName + "' successfully registered";
            log.info("Planet registration completed: {}", result);

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            String errorMessage = "Failed to register planet '" + planetName + "': " + e.getMessage();
            log.error(errorMessage, e);
            return ResponseEntity.internalServerError().body(errorMessage);
        }
    }

    /**
     * Test planet unregistration
     */
    @DeleteMapping("/planet")
    public ResponseEntity<String> unregisterPlanet(
            @RequestParam String planetName,
            @RequestParam(required = false) String reason) {

        try {
            log.info("Testing planet unregistration for: {}", planetName);

            CompletableFuture<Void> future = registryClient.unregisterPlanet(planetName, reason);

            // Wait for the response and handle the result
            future.get();

            String result = "Planet '" + planetName + "' successfully unregistered";
            log.info("Planet unregistration completed: {}", result);

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            String errorMessage = "Failed to unregister planet '" + planetName + "': " + e.getMessage();
            log.error(errorMessage, e);
            return ResponseEntity.internalServerError().body(errorMessage);
        }
    }

    /**
     * Test planet lookup
     */
    @GetMapping("/planet")
    public ResponseEntity<String> lookupPlanet(
            @RequestParam String planetName,
            @RequestParam(required = false) String worldName) {

        try {
            log.info("Testing planet lookup for: {}", planetName);

            CompletableFuture<Void> future;
            if (worldName != null) {
                future = registryClient.lookupPlanet(planetName, worldName);
            } else {
                future = registryClient.lookupPlanetByName(planetName);
            }

            // Wait for the response and handle the result
            future.get();

            String result = "Planet lookup for '" + planetName + "' completed successfully";
            if (worldName != null) {
                result += " (with world: " + worldName + ")";
            }
            log.info("Planet lookup completed: {}", result);

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            String errorMessage = "Failed to lookup planet '" + planetName + "': " + e.getMessage();
            log.error(errorMessage, e);
            return ResponseEntity.internalServerError().body(errorMessage);
        }
    }
}
