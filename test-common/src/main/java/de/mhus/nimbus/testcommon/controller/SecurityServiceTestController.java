package de.mhus.nimbus.testcommon.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.mhus.nimbus.common.exception.NimbusException;
import de.mhus.nimbus.common.service.SecurityService;
import io.jsonwebtoken.Claims;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Base64;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * REST Controller for testing SecurityService functionality
 */
@RestController
@RequestMapping("/api/test/security")
public class SecurityServiceTestController {

    private static final Logger LOGGER = LoggerFactory.getLogger(SecurityServiceTestController.class);

    // Standard JWT Claims according to RFC 7519
    private static final Set<String> STANDARD_JWT_CLAIMS = Set.of(
        "iss", "sub", "aud", "exp", "nbf", "iat", "jti"
    );

    private final SecurityService securityService;

    @Autowired
    public SecurityServiceTestController(SecurityService securityService) {
        this.securityService = securityService;
    }

    /**
     * Test login request with SecurityService
     */
    @PostMapping("/login")
    public ResponseEntity<String> login(
            @RequestParam String username,
            @RequestParam String password,
            @RequestParam(required = false) String clientInfo) {
        try {
            LOGGER.info("REST: Requesting login via SecurityService for username: {}", username);

            SecurityService.LoginResult result;
            if (clientInfo != null) {
                result = securityService.login(username, password, clientInfo);
            } else {
                result = securityService.login(username, password);
            }

            LOGGER.info("Login response received for user '{}': success={}", username, result.isSuccess());

            // Return the response as a formatted string
            StringBuilder response = new StringBuilder();
            response.append("SecurityService Login Response:\n");
            response.append("Success: ").append(result.isSuccess()).append("\n");
            response.append("Message: ").append(result.getMessage()).append("\n");

            if (result.isSuccess() && result.getToken() != null) {
                response.append("Token: ").append(result.getToken()).append("\n");
                response.append("Expires At: ").append(result.getExpiresAt()).append("\n");

                // Extrahiere und zeige Character-Namen aus dem Token
                List<String> characterNames = extractCharacterNamesFromToken(result.getToken());
                if (!characterNames.isEmpty()) {
                    response.append("Identity Characters: ").append(String.join(", ", characterNames)).append("\n");
                } else {
                    response.append("Identity Characters: None\n");
                }

                if (result.getUser() != null) {
                    response.append("User ID: ").append(result.getUser().getId()).append("\n");
                    response.append("Username: ").append(result.getUser().getUsername()).append("\n");
                    response.append("Email: ").append(result.getUser().getEmail()).append("\n");
                    if (result.getUser().getFirstName() != null) {
                        response.append("First Name: ").append(result.getUser().getFirstName()).append("\n");
                    }
                    if (result.getUser().getLastName() != null) {
                        response.append("Last Name: ").append(result.getUser().getLastName()).append("\n");
                    }
                }
            }

            if (result.getErrorCode() != null) {
                response.append("Error Code: ").append(result.getErrorCode()).append("\n");
            }

            return ResponseEntity.ok(response.toString());

        } catch (java.util.concurrent.TimeoutException e) {
            LOGGER.error("Login request timed out for user '{}'", username);
            return ResponseEntity.status(408).body("Login request timed out after 30 seconds");
        } catch (NimbusException e) {
            LOGGER.error("Login failed with NimbusException for user '{}': {}", username, e.getMessage());
            return ResponseEntity.badRequest().body("Login failed: " + e.getMessage() + " (Code: " + e.getErrorCode() + ")");
        } catch (Exception e) {
            LOGGER.error("Failed to login via SecurityService", e);
            return ResponseEntity.internalServerError().body("Failed to login: " + e.getMessage());
        }
    }

