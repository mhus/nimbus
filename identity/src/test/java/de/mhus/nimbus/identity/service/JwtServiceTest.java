package de.mhus.nimbus.identity.service;

import de.mhus.nimbus.identity.config.JwtProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.security.PublicKey;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test für JwtService mit RSA-Signierung
 */
@SpringBootTest
@ActiveProfiles("test")
class JwtServiceTest {

    @Autowired
    private JwtService jwtService;

    @Autowired
    private JwtProperties jwtProperties;

    @Test
    void testGenerateAndValidateToken() {
        // Given
        Long userId = 123L;
        String username = "testuser";
        String email = "test@example.com";

        // When
        String token = jwtService.generateToken(userId, username, email);

        // Then
        assertNotNull(token);
        assertFalse(token.isEmpty());

        // Validiere Token-Inhalte
        assertEquals(username, jwtService.extractUsername(token));
        assertEquals(userId, jwtService.extractUserId(token));
        assertEquals(jwtProperties.getIssuer(), jwtService.extractIssuer(token));
        assertFalse(jwtService.isTokenExpired(token));
        assertTrue(jwtService.validateToken(token, username));
    }

    @Test
    void testPublicKeyRetrieval() {
        // When
        PublicKey publicKey = jwtService.getPublicKey();
        String publicKeyString = jwtService.getPublicKeyAsString();

        // Then
        assertNotNull(publicKey);
        assertEquals("RSA", publicKey.getAlgorithm());
        assertNotNull(publicKeyString);
        assertFalse(publicKeyString.isEmpty());
    }

    @Test
    void testTokenValidationWithWrongUsername() {
        // Given
        String token = jwtService.generateToken(1L, "user1", "user1@example.com");

        // When/Then
        assertFalse(jwtService.validateToken(token, "user2"));
    }

    @Test
    void testTokenSignatureVerification() {
        // Given
        String token = jwtService.generateToken(1L, "testuser", "test@example.com");

        // When - Token sollte mit öffentlichem Schlüssel validierbar sein
        assertDoesNotThrow(() -> {
            String extractedUsername = jwtService.extractUsername(token);
            Long extractedUserId = jwtService.extractUserId(token);
            String extractedIssuer = jwtService.extractIssuer(token);

            assertEquals("testuser", extractedUsername);
            assertEquals(1L, extractedUserId);
            assertEquals(jwtProperties.getIssuer(), extractedIssuer);
        });
    }

    @Test
    void testInvalidTokenHandling() {
        // Given
        String invalidToken = "invalid.token.here";

        // When/Then - Sollte Exception werfen oder false zurückgeben
        assertFalse(jwtService.validateToken(invalidToken, "anyuser"));
    }
}
