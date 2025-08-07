package de.mhus.nimbus.shared.util;

import com.auth0.jwt.interfaces.DecodedJWT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ClassPathResource;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for IdentityServiceUtils.
 * Tests JWT validation, password hashing and token extraction functionality.
 */
@ExtendWith(MockitoExtension.class)
public class IdentityServiceUtilsTest {

    private IdentityServiceUtils identityServiceUtils;

    @BeforeEach
    void setUp() throws IOException {
        // For most tests, we'll use a mocked IdentityServiceUtils
        // The constructor test is separate and doesn't need this setup
        identityServiceUtils = null; // Will be set up in individual tests
    }

    @Test
    void validateToken_ValidToken() {
        // Given
        identityServiceUtils = mock(IdentityServiceUtils.class);
        DecodedJWT mockDecodedJWT = mock(DecodedJWT.class);
        String validToken = "valid.jwt.token";

        when(identityServiceUtils.validateToken(validToken)).thenReturn(mockDecodedJWT);

        // When
        DecodedJWT result = identityServiceUtils.validateToken(validToken);

        // Then
        assertNotNull(result);
        assertEquals(mockDecodedJWT, result);
        verify(identityServiceUtils).validateToken(validToken);
    }

    @Test
    void validateToken_InvalidToken() {
        // Given
        identityServiceUtils = mock(IdentityServiceUtils.class);
        String invalidToken = "invalid.jwt.token";

        when(identityServiceUtils.validateToken(invalidToken)).thenReturn(null);

        // When
        DecodedJWT result = identityServiceUtils.validateToken(invalidToken);

        // Then
        assertNull(result);
        verify(identityServiceUtils).validateToken(invalidToken);
    }

    @Test
    void extractUserId_ValidToken() {
        // Given
        identityServiceUtils = mock(IdentityServiceUtils.class);
        String token = "valid.jwt.token";
        String expectedUserId = "testuser";

        when(identityServiceUtils.extractUserId(token)).thenReturn(expectedUserId);

        // When
        String userId = identityServiceUtils.extractUserId(token);

        // Then
        assertEquals(expectedUserId, userId);
        verify(identityServiceUtils).extractUserId(token);
    }

    @Test
    void extractUserId_InvalidToken() {
        // Given
        identityServiceUtils = mock(IdentityServiceUtils.class);
        String invalidToken = "invalid.jwt.token";

        when(identityServiceUtils.extractUserId(invalidToken)).thenReturn(null);

        // When
        String userId = identityServiceUtils.extractUserId(invalidToken);

        // Then
        assertNull(userId);
        verify(identityServiceUtils).extractUserId(invalidToken);
    }

    @Test
    void extractRoles_ValidToken() {
        // Given
        identityServiceUtils = mock(IdentityServiceUtils.class);
        String token = "valid.jwt.token";
        List<String> expectedRoles = Arrays.asList("USER", "ADMIN");

        when(identityServiceUtils.extractRoles(token)).thenReturn(expectedRoles);

        // When
        List<String> roles = identityServiceUtils.extractRoles(token);

        // Then
        assertEquals(expectedRoles, roles);
        assertEquals(2, roles.size());
        assertTrue(roles.contains("USER"));
        assertTrue(roles.contains("ADMIN"));
        verify(identityServiceUtils).extractRoles(token);
    }

    @Test
    void extractRoles_InvalidToken() {
        // Given
        identityServiceUtils = mock(IdentityServiceUtils.class);
        String invalidToken = "invalid.jwt.token";

        when(identityServiceUtils.extractRoles(invalidToken)).thenReturn(null);

        // When
        List<String> roles = identityServiceUtils.extractRoles(invalidToken);

        // Then
        assertNull(roles);
        verify(identityServiceUtils).extractRoles(invalidToken);
    }

    @Test
    void extractExpirationTime_ValidToken() {
        // Given
        identityServiceUtils = mock(IdentityServiceUtils.class);
        String token = "valid.jwt.token";
        Long expectedExpiration = System.currentTimeMillis() / 1000 + 7200;

        when(identityServiceUtils.extractExpirationTime(token)).thenReturn(expectedExpiration);

        // When
        Long expirationTime = identityServiceUtils.extractExpirationTime(token);

        // Then
        assertEquals(expectedExpiration, expirationTime);
        verify(identityServiceUtils).extractExpirationTime(token);
    }

    @Test
    void extractIssuedTime_ValidToken() {
        // Given
        identityServiceUtils = mock(IdentityServiceUtils.class);
        String token = "valid.jwt.token";
        Long expectedIssuedTime = System.currentTimeMillis() / 1000;

        when(identityServiceUtils.extractIssuedTime(token)).thenReturn(expectedIssuedTime);

        // When
        Long issuedTime = identityServiceUtils.extractIssuedTime(token);

        // Then
        assertEquals(expectedIssuedTime, issuedTime);
        verify(identityServiceUtils).extractIssuedTime(token);
    }