    /**
     * Test async login request with SecurityService
     */
    @PostMapping("/login-async")
    public ResponseEntity<String> loginAsync(
            @RequestParam String username,
            @RequestParam String password,
            @RequestParam(required = false) String clientInfo) {
        try {
            LOGGER.info("REST: Requesting async login via SecurityService for username: {}", username);

            CompletableFuture<SecurityService.LoginResult> future =
                securityService.loginAsync(username, password, clientInfo);

            // Wait for the response synchronously with timeout
            SecurityService.LoginResult result = future.get(30, java.util.concurrent.TimeUnit.SECONDS);

            LOGGER.info("Async login response received for user '{}': success={}", username, result.isSuccess());

            // Return the response as a formatted string
            StringBuilder response = new StringBuilder();
            response.append("SecurityService Async Login Response:\n");
            response.append("Success: ").append(result.isSuccess()).append("\n");
            response.append("Message: ").append(result.getMessage()).append("\n");

            if (result.isSuccess() && result.getToken() != null) {
                response.append("Token: ").append(result.getToken()).append("\n");
                response.append("Expires At: ").append(result.getExpiresAt()).append("\n");

                // Extrahiere und zeige Character-Namen aus dem Token
                List<String> characterNames = extractCharacterNamesFromToken(result.getToken());
                if (!characterNames.isEmpty()) {
                    response.append("Identity Characters: ").append(String.join(", ", characterNames)).append("\n");
                } else {
                    response.append("Identity Characters: None\n");
                }

                if (result.getUser() != null) {
                    response.append("User ID: ").append(result.getUser().getId()).append("\n");
                    response.append("Username: ").append(result.getUser().getUsername()).append("\n");
                    response.append("Email: ").append(result.getUser().getEmail()).append("\n");
                    if (result.getUser().getFirstName() != null) {
                        response.append("First Name: ").append(result.getUser().getFirstName()).append("\n");
                    }
                    if (result.getUser().getLastName() != null) {
                        response.append("Last Name: ").append(result.getUser().getLastName()).append("\n");
                    }
                }
            }

            if (result.getErrorCode() != null) {
                response.append("Error Code: ").append(result.getErrorCode()).append("\n");
            }

            return ResponseEntity.ok(response.toString());

        } catch (java.util.concurrent.TimeoutException e) {
            LOGGER.error("Async login request timed out for user '{}'", username);
            return ResponseEntity.status(408).body("Async login request timed out after 30 seconds");
        } catch (Exception e) {
            LOGGER.error("Failed to login async via SecurityService", e);
            return ResponseEntity.internalServerError().body("Failed to login async: " + e.getMessage());
        }
    }

    /**
     * Test public key request with SecurityService
     */
    @GetMapping("/public-key")
    public ResponseEntity<String> getPublicKey(@RequestParam(required = false, defaultValue = "false") boolean forceRefresh) {
        try {
            LOGGER.info("REST: Requesting public key via SecurityService, forceRefresh={}", forceRefresh);

            SecurityService.PublicKeyInfo keyInfo = securityService.getPublicKey(forceRefresh);

            LOGGER.info("Public key response received successfully");

            // Return the response as a formatted string
            StringBuilder response = new StringBuilder();
            response.append("SecurityService Public Key Response:\n");
            response.append("Key Type: ").append(keyInfo.getKeyType()).append("\n");
            response.append("Algorithm: ").append(keyInfo.getAlgorithm()).append("\n");
            response.append("Issuer: ").append(keyInfo.getIssuer()).append("\n");
            response.append("Fetched At: ").append(keyInfo.getFetchedAt()).append("\n");
            response.append("Public Key: ").append(keyInfo.getPublicKey()).append("\n");

            return ResponseEntity.ok(response.toString());

        } catch (java.util.concurrent.TimeoutException e) {
            LOGGER.error("Public key request timed out");
            return ResponseEntity.status(408).body("Public key request timed out after 30 seconds");
        } catch (NimbusException e) {
            LOGGER.error("Public key request failed with NimbusException: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Public key request failed: " + e.getMessage() + " (Code: " + e.getErrorCode() + ")");
        } catch (Exception e) {
            LOGGER.error("Failed to get public key via SecurityService", e);
            return ResponseEntity.internalServerError().body("Failed to get public key: " + e.getMessage());
        }
    }

