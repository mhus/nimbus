package de.mhus.nimbus.shared.jwt;

import de.mhus.nimbus.shared.keys.IKeyService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtServiceTest {

    @Mock
    private IKeyService keyService;

    private JwtService jwtService;

    private SecretKey testSecretKey;
    private SecretKey testSyncKey;
    private PublicKey testPublicKey;
    private PrivateKey testPrivateKey;

    private static final String TEST_KEY_ID = "owner:uuid-123";
    private static final String TEST_SUBJECT = "user123";
    private static final String TEST_ROLE = "admin";
    private static final String TEST_EMAIL = "user@example.com";

    @BeforeEach
    void setUp() throws Exception {
        jwtService = new JwtService(keyService);

        // Generate test secret key for HMAC
        KeyGenerator keyGen = KeyGenerator.getInstance("HmacSHA256");
        keyGen.init(256);
        testSecretKey = keyGen.generateKey();
        testSyncKey = keyGen.generateKey();

        // Generate test RSA key pair
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        KeyPair keyPair = keyPairGenerator.generateKeyPair();
        testPublicKey = keyPair.getPublic();
        testPrivateKey = keyPair.getPrivate();
    }

    // ================================================================
    // createTokenWithSecretKey tests
    // ================================================================

    @Test
    void createTokenWithSecretKey_validInput_shouldCreateToken() {
        when(keyService.findSecretKey(TEST_KEY_ID)).thenReturn(Optional.of(testSecretKey));

        Map<String, Object> claims = Map.of("role", TEST_ROLE, "email", TEST_EMAIL);
        Instant expiresAt = Instant.now().plusSeconds(3600);

        String token = jwtService.createTokenWithSecretKey(TEST_KEY_ID, TEST_SUBJECT, claims, expiresAt);

        assertThat(token).isNotNull();
        assertThat(token).isNotEmpty();
        assertThat(token.split("\\.")).hasSize(3); // JWT has 3 parts
    }

    @Test
    void createTokenWithSecretKey_noClaims_shouldCreateToken() {
        when(keyService.findSecretKey(TEST_KEY_ID)).thenReturn(Optional.of(testSecretKey));

        String token = jwtService.createTokenWithSecretKey(TEST_KEY_ID, TEST_SUBJECT, null, null);

        assertThat(token).isNotNull();
        assertThat(token).isNotEmpty();
    }

    @Test
    void createTokenWithSecretKey_emptyClaims_shouldCreateToken() {
        when(keyService.findSecretKey(TEST_KEY_ID)).thenReturn(Optional.of(testSecretKey));

        String token = jwtService.createTokenWithSecretKey(TEST_KEY_ID, TEST_SUBJECT, Map.of(), null);

        assertThat(token).isNotNull();
        assertThat(token).isNotEmpty();
    }

    @Test
    void createTokenWithSecretKey_noExpiration_shouldCreateToken() {
        when(keyService.findSecretKey(TEST_KEY_ID)).thenReturn(Optional.of(testSecretKey));

        String token = jwtService.createTokenWithSecretKey(TEST_KEY_ID, TEST_SUBJECT, null, null);

        assertThat(token).isNotNull();
        assertThat(token).isNotEmpty();
    }

    @Test
    void createTokenWithSecretKey_keyNotFound_shouldThrowException() {
        when(keyService.findSecretKey(TEST_KEY_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> 
            jwtService.createTokenWithSecretKey(TEST_KEY_ID, TEST_SUBJECT, null, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Secret key not found");
    }

    // ================================================================
    // createTokenWithSyncKey tests
    // ================================================================

    @Test
    void createTokenWithSyncKey_validInput_shouldCreateToken() {
        when(keyService.findSyncKey(TEST_KEY_ID)).thenReturn(Optional.of(testSyncKey));

        Map<String, Object> claims = Map.of("role", TEST_ROLE, "email", TEST_EMAIL);
        Instant expiresAt = Instant.now().plusSeconds(3600);

        String token = jwtService.createTokenWithSyncKey(TEST_KEY_ID, TEST_SUBJECT, claims, expiresAt);

        assertThat(token).isNotNull();
        assertThat(token).isNotEmpty();
        assertThat(token.split("\\.")).hasSize(3);
    }

    @Test
    void createTokenWithSyncKey_noClaims_shouldCreateToken() {
        when(keyService.findSyncKey(TEST_KEY_ID)).thenReturn(Optional.of(testSyncKey));

        String token = jwtService.createTokenWithSyncKey(TEST_KEY_ID, TEST_SUBJECT, null, null);

        assertThat(token).isNotNull();
        assertThat(token).isNotEmpty();
    }

    @Test
    void createTokenWithSyncKey_keyNotFound_shouldThrowException() {
        when(keyService.findSyncKey(TEST_KEY_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> 
            jwtService.createTokenWithSyncKey(TEST_KEY_ID, TEST_SUBJECT, null, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Sync key not found");
    }

    // ================================================================
    // validateTokenWithSecretKey tests
    // ================================================================

    @Test
    void validateTokenWithSecretKey_validToken_shouldReturnClaims() {
        when(keyService.findSecretKey(TEST_KEY_ID)).thenReturn(Optional.of(testSecretKey));

        Map<String, Object> claims = Map.of("role", TEST_ROLE, "email", TEST_EMAIL);
        Instant expiresAt = Instant.now().plusSeconds(3600);
        String token = jwtService.createTokenWithSecretKey(TEST_KEY_ID, TEST_SUBJECT, claims, expiresAt);

        Optional<Jws<Claims>> result = jwtService.validateTokenWithSecretKey(token, TEST_KEY_ID);

        assertThat(result).isPresent();
        Claims parsedClaims = result.get().getPayload();
        assertThat(parsedClaims.getSubject()).isEqualTo(TEST_SUBJECT);
        assertThat(parsedClaims.get("role", String.class)).isEqualTo(TEST_ROLE);
        assertThat(parsedClaims.get("email", String.class)).isEqualTo(TEST_EMAIL);
    }

    @Test
    void validateTokenWithSecretKey_invalidToken_shouldReturnEmpty() {
        when(keyService.findSecretKey(TEST_KEY_ID)).thenReturn(Optional.of(testSecretKey));

        String invalidToken = "invalid.jwt.token";

        Optional<Jws<Claims>> result = jwtService.validateTokenWithSecretKey(invalidToken, TEST_KEY_ID);

        assertThat(result).isEmpty();
    }

    @Test
    void validateTokenWithSecretKey_wrongKey_shouldReturnEmpty() throws Exception {
        when(keyService.findSecretKey(TEST_KEY_ID)).thenReturn(Optional.of(testSecretKey));

        // Create token with testSecretKey
        String token = jwtService.createTokenWithSecretKey(TEST_KEY_ID, TEST_SUBJECT, null, null);

        // Try to validate with a different key
        KeyGenerator keyGen = KeyGenerator.getInstance("HmacSHA256");
        keyGen.init(256);
        SecretKey differentKey = keyGen.generateKey();
        when(keyService.findSecretKey(TEST_KEY_ID)).thenReturn(Optional.of(differentKey));

        Optional<Jws<Claims>> result = jwtService.validateTokenWithSecretKey(token, TEST_KEY_ID);

        assertThat(result).isEmpty();
    }

    @Test
    void validateTokenWithSecretKey_keyNotFound_shouldReturnEmpty() {
        when(keyService.findSecretKey(TEST_KEY_ID)).thenReturn(Optional.empty());

        String token = "some.jwt.token";

        Optional<Jws<Claims>> result = jwtService.validateTokenWithSecretKey(token, TEST_KEY_ID);

        assertThat(result).isEmpty();
    }

    @Test
    void validateTokenWithSecretKey_expiredToken_shouldReturnEmpty() {
        when(keyService.findSecretKey(TEST_KEY_ID)).thenReturn(Optional.of(testSecretKey));

        // Create token that expired 1 hour ago
        Instant expiresAt = Instant.now().minusSeconds(3600);
        String token = jwtService.createTokenWithSecretKey(TEST_KEY_ID, TEST_SUBJECT, null, expiresAt);

        Optional<Jws<Claims>> result = jwtService.validateTokenWithSecretKey(token, TEST_KEY_ID);

        assertThat(result).isEmpty();
    }

    // ================================================================
    // validateTokenWithSyncKey tests
    // ================================================================

    @Test
    void validateTokenWithSyncKey_validToken_shouldReturnClaims() {
        when(keyService.findSyncKey(TEST_KEY_ID)).thenReturn(Optional.of(testSyncKey));

        Map<String, Object> claims = Map.of("role", TEST_ROLE);
        Instant expiresAt = Instant.now().plusSeconds(3600);
        String token = jwtService.createTokenWithSyncKey(TEST_KEY_ID, TEST_SUBJECT, claims, expiresAt);

        Optional<Jws<Claims>> result = jwtService.validateTokenWithSyncKey(token, TEST_KEY_ID);

        assertThat(result).isPresent();
        Claims parsedClaims = result.get().getPayload();
        assertThat(parsedClaims.getSubject()).isEqualTo(TEST_SUBJECT);
        assertThat(parsedClaims.get("role", String.class)).isEqualTo(TEST_ROLE);
    }

    @Test
    void validateTokenWithSyncKey_invalidToken_shouldReturnEmpty() {
        when(keyService.findSyncKey(TEST_KEY_ID)).thenReturn(Optional.of(testSyncKey));

        String invalidToken = "invalid.jwt.token";

        Optional<Jws<Claims>> result = jwtService.validateTokenWithSyncKey(invalidToken, TEST_KEY_ID);

        assertThat(result).isEmpty();
    }

    @Test
    void validateTokenWithSyncKey_keyNotFound_shouldReturnEmpty() {
        when(keyService.findSyncKey(TEST_KEY_ID)).thenReturn(Optional.empty());

        String token = "some.jwt.token";

        Optional<Jws<Claims>> result = jwtService.validateTokenWithSyncKey(token, TEST_KEY_ID);

        assertThat(result).isEmpty();
    }

    // ================================================================
    // validateTokenWithPublicKey tests
    // ================================================================

    @Test
    void validateTokenWithPublicKey_validToken_shouldReturnClaims() {
        // Create token signed with private key using JJWT directly
        Map<String, Object> claims = Map.of("role", TEST_ROLE);
        Instant expiresAt = Instant.now().plusSeconds(3600);
        
        String token = io.jsonwebtoken.Jwts.builder()
                .subject(TEST_SUBJECT)
                .claims(claims)
                .expiration(java.util.Date.from(expiresAt))
                .signWith(testPrivateKey)
                .compact();

        when(keyService.findPublicKey(TEST_KEY_ID)).thenReturn(Optional.of(testPublicKey));

        Optional<Jws<Claims>> result = jwtService.validateTokenWithPublicKey(token, TEST_KEY_ID);

        assertThat(result).isPresent();
        Claims parsedClaims = result.get().getPayload();
        assertThat(parsedClaims.getSubject()).isEqualTo(TEST_SUBJECT);
        assertThat(parsedClaims.get("role", String.class)).isEqualTo(TEST_ROLE);
    }

    @Test
    void validateTokenWithPublicKey_invalidToken_shouldReturnEmpty() {
        when(keyService.findPublicKey(TEST_KEY_ID)).thenReturn(Optional.of(testPublicKey));

        String invalidToken = "invalid.jwt.token";

        Optional<Jws<Claims>> result = jwtService.validateTokenWithPublicKey(invalidToken, TEST_KEY_ID);

        assertThat(result).isEmpty();
    }

    @Test
    void validateTokenWithPublicKey_wrongKey_shouldReturnEmpty() throws Exception {
        // Create token with testPrivateKey
        String token = io.jsonwebtoken.Jwts.builder()
                .subject(TEST_SUBJECT)
                .signWith(testPrivateKey)
                .compact();

        // Try to validate with a different public key
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        KeyPair differentKeyPair = keyPairGenerator.generateKeyPair();
        PublicKey differentPublicKey = differentKeyPair.getPublic();

        when(keyService.findPublicKey(TEST_KEY_ID)).thenReturn(Optional.of(differentPublicKey));

        Optional<Jws<Claims>> result = jwtService.validateTokenWithPublicKey(token, TEST_KEY_ID);

        assertThat(result).isEmpty();
    }

    @Test
    void validateTokenWithPublicKey_keyNotFound_shouldReturnEmpty() {
        when(keyService.findPublicKey(TEST_KEY_ID)).thenReturn(Optional.empty());

        String token = "some.jwt.token";

        Optional<Jws<Claims>> result = jwtService.validateTokenWithPublicKey(token, TEST_KEY_ID);

        assertThat(result).isEmpty();
    }

    // ================================================================
    // Round-trip tests
    // ================================================================

    @Test
    void roundTrip_secretKey_shouldWorkCorrectly() {
        when(keyService.findSecretKey(TEST_KEY_ID)).thenReturn(Optional.of(testSecretKey));

        Map<String, Object> claims = Map.of("role", TEST_ROLE, "email", TEST_EMAIL);
        Instant expiresAt = Instant.now().plusSeconds(3600);

        String token = jwtService.createTokenWithSecretKey(TEST_KEY_ID, TEST_SUBJECT, claims, expiresAt);
        Optional<Jws<Claims>> result = jwtService.validateTokenWithSecretKey(token, TEST_KEY_ID);

        assertThat(result).isPresent();
        Claims parsedClaims = result.get().getPayload();
        assertThat(parsedClaims.getSubject()).isEqualTo(TEST_SUBJECT);
        assertThat(parsedClaims.get("role", String.class)).isEqualTo(TEST_ROLE);
        assertThat(parsedClaims.get("email", String.class)).isEqualTo(TEST_EMAIL);
    }

    @Test
    void roundTrip_syncKey_shouldWorkCorrectly() {
        when(keyService.findSyncKey(TEST_KEY_ID)).thenReturn(Optional.of(testSyncKey));

        Map<String, Object> claims = Map.of("role", TEST_ROLE);
        Instant expiresAt = Instant.now().plusSeconds(3600);

        String token = jwtService.createTokenWithSyncKey(TEST_KEY_ID, TEST_SUBJECT, claims, expiresAt);
        Optional<Jws<Claims>> result = jwtService.validateTokenWithSyncKey(token, TEST_KEY_ID);

        assertThat(result).isPresent();
        Claims parsedClaims = result.get().getPayload();
        assertThat(parsedClaims.getSubject()).isEqualTo(TEST_SUBJECT);
        assertThat(parsedClaims.get("role", String.class)).isEqualTo(TEST_ROLE);
    }
}