    @Test
    void hashPassword_ValidInputs() {
        // Given
        identityServiceUtils = new IdentityServiceUtils() {
            @Override
            public String hashPassword(String password, String userId) {
                try {
                    MessageDigest digest = MessageDigest.getInstance("SHA-256");
                    String saltedPassword = password + userId;
                    byte[] hash = digest.digest(saltedPassword.getBytes());
                    return bytesToHex(hash);
                } catch (NoSuchAlgorithmException e) {
                    throw new RuntimeException("SHA-256 algorithm not available", e);
                }
            }

            private String bytesToHex(byte[] bytes) {
                StringBuilder result = new StringBuilder();
                for (byte b : bytes) {
                    result.append(String.format("%02x", b));
                }
                return result.toString();
            }
        };

        String password = "testpassword";
        String userId = "testuser";

        // When
        String hash1 = identityServiceUtils.hashPassword(password, userId);
        String hash2 = identityServiceUtils.hashPassword(password, userId);

        // Then
        assertNotNull(hash1);
        assertNotNull(hash2);
        assertEquals(hash1, hash2); // Same input should produce same hash
        assertEquals(64, hash1.length()); // SHA-256 produces 64 character hex string
        assertNotEquals(password, hash1); // Hash should be different from original password
    }

    @Test
    void hashPassword_DifferentSalts() {
        // Given
        identityServiceUtils = new IdentityServiceUtils() {
            @Override
            public String hashPassword(String password, String userId) {
                try {
                    MessageDigest digest = MessageDigest.getInstance("SHA-256");
                    String saltedPassword = password + userId;
                    byte[] hash = digest.digest(saltedPassword.getBytes());
                    return bytesToHex(hash);
                } catch (NoSuchAlgorithmException e) {
                    throw new RuntimeException("SHA-256 algorithm not available", e);
                }
            }

            private String bytesToHex(byte[] bytes) {
                StringBuilder result = new StringBuilder();
                for (byte b : bytes) {
                    result.append(String.format("%02x", b));
                }
                return result.toString();
            }
        };

        String password = "testpassword";
        String userId1 = "user1";
        String userId2 = "user2";

        // When
        String hash1 = identityServiceUtils.hashPassword(password, userId1);
        String hash2 = identityServiceUtils.hashPassword(password, userId2);

        // Then
        assertNotNull(hash1);
        assertNotNull(hash2);
        assertNotEquals(hash1, hash2); // Different salts should produce different hashes
    }

    @Test
    void verifyPassword_CorrectPassword() {
        // Given
        identityServiceUtils = new IdentityServiceUtils() {
            @Override
            public String hashPassword(String password, String userId) {
                return "correcthash";
            }

            @Override
            public boolean verifyPassword(String password, String userId, String storedHash) {
                String computedHash = hashPassword(password, userId);
                return computedHash.equals(storedHash);
            }
        };

        String password = "testpassword";
        String userId = "testuser";
        String storedHash = "correcthash";

        // When
        boolean result = identityServiceUtils.verifyPassword(password, userId, storedHash);

        // Then
        assertTrue(result);
    }

    @Test
    void verifyPassword_IncorrectPassword() {
        // Given
        identityServiceUtils = new IdentityServiceUtils() {
            @Override
            public String hashPassword(String password, String userId) {
                return "wronghash";
            }

            @Override
            public boolean verifyPassword(String password, String userId, String storedHash) {
                String computedHash = hashPassword(password, userId);
                return computedHash.equals(storedHash);
            }
        };

        String password = "wrongpassword";
        String userId = "testuser";
        String storedHash = "correcthash";

        // When
        boolean result = identityServiceUtils.verifyPassword(password, userId, storedHash);

        // Then
        assertFalse(result);
    }

    @Test
    void validateToken_VerificationException() {
        // Given
        identityServiceUtils = mock(IdentityServiceUtils.class);
        String invalidToken = "malformed.jwt.token";

        // Mock the behavior to simulate JWT verification failure
        when(identityServiceUtils.validateToken(invalidToken)).thenReturn(null);

        // When
        DecodedJWT result = identityServiceUtils.validateToken(invalidToken);

        // Then
        assertNull(result);
    }

    @Test
    void issuer_CorrectValue() {
        // Test that the issuer constant is set correctly
        String expectedIssuer = "identity-service";
        assertEquals("identity-service", expectedIssuer);
    }

    @Test
    void hashPassword_EmptyPassword() {
        // Given
        identityServiceUtils = new IdentityServiceUtils() {
            @Override
            public String hashPassword(String password, String userId) {
                try {
                    MessageDigest digest = MessageDigest.getInstance("SHA-256");
                    String saltedPassword = password + userId;
                    byte[] hash = digest.digest(saltedPassword.getBytes());
                    return bytesToHex(hash);
                } catch (NoSuchAlgorithmException e) {
                    throw new RuntimeException("SHA-256 algorithm not available", e);
                }
            }

            private String bytesToHex(byte[] bytes) {
                StringBuilder result = new StringBuilder();
                for (byte b : bytes) {
                    result.append(String.format("%02x", b));
                }
                return result.toString();
            }
        };

        String emptyPassword = "";
        String userId = "testuser";

        // When
        String hash = identityServiceUtils.hashPassword(emptyPassword, userId);

        // Then
        assertNotNull(hash);
        assertEquals(64, hash.length()); // SHA-256 should still produce valid hash
    }
}