    /**
     * Test async public key request with SecurityService
     */
    @GetMapping("/public-key-async")
    public ResponseEntity<String> getPublicKeyAsync(@RequestParam(required = false, defaultValue = "false") boolean forceRefresh) {
        try {
            LOGGER.info("REST: Requesting async public key via SecurityService, forceRefresh={}", forceRefresh);

            CompletableFuture<SecurityService.PublicKeyInfo> future =
                securityService.getPublicKeyAsync(forceRefresh);

            // Wait for the response synchronously with timeout
            SecurityService.PublicKeyInfo keyInfo = future.get(30, java.util.concurrent.TimeUnit.SECONDS);

            LOGGER.info("Async public key response received successfully");

            // Return the response as a formatted string
            StringBuilder response = new StringBuilder();
            response.append("SecurityService Async Public Key Response:\n");
            response.append("Key Type: ").append(keyInfo.getKeyType()).append("\n");
            response.append("Algorithm: ").append(keyInfo.getAlgorithm()).append("\n");
            response.append("Issuer: ").append(keyInfo.getIssuer()).append("\n");
            response.append("Fetched At: ").append(keyInfo.getFetchedAt()).append("\n");
            response.append("Public Key: ").append(keyInfo.getPublicKey()).append("\n");

            return ResponseEntity.ok(response.toString());

        } catch (java.util.concurrent.TimeoutException e) {
            LOGGER.error("Async public key request timed out");
            return ResponseEntity.status(408).body("Async public key request timed out after 30 seconds");
        } catch (Exception e) {
            LOGGER.error("Failed to get async public key via SecurityService", e);
            return ResponseEntity.internalServerError().body("Failed to get async public key: " + e.getMessage());
        }
    }

    /**
     * Test token validation with SecurityService
     */
    @PostMapping("/validate-token")
    public ResponseEntity<String> validateToken(@RequestParam String token) {
        try {
            LOGGER.info("REST: Validating token via SecurityService");

            Claims claims = securityService.validateToken(token);

            LOGGER.info("Token validation successful for subject: {}", claims.getSubject());

            // Return the response as a formatted string
            StringBuilder response = new StringBuilder();
            response.append("SecurityService Token Validation Response:\n");
            response.append("Valid: true\n");
            response.append("Subject: ").append(claims.getSubject()).append("\n");
            response.append("Issuer: ").append(claims.getIssuer()).append("\n");
            response.append("Audience: ").append(claims.getAudience()).append("\n");
            response.append("Issued At: ").append(claims.getIssuedAt()).append("\n");
            response.append("Expiration: ").append(claims.getExpiration()).append("\n");
            response.append("Not Before: ").append(claims.getNotBefore()).append("\n");
            response.append("JWT ID: ").append(claims.getId()).append("\n");

            // Extrahiere und zeige Character-Namen aus dem Token
            List<String> characterNames = extractCharacterNamesFromToken(token);
            if (!characterNames.isEmpty()) {
                response.append("Identity Characters: ").append(String.join(", ", characterNames)).append("\n");
            } else {
                response.append("Identity Characters: None\n");
            }

            // Extrahiere und zeige ACE-Regeln aus dem Token
            List<String> aceRules = extractAceRulesFromToken(token);
            if (!aceRules.isEmpty()) {
                response.append("ACE Rules: ").append(String.join(", ", aceRules)).append("\n");
            } else {
                response.append("ACE Rules: None\n");
            }

            // Add custom claims
            response.append("Custom Claims:\n");
            claims.entrySet().stream()
                .filter(entry -> !STANDARD_JWT_CLAIMS.contains(entry.getKey()))
                .forEach(entry -> response.append("  ").append(entry.getKey())
                    .append(": ").append(entry.getValue()).append("\n"));

            return ResponseEntity.ok(response.toString());

        } catch (NimbusException e) {
            LOGGER.error("Token validation failed with NimbusException: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Token validation failed: " + e.getMessage() + " (Code: " + e.getErrorCode() + ")");
        } catch (Exception e) {
            LOGGER.error("Failed to validate token via SecurityService", e);
            return ResponseEntity.badRequest().body("Token validation failed: " + e.getMessage());
        }
    }

