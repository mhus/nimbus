package de.mhus.nimbus.shared.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.ECGenParameterSpec;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtServiceTest {

    @Mock
    private KeyService keyService;

    private JwtService jwtService;

    private PublicKey testPublicKey;
    private PrivateKey testPrivateKey;

    private static final String TEST_KEY_ID = "owner:id-123";
    private static final String TEST_SUBJECT = "user123";
    private static final String TEST_ROLE = "admin";
    private static final String TEST_EMAIL = "user@example.com";

    @BeforeEach
    void setUp() throws Exception {
        jwtService = new JwtService(keyService);
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("EC");
        keyPairGenerator.initialize(new ECGenParameterSpec("secp256r1"));
        KeyPair keyPair = keyPairGenerator.generateKeyPair();
        testPublicKey = keyPair.getPublic();
        testPrivateKey = keyPair.getPrivate();
    }

    // ================================================================
    // createTokenWithSecretKey (asymmetric priority) tests
    // ================================================================

    @Test
    void createTokenWithSecretKey_privateKeyPriority_shouldCreateAsymmetricToken() {
        when(keyService.findPrivateKey(KeyType.UNIVERSE, TEST_KEY_ID)).thenReturn(Optional.of(testPrivateKey));

        Map<String, Object> claims = Map.of("role", TEST_ROLE, "email", TEST_EMAIL);
        Instant expiresAt = Instant.now().plusSeconds(3600);

        String token = jwtService.createTokenWithSecretKey(TEST_KEY_ID, TEST_SUBJECT, claims, expiresAt);

        assertThat(token).isNotNull().isNotEmpty();
        assertThat(token.split("\\.")).hasSize(3);
    }

    @Test
    void createTokenWithSecretKey_eccPrivateKey_shouldCreateToken() {
        when(keyService.findPrivateKey(KeyType.UNIVERSE, TEST_KEY_ID)).thenReturn(Optional.of(testPrivateKey));
        String token = jwtService.createTokenWithSecretKey(TEST_KEY_ID, TEST_SUBJECT, Map.of("role", TEST_ROLE), Instant.now().plusSeconds(3600));
        assertThat(token).isNotNull().isNotEmpty();
    }

    @Test
    void createTokenWithSecretKey_missingPrivateKey_shouldThrow() {
        when(keyService.findPrivateKey(KeyType.UNIVERSE, TEST_KEY_ID)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> jwtService.createTokenWithSecretKey(TEST_KEY_ID, TEST_SUBJECT, null, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("ECC private key not found");
    }

    // ================================================================
    // validateTokenWithPublicKey tests
    // ================================================================

    @Test
    void validateTokenWithPublicKey_validToken_shouldReturnClaims() {
        when(keyService.findPrivateKey(KeyType.UNIVERSE, TEST_KEY_ID)).thenReturn(Optional.of(testPrivateKey));
        String token = jwtService.createTokenWithSecretKey(TEST_KEY_ID, TEST_SUBJECT, Map.of("role", TEST_ROLE), Instant.now().plusSeconds(3600));
        when(keyService.findPublicKey(KeyType.UNIVERSE, TEST_KEY_ID)).thenReturn(Optional.of(testPublicKey));
        Optional<Jws<Claims>> result = jwtService.validateTokenWithPublicKey(token, TEST_KEY_ID);
        assertThat(result).isPresent();
    }

    @Test
    void validateTokenWithSecretKey_eccToken_shouldReturnClaims() {
        when(keyService.findPrivateKey(KeyType.UNIVERSE, TEST_KEY_ID)).thenReturn(Optional.of(testPrivateKey));
        when(keyService.findPublicKey(KeyType.UNIVERSE, TEST_KEY_ID)).thenReturn(Optional.of(testPublicKey));
        String token = jwtService.createTokenWithSecretKey(TEST_KEY_ID, TEST_SUBJECT, Map.of("role", TEST_ROLE), Instant.now().plusSeconds(3600));
        Optional<Jws<Claims>> result = jwtService.validateTokenWithSecretKey(token, TEST_KEY_ID);
        assertThat(result).isPresent();
    }

    @Test
    void validateTokenWithSecretKey_wrongPublicKey_shouldReturnEmpty() throws Exception {
        when(keyService.findPrivateKey(KeyType.UNIVERSE, TEST_KEY_ID)).thenReturn(Optional.of(testPrivateKey));
        String token = jwtService.createTokenWithSecretKey(TEST_KEY_ID, TEST_SUBJECT, null, null);
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("EC");
        keyPairGenerator.initialize(new java.security.spec.ECGenParameterSpec("secp256r1"));
        PublicKey differentPublic = keyPairGenerator.generateKeyPair().getPublic();
        when(keyService.findPublicKey(KeyType.UNIVERSE, TEST_KEY_ID)).thenReturn(Optional.of(differentPublic));
        Optional<Jws<Claims>> result = jwtService.validateTokenWithSecretKey(token, TEST_KEY_ID);
        assertThat(result).isEmpty();
    }

    @Test
    void validateTokenWithSecretKey_noKeyFound_shouldReturnEmpty() {
        when(keyService.findPublicKey(KeyType.UNIVERSE, TEST_KEY_ID)).thenReturn(Optional.empty());
        Optional<Jws<Claims>> result = jwtService.validateTokenWithSecretKey("invalid.jwt.token", TEST_KEY_ID);
        assertThat(result).isEmpty();
    }

    // ================================================================
    // Round-trip tests
    // ================================================================

    @Test
    void roundTrip_asymmetric_shouldWork() {
        when(keyService.findPrivateKey(KeyType.UNIVERSE, TEST_KEY_ID)).thenReturn(Optional.of(testPrivateKey));
        when(keyService.findPublicKey(KeyType.UNIVERSE, TEST_KEY_ID)).thenReturn(Optional.of(testPublicKey));
        String token = jwtService.createTokenWithSecretKey(TEST_KEY_ID, TEST_SUBJECT, Map.of("role", TEST_ROLE), Instant.now().plusSeconds(3600));
        Optional<Jws<Claims>> result = jwtService.validateTokenWithPublicKey(token, TEST_KEY_ID);
        assertThat(result).isPresent();
    }
}
