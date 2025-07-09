package de.mhus.nimbus.testcommon.controller;

import de.mhus.nimbus.common.client.IdentityClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.CompletableFuture;

/**
 * REST Controller for testing IdentityClient functionality
 */
@RestController
@RequestMapping("/api/test/identity")
public class IdentityTestController {

    private static final Logger LOGGER = LoggerFactory.getLogger(IdentityTestController.class);

    private final IdentityClient identityClient;

    @Autowired
    public IdentityTestController(IdentityClient identityClient) {
        this.identityClient = identityClient;
    }

    /**
     * Test login request
     */
    @PostMapping("/login")
    public ResponseEntity<String> requestLogin(
            @RequestParam String username,
            @RequestParam String password,
            @RequestParam(required = false) String clientInfo) {
        try {
            LOGGER.info("REST: Requesting login for username: {}", username);
            CompletableFuture<Void> future;
            if (clientInfo != null) {
                future = identityClient.requestLogin(username, password, clientInfo);
            } else {
                future = identityClient.requestLogin(username, password);
            }
            return ResponseEntity.ok("Login request sent for username: " + username);
        } catch (Exception e) {
            LOGGER.error("Failed to request login", e);
            return ResponseEntity.internalServerError().body("Failed to request login: " + e.getMessage());
        }
    }

    /**
     * Test user lookup by ID
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<String> lookupUserById(@PathVariable Long userId) {
        try {
            LOGGER.info("REST: Looking up user by ID: {}", userId);
            CompletableFuture<Void> future = identityClient.lookupUserById(userId);
            return ResponseEntity.ok("User lookup request sent for ID: " + userId);
        } catch (Exception e) {
            LOGGER.error("Failed to lookup user by ID", e);
            return ResponseEntity.internalServerError().body("Failed to lookup user: " + e.getMessage());
        }
    }

    /**
     * Test user lookup by username
     */
    @GetMapping("/user/username/{username}")
    public ResponseEntity<String> lookupUserByUsername(@PathVariable String username) {
        try {
            LOGGER.info("REST: Looking up user by username: {}", username);
            CompletableFuture<Void> future = identityClient.lookupUserByUsername(username);
            return ResponseEntity.ok("User lookup request sent for username: " + username);
        } catch (Exception e) {
            LOGGER.error("Failed to lookup user by username", e);
            return ResponseEntity.internalServerError().body("Failed to lookup user: " + e.getMessage());
        }
    }

    /**
     * Test user lookup by email
     */
    @GetMapping("/user/email/{email}")
    public ResponseEntity<String> lookupUserByEmail(@PathVariable String email) {
        try {
            LOGGER.info("REST: Looking up user by email: {}", email);
            CompletableFuture<Void> future = identityClient.lookupUserByEmail(email);
            return ResponseEntity.ok("User lookup request sent for email: " + email);
        } catch (Exception e) {
            LOGGER.error("Failed to lookup user by email", e);
            return ResponseEntity.internalServerError().body("Failed to lookup user: " + e.getMessage());
        }
    }

    /**
     * Test character lookup by ID
     */
    @GetMapping("/character/{characterId}")
    public ResponseEntity<String> lookupCharacterById(@PathVariable Long characterId) {
        try {
            LOGGER.info("REST: Looking up character by ID: {}", characterId);
            CompletableFuture<Void> future = identityClient.lookupCharacterById(characterId);
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
            CompletableFuture<Void> future = identityClient.lookupCharacterByName(characterName);
            return ResponseEntity.ok("Character lookup request sent for name: " + characterName);
        } catch (Exception e) {
            LOGGER.error("Failed to lookup character by name", e);
            return ResponseEntity.internalServerError().body("Failed to lookup character: " + e.getMessage());
        }
    }

    /**
     * Test characters lookup by user ID
     */
    @GetMapping("/characters/user/{userId}")
    public ResponseEntity<String> lookupCharactersByUserId(@PathVariable Long userId) {
        try {
            LOGGER.info("REST: Looking up characters for user ID: {}", userId);
            CompletableFuture<Void> future = identityClient.lookupCharactersByUserId(userId);
            return ResponseEntity.ok("Characters lookup request sent for user ID: " + userId);
        } catch (Exception e) {
            LOGGER.error("Failed to lookup characters by user ID", e);
            return ResponseEntity.internalServerError().body("Failed to lookup characters: " + e.getMessage());
        }
    }

    /**
     * Test public key request
     */
    @GetMapping("/public-key")
    public ResponseEntity<String> requestPublicKey() {
        try {
            LOGGER.info("REST: Requesting public key");
            CompletableFuture<Void> future = identityClient.requestPublicKey();
            return ResponseEntity.ok("Public key request sent");
        } catch (Exception e) {
            LOGGER.error("Failed to request public key", e);
            return ResponseEntity.internalServerError().body("Failed to request public key: " + e.getMessage());
        }
    }
}