    /**
     * Get cached public key information
     */
    @GetMapping("/cached-public-key")
    public ResponseEntity<String> getCachedPublicKey() {
        try {
            LOGGER.info("REST: Getting cached public key via SecurityService");

            SecurityService.PublicKeyInfo keyInfo = securityService.getCachedPublicKey();

            if (keyInfo == null) {
                return ResponseEntity.ok("No cached public key available");
            }

            // Return the response as a formatted string
            StringBuilder response = new StringBuilder();
            response.append("SecurityService Cached Public Key:\n");
            response.append("Key Type: ").append(keyInfo.getKeyType()).append("\n");
            response.append("Algorithm: ").append(keyInfo.getAlgorithm()).append("\n");
            response.append("Issuer: ").append(keyInfo.getIssuer()).append("\n");
            response.append("Fetched At: ").append(keyInfo.getFetchedAt()).append("\n");
            response.append("Public Key: ").append(keyInfo.getPublicKey()).append("\n");

            return ResponseEntity.ok(response.toString());

        } catch (Exception e) {
            LOGGER.error("Failed to get cached public key", e);
            return ResponseEntity.internalServerError().body("Failed to get cached public key: " + e.getMessage());
        }
    }

    /**
     * Check if a valid public key is cached
     */
    @GetMapping("/has-valid-public-key")
    public ResponseEntity<String> hasValidPublicKey() {
        try {
            LOGGER.info("REST: Checking if valid public key is cached");

            boolean hasValidKey = securityService.hasValidPublicKey();

            StringBuilder response = new StringBuilder();
            response.append("SecurityService Valid Public Key Check:\n");
            response.append("Has Valid Key: ").append(hasValidKey).append("\n");

            return ResponseEntity.ok(response.toString());

        } catch (Exception e) {
            LOGGER.error("Failed to check valid public key", e);
            return ResponseEntity.internalServerError().body("Failed to check valid public key: " + e.getMessage());
        }
    }

    /**
     * Clear the public key cache
     */
    @DeleteMapping("/public-key-cache")
    public ResponseEntity<String> clearPublicKeyCache() {
        try {
            LOGGER.info("REST: Clearing public key cache via SecurityService");

            securityService.clearPublicKeyCache();

            return ResponseEntity.ok("Public key cache cleared successfully");

        } catch (Exception e) {
            LOGGER.error("Failed to clear public key cache", e);
            return ResponseEntity.internalServerError().body("Failed to clear public key cache: " + e.getMessage());
        }
    }

    private List<String> extractCharacterNamesFromToken(String token) {
        // Implement the logic to extract character names from the token
        // This is a placeholder implementation
        try {
            String[] parts = token.split("\\.");
            if (parts.length == 3) {
                String payload = new String(Base64.getUrlDecoder().decode(parts[1]));
                ObjectMapper mapper = new ObjectMapper();
                JsonNode jsonNode = mapper.readTree(payload);

                // Assuming character names are stored under a claim named "character_names"
                JsonNode characterNamesNode = jsonNode.get("character_names");
                if (characterNamesNode != null && characterNamesNode.isArray()) {
                    return mapper.readValue(characterNamesNode.traverse(), mapper.getTypeFactory().constructCollectionType(List.class, String.class));
                }
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to extract character names from token: {}", e.getMessage());
        }
        return List.of();
    }

    private List<String> extractAceRulesFromToken(String token) {
        // Implement the logic to extract ACE rules from the token
        try {
            String[] parts = token.split("\\.");
            if (parts.length == 3) {
                String payload = new String(Base64.getUrlDecoder().decode(parts[1]));
                ObjectMapper mapper = new ObjectMapper();
                JsonNode jsonNode = mapper.readTree(payload);

                // ACE rules are stored under the claim named "aceRules"
                JsonNode aceRulesNode = jsonNode.get("aceRules");
                if (aceRulesNode != null && aceRulesNode.isArray()) {
                    return mapper.readValue(aceRulesNode.traverse(), mapper.getTypeFactory().constructCollectionType(List.class, String.class));
                }
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to extract ACE rules from token: {}", e.getMessage());
        }
        return List.of();
    }
}
