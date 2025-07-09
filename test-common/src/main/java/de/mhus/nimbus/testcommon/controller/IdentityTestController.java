package de.mhus.nimbus.testcommon.controller;

import de.mhus.nimbus.common.client.IdentityClient;
import de.mhus.nimbus.shared.avro.LoginResponse;
import de.mhus.nimbus.shared.avro.UserLookupResponse;
import de.mhus.nimbus.shared.avro.PlayerCharacterLookupResponse;
import de.mhus.nimbus.shared.avro.PublicKeyResponse;
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
     * Test login request with response handling
     */
    @PostMapping("/login")
    public ResponseEntity<String> requestLogin(
            @RequestParam String username,
            @RequestParam String password,
            @RequestParam(required = false) String clientInfo) {
        try {
            LOGGER.info("REST: Requesting login for username: {}", username);
            CompletableFuture<LoginResponse> future;
            if (clientInfo != null) {
                future = identityClient.requestLogin(username, password, clientInfo);
            } else {
                future = identityClient.requestLogin(username, password);
            }

            // Handle the response asynchronously
            future.thenAccept(response -> {
                LOGGER.info("Login response received for user '{}': status={}", username, response.getStatus());
            }).exceptionally(throwable -> {
                LOGGER.error("Login failed for user '{}': {}", username, throwable.getMessage());
                return null;
            });

            return ResponseEntity.ok("Login request sent for username: " + username);
        } catch (Exception e) {
            LOGGER.error("Failed to request login", e);
            return ResponseEntity.internalServerError().body("Failed to request login: " + e.getMessage());
        }
    }

    /**
     * Test user lookup by ID with response handling
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<String> lookupUserById(@PathVariable Long userId) {
        try {
            LOGGER.info("REST: Looking up user by ID: {}", userId);
            CompletableFuture<UserLookupResponse> future = identityClient.lookupUserById(userId);

            // Handle the response asynchronously
            future.thenAccept(response -> {
                LOGGER.info("User lookup response received for ID {}: status={}", userId, response.getStatus());
                if (response.getUser() != null) {
                    LOGGER.info("Found user: {}", response.getUser().getUsername());
                }
            }).exceptionally(throwable -> {
                LOGGER.error("User lookup failed for ID {}: {}", userId, throwable.getMessage());
                return null;
            });

            return ResponseEntity.ok("User lookup request sent for ID: " + userId);
        } catch (Exception e) {
            LOGGER.error("Failed to lookup user by ID", e);
            return ResponseEntity.internalServerError().body("Failed to lookup user: " + e.getMessage());
        }
    }

    /**
     * Test user lookup by username with response handling
     */
    @GetMapping("/user/username/{username}")
    public ResponseEntity<String> lookupUserByUsername(@PathVariable String username) {
        try {
            LOGGER.info("REST: Looking up user by username: {}", username);
            CompletableFuture<UserLookupResponse> future = identityClient.lookupUserByUsername(username);

            // Handle the response asynchronously
            future.thenAccept(response -> {
                LOGGER.info("User lookup response received for username '{}': status={}", username, response.getStatus());
                if (response.getUser() != null) {
                    LOGGER.info("Found user: {} (ID: {})", response.getUser().getUsername(), response.getUser().getId());
                }
            }).exceptionally(throwable -> {
                LOGGER.error("User lookup failed for username '{}': {}", username, throwable.getMessage());
                return null;
            });

            return ResponseEntity.ok("User lookup request sent for username: " + username);
        } catch (Exception e) {
            LOGGER.error("Failed to lookup user by username", e);
            return ResponseEntity.internalServerError().body("Failed to lookup user: " + e.getMessage());
        }
    }

    /**
     * Test user lookup by email with response handling
     */
    @GetMapping("/user/email/{email}")
    public ResponseEntity<String> lookupUserByEmail(@PathVariable String email) {
        try {
            LOGGER.info("REST: Looking up user by email: {}", email);
            CompletableFuture<UserLookupResponse> future = identityClient.lookupUserByEmail(email);

            // Handle the response asynchronously
            future.thenAccept(response -> {
                LOGGER.info("User lookup response received for email '{}': status={}", email, response.getStatus());
                if (response.getUser() != null) {
                    LOGGER.info("Found user: {} (ID: {})", response.getUser().getUsername(), response.getUser().getId());
                }
            }).exceptionally(throwable -> {
                LOGGER.error("User lookup failed for email '{}': {}", email, throwable.getMessage());
                return null;
            });

            return ResponseEntity.ok("User lookup request sent for email: " + email);
        } catch (Exception e) {
            LOGGER.error("Failed to lookup user by email", e);
            return ResponseEntity.internalServerError().body("Failed to lookup user: " + e.getMessage());
        }
    }

    /**
     * Test character lookup by ID with response handling
     */
    @GetMapping("/character/{characterId}")
    public ResponseEntity<String> lookupCharacterById(@PathVariable Long characterId) {
        try {
            LOGGER.info("REST: Looking up character by ID: {}", characterId);
            CompletableFuture<PlayerCharacterLookupResponse> future = identityClient.lookupCharacterById(characterId);

            // Handle the response asynchronously
            future.thenAccept(response -> {
                LOGGER.info("Character lookup response received for ID {}: status={}", characterId, response.getStatus());
                LOGGER.info("Found {} character(s)", response.getCharacters().size());
            }).exceptionally(throwable -> {
                LOGGER.error("Character lookup failed for ID {}: {}", characterId, throwable.getMessage());
                return null;
            });

            return ResponseEntity.ok("Character lookup request sent for ID: " + characterId);
        } catch (Exception e) {
            LOGGER.error("Failed to lookup character by ID", e);
            return ResponseEntity.internalServerError().body("Failed to lookup character: " + e.getMessage());
        }
    }

    /**
     * Test character lookup by name with response handling
     */
    @GetMapping("/character/name/{characterName}")
    public ResponseEntity<String> lookupCharacterByName(@PathVariable String characterName) {
        try {
            LOGGER.info("REST: Looking up character by name: {}", characterName);
            CompletableFuture<PlayerCharacterLookupResponse> future = identityClient.lookupCharacterByName(characterName);

            // Handle the response asynchronously
            future.thenAccept(response -> {
                LOGGER.info("Character lookup response received for name '{}': status={}", characterName, response.getStatus());
                LOGGER.info("Found {} character(s)", response.getCharacters().size());
            }).exceptionally(throwable -> {
                LOGGER.error("Character lookup failed for name '{}': {}", characterName, throwable.getMessage());
                return null;
            });

            return ResponseEntity.ok("Character lookup request sent for name: " + characterName);
        } catch (Exception e) {
            LOGGER.error("Failed to lookup character by name", e);
            return ResponseEntity.internalServerError().body("Failed to lookup character: " + e.getMessage());
        }
    }

    /**
     * Test characters lookup by user ID with response handling
     */
    @GetMapping("/characters/user/{userId}")
    public ResponseEntity<String> lookupCharactersByUserId(@PathVariable Long userId) {
        try {
            LOGGER.info("REST: Looking up characters for user ID: {}", userId);
            CompletableFuture<PlayerCharacterLookupResponse> future = identityClient.lookupCharactersByUserId(userId);

            // Handle the response asynchronously
            future.thenAccept(response -> {
                LOGGER.info("Characters lookup response received for user ID {}: status={}", userId, response.getStatus());
                LOGGER.info("Found {} character(s) for user", response.getCharacters().size());
                response.getCharacters().forEach(character ->
                    LOGGER.info("Character: {} (ID: {}, Level: {})",
                               character.getName(), character.getId(), character.getLevel())
                );
            }).exceptionally(throwable -> {
                LOGGER.error("Characters lookup failed for user ID {}: {}", userId, throwable.getMessage());
                return null;
            });

            return ResponseEntity.ok("Characters lookup request sent for user ID: " + userId);
        } catch (Exception e) {
            LOGGER.error("Failed to lookup characters by user ID", e);
            return ResponseEntity.internalServerError().body("Failed to lookup characters: " + e.getMessage());
        }
    }

    /**
     * Test public key request with response handling
     */
    @GetMapping("/public-key")
    public ResponseEntity<String> requestPublicKey() {
        try {
            LOGGER.info("REST: Requesting public key");
            CompletableFuture<PublicKeyResponse> future = identityClient.requestPublicKey();

            // Handle the response asynchronously
            future.thenAccept(response -> {
                LOGGER.info("Public key response received: status={}", response.getStatus());
                if (response.getPublicKey() != null) {
                    LOGGER.info("Public key type: {}, algorithm: {}", response.getKeyType(), response.getAlgorithm());
                }
            }).exceptionally(throwable -> {
                LOGGER.error("Public key request failed: {}", throwable.getMessage());
                return null;
            });

            return ResponseEntity.ok("Public key request sent");
        } catch (Exception e) {
            LOGGER.error("Failed to request public key", e);
            return ResponseEntity.internalServerError().body("Failed to request public key: " + e.getMessage());
        }
    }
}
