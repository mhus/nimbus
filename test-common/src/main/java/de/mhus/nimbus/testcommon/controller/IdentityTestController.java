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

import java.util.List;
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

            // Wait for the response synchronously with timeout
            LoginResponse response = future.get(30, java.util.concurrent.TimeUnit.SECONDS);

            LOGGER.info("Login response received for user '{}': status={}", username, response.getStatus());

            // Return the response as a formatted string
            StringBuilder result = new StringBuilder();
            result.append("Login Response:\n");
            result.append("Status: ").append(response.getStatus()).append("\n");
            result.append("Request ID: ").append(response.getRequestId()).append("\n");
            if (response.getUser() != null) {
                result.append("User ID: ").append(response.getUser().getId()).append("\n");
                result.append("Username: ").append(response.getUser().getUsername()).append("\n");
                result.append("Email: ").append(response.getUser().getEmail()).append("\n");
                if (response.getUser().getFirstName() != null) {
                    result.append("First Name: ").append(response.getUser().getFirstName()).append("\n");
                }
                if (response.getUser().getLastName() != null) {
                    result.append("Last Name: ").append(response.getUser().getLastName()).append("\n");
                }
            }
            if (response.getToken() != null) {
                result.append("Token: ").append(response.getToken()).append("\n");

                // Extrahiere und zeige Character-Namen aus dem Token
                List<String> characterNames = identityClient.extractCharacterNamesFromToken(response.getToken());
                if (!characterNames.isEmpty()) {
                    result.append("Identity Characters: ").append(String.join(", ", characterNames)).append("\n");
                } else {
                    result.append("Identity Characters: None\n");
                }
            }
            if (response.getExpiresAt() != null) {
                result.append("Expires At: ").append(response.getExpiresAt()).append("\n");
            }
            if (response.getErrorMessage() != null) {
                result.append("Error Message: ").append(response.getErrorMessage()).append("\n");
            }

            return ResponseEntity.ok(result.toString());

        } catch (java.util.concurrent.TimeoutException e) {
            LOGGER.error("Login request timed out for user '{}'", username);
            return ResponseEntity.status(408).body("Login request timed out after 30 seconds");
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

            // Wait for the response synchronously with timeout
            UserLookupResponse response = future.get(30, java.util.concurrent.TimeUnit.SECONDS);

            LOGGER.info("User lookup response received for ID {}: status={}", userId, response.getStatus());

            // Return the response as a formatted string
            StringBuilder result = new StringBuilder();
            result.append("User Lookup Response:\n");
            result.append("Status: ").append(response.getStatus()).append("\n");
            result.append("Request ID: ").append(response.getRequestId()).append("\n");
            if (response.getUser() != null) {
                result.append("User ID: ").append(response.getUser().getId()).append("\n");
                result.append("Username: ").append(response.getUser().getUsername()).append("\n");
                result.append("Email: ").append(response.getUser().getEmail()).append("\n");
                result.append("Created: ").append(response.getUser().getCreatedAt()).append("\n");
            }
            if (response.getErrorMessage() != null) {
                result.append("Error Message: ").append(response.getErrorMessage()).append("\n");
            }

            return ResponseEntity.ok(result.toString());

        } catch (java.util.concurrent.TimeoutException e) {
            LOGGER.error("User lookup request timed out for ID {}", userId);
            return ResponseEntity.status(408).body("User lookup request timed out after 30 seconds");
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

            // Wait for the response synchronously with timeout
            UserLookupResponse response = future.get(30, java.util.concurrent.TimeUnit.SECONDS);

            LOGGER.info("User lookup response received for username '{}': status={}", username, response.getStatus());

            // Return the response as a formatted string
            StringBuilder result = new StringBuilder();
            result.append("User Lookup Response:\n");
            result.append("Status: ").append(response.getStatus()).append("\n");
            result.append("Request ID: ").append(response.getRequestId()).append("\n");
            if (response.getUser() != null) {
                result.append("User ID: ").append(response.getUser().getId()).append("\n");
                result.append("Username: ").append(response.getUser().getUsername()).append("\n");
                result.append("Email: ").append(response.getUser().getEmail()).append("\n");
                result.append("Created: ").append(response.getUser().getCreatedAt()).append("\n");
            }
            if (response.getErrorMessage() != null) {
                result.append("Error Message: ").append(response.getErrorMessage()).append("\n");
            }

            return ResponseEntity.ok(result.toString());

        } catch (java.util.concurrent.TimeoutException e) {
            LOGGER.error("User lookup request timed out for username '{}'", username);
            return ResponseEntity.status(408).body("User lookup request timed out after 30 seconds");
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

            // Wait for the response synchronously with timeout
            UserLookupResponse response = future.get(30, java.util.concurrent.TimeUnit.SECONDS);

            LOGGER.info("User lookup response received for email '{}': status={}", email, response.getStatus());

            // Return the response as a formatted string
            StringBuilder result = new StringBuilder();
            result.append("User Lookup Response:\n");
            result.append("Status: ").append(response.getStatus()).append("\n");
            result.append("Request ID: ").append(response.getRequestId()).append("\n");
            if (response.getUser() != null) {
                result.append("User ID: ").append(response.getUser().getId()).append("\n");
                result.append("Username: ").append(response.getUser().getUsername()).append("\n");
                result.append("Email: ").append(response.getUser().getEmail()).append("\n");
                result.append("Created: ").append(response.getUser().getCreatedAt()).append("\n");
            }
            if (response.getErrorMessage() != null) {
                result.append("Error Message: ").append(response.getErrorMessage()).append("\n");
            }

            return ResponseEntity.ok(result.toString());

        } catch (java.util.concurrent.TimeoutException e) {
            LOGGER.error("User lookup request timed out for email '{}'", email);
            return ResponseEntity.status(408).body("User lookup request timed out after 30 seconds");
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

            // Wait for the response synchronously with timeout
            PlayerCharacterLookupResponse response = future.get(30, java.util.concurrent.TimeUnit.SECONDS);

            LOGGER.info("Character lookup response received for ID {}: status={}", characterId, response.getStatus());

            // Return the response as a formatted string
            StringBuilder result = new StringBuilder();
            result.append("Character Lookup Response:\n");
            result.append("Status: ").append(response.getStatus()).append("\n");
            result.append("Request ID: ").append(response.getRequestId()).append("\n");
            result.append("Characters found: ").append(response.getCharacters().size()).append("\n");

            response.getCharacters().forEach(character -> {
                result.append("  - Character ID: ").append(character.getId()).append("\n");
                result.append("    Name: ").append(character.getName()).append("\n");
                result.append("    Level: ").append(character.getLevel()).append("\n");
                result.append("    User ID: ").append(character.getUserId()).append("\n");
                if (character.getCreatedAt() != null) {
                    result.append("    Created: ").append(character.getCreatedAt()).append("\n");
                }
                result.append("\n");
            });

            if (response.getErrorMessage() != null) {
                result.append("Error Message: ").append(response.getErrorMessage()).append("\n");
            }

            return ResponseEntity.ok(result.toString());

        } catch (java.util.concurrent.TimeoutException e) {
            LOGGER.error("Character lookup request timed out for ID {}", characterId);
            return ResponseEntity.status(408).body("Character lookup request timed out after 30 seconds");
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

            // Wait for the response synchronously with timeout
            PlayerCharacterLookupResponse response = future.get(30, java.util.concurrent.TimeUnit.SECONDS);

            LOGGER.info("Character lookup response received for name '{}': status={}", characterName, response.getStatus());

            // Return the response as a formatted string
            StringBuilder result = new StringBuilder();
            result.append("Character Lookup Response:\n");
            result.append("Status: ").append(response.getStatus()).append("\n");
            result.append("Request ID: ").append(response.getRequestId()).append("\n");
            result.append("Characters found: ").append(response.getCharacters().size()).append("\n");

            response.getCharacters().forEach(character -> {
                result.append("  - Character ID: ").append(character.getId()).append("\n");
                result.append("    Name: ").append(character.getName()).append("\n");
                result.append("    Level: ").append(character.getLevel()).append("\n");
                result.append("    User ID: ").append(character.getUserId()).append("\n");
                if (character.getCreatedAt() != null) {
                    result.append("    Created: ").append(character.getCreatedAt()).append("\n");
                }
                result.append("\n");
            });

            if (response.getErrorMessage() != null) {
                result.append("Error Message: ").append(response.getErrorMessage()).append("\n");
            }

            return ResponseEntity.ok(result.toString());

        } catch (java.util.concurrent.TimeoutException e) {
            LOGGER.error("Character lookup request timed out for name '{}'", characterName);
            return ResponseEntity.status(408).body("Character lookup request timed out after 30 seconds");
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

            // Wait for the response synchronously with timeout
            PlayerCharacterLookupResponse response = future.get(30, java.util.concurrent.TimeUnit.SECONDS);

            LOGGER.info("Characters lookup response received for user ID {}: status={}", userId, response.getStatus());

            // Return the response as a formatted string
            StringBuilder result = new StringBuilder();
            result.append("Characters Lookup Response:\n");
            result.append("Status: ").append(response.getStatus()).append("\n");
            result.append("Request ID: ").append(response.getRequestId()).append("\n");
            result.append("Characters found: ").append(response.getCharacters().size()).append("\n");

            response.getCharacters().forEach(character -> {
                result.append("  - Character ID: ").append(character.getId()).append("\n");
                result.append("    Name: ").append(character.getName()).append("\n");
                result.append("    Level: ").append(character.getLevel()).append("\n");
                result.append("    User ID: ").append(character.getUserId()).append("\n");
                if (character.getCreatedAt() != null) {
                    result.append("    Created: ").append(character.getCreatedAt()).append("\n");
                }
                result.append("\n");
            });

            if (response.getErrorMessage() != null) {
                result.append("Error Message: ").append(response.getErrorMessage()).append("\n");
            }

            return ResponseEntity.ok(result.toString());

        } catch (java.util.concurrent.TimeoutException e) {
            LOGGER.error("Characters lookup request timed out for user ID {}", userId);
            return ResponseEntity.status(408).body("Characters lookup request timed out after 30 seconds");
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

            // Wait for the response synchronously with timeout
            PublicKeyResponse response = future.get(30, java.util.concurrent.TimeUnit.SECONDS);

            LOGGER.info("Public key response received: status={}", response.getStatus());

            // Return the response as a formatted string
            StringBuilder result = new StringBuilder();
            result.append("Public Key Response:\n");
            result.append("Status: ").append(response.getStatus()).append("\n");
            result.append("Request ID: ").append(response.getRequestId()).append("\n");
            if (response.getPublicKey() != null) {
                result.append("Key Type: ").append(response.getKeyType()).append("\n");
                result.append("Algorithm: ").append(response.getAlgorithm()).append("\n");
                result.append("Public Key: ").append(response.getPublicKey()).append("\n");
            }
            if (response.getErrorMessage() != null) {
                result.append("Error Message: ").append(response.getErrorMessage()).append("\n");
            }

            return ResponseEntity.ok(result.toString());

        } catch (java.util.concurrent.TimeoutException e) {
            LOGGER.error("Public key request timed out");
            return ResponseEntity.status(408).body("Public key request timed out after 30 seconds");
        } catch (Exception e) {
            LOGGER.error("Failed to request public key", e);
            return ResponseEntity.internalServerError().body("Failed to request public key: " + e.getMessage());
        }
    }
}
