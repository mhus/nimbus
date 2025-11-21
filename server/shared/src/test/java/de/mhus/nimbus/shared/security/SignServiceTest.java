package de.mhus.nimbus.shared.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SignServiceTest {

    @Mock
    private KeyService keyService;

    private SignService signService;

    private PrivateKey testPrivateKey;
    private PublicKey testPublicKey;

    private static final String TEST_KEY_ID = "owner:id-123";
    private static final String TEST_TEXT = "Important message to sign";

    @BeforeEach
    void setUp() throws Exception {
        signService = new SignService(keyService);
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("EC");
        kpg.initialize(new java.security.spec.ECGenParameterSpec("secp256r1"));
        KeyPair kp = kpg.generateKeyPair();
        testPrivateKey = kp.getPrivate();
        testPublicKey = kp.getPublic();
    }

    // ================================================================
    // sign() tests
    // ================================================================

    @Test
    void sign_withPrivateKey_shouldReturnAsymmetricSignature() {
        when(keyService.findPrivateKey(KeyType.UNIVERSE, TEST_KEY_ID)).thenReturn(Optional.of(testPrivateKey));
        String signature = signService.sign(TEST_TEXT, TEST_KEY_ID);
        assertThat(signature).contains("ECDSA");
    }

    @Test
    void sign_noKeyFound_shouldThrowException() {
        when(keyService.findPrivateKey(KeyType.UNIVERSE, TEST_KEY_ID)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> signService.sign(TEST_TEXT, TEST_KEY_ID))
            .isInstanceOf(SignService.SignatureException.class)
            .hasMessageContaining("No asymmetric private key");
    }

    @Test
    void sign_sameTextDifferentKeys_shouldProduceDifferentSignatures() throws Exception {
        when(keyService.findPrivateKey(KeyType.UNIVERSE, TEST_KEY_ID)).thenReturn(Optional.of(testPrivateKey));
        String signature1 = signService.sign(TEST_TEXT, TEST_KEY_ID);

        KeyPairGenerator kpg = KeyPairGenerator.getInstance("EC");
        kpg.initialize(new java.security.spec.ECGenParameterSpec("secp256r1"));
        KeyPair anotherKp = kpg.generateKeyPair();
        when(keyService.findPrivateKey(KeyType.UNIVERSE, TEST_KEY_ID)).thenReturn(Optional.of(anotherKp.getPrivate()));
        String signature2 = signService.sign(TEST_TEXT, TEST_KEY_ID);

        assertThat(signature1).isNotEqualTo(signature2);
    }

    @Test
    void sign_differentTextSameKey_shouldProduceDifferentSignatures() {
        when(keyService.findPrivateKey(KeyType.UNIVERSE, TEST_KEY_ID)).thenReturn(Optional.of(testPrivateKey));

        String signature1 = signService.sign("Text 1", TEST_KEY_ID);
        String signature2 = signService.sign("Text 2", TEST_KEY_ID);

        assertThat(signature1).isNotEqualTo(signature2);
    }

    // ================================================================
    // validate() tests
    // ================================================================

    @Test
    void validate_validSignature_shouldReturnTrue() {
        when(keyService.findPrivateKey(KeyType.UNIVERSE, TEST_KEY_ID)).thenReturn(Optional.of(testPrivateKey));
        when(keyService.findPublicKey(KeyType.UNIVERSE, TEST_KEY_ID)).thenReturn(Optional.of(testPublicKey));
        String signature = signService.sign(TEST_TEXT, TEST_KEY_ID);
        boolean valid = signService.validate(TEST_TEXT, signature);
        assertThat(valid).isTrue();
    }

    @Test
    void validate_asymmetricSignature_shouldReturnTrue() {
        when(keyService.findPrivateKey(KeyType.UNIVERSE, TEST_KEY_ID)).thenReturn(Optional.of(testPrivateKey));
        when(keyService.findPublicKey(KeyType.UNIVERSE, TEST_KEY_ID)).thenReturn(Optional.of(testPublicKey));
        String signature = signService.sign(TEST_TEXT, TEST_KEY_ID);
        boolean valid = signService.validate(TEST_TEXT, signature);
        assertThat(valid).isTrue();
    }

    @Test
    void validate_modifiedText_shouldReturnFalse() {
        when(keyService.findPrivateKey(KeyType.UNIVERSE, TEST_KEY_ID)).thenReturn(Optional.of(testPrivateKey));
        when(keyService.findPublicKey(KeyType.UNIVERSE, TEST_KEY_ID)).thenReturn(Optional.of(testPublicKey));
        String signature = signService.sign(TEST_TEXT, TEST_KEY_ID);
        boolean valid = signService.validate("Changed", signature);
        assertThat(valid).isFalse();
    }

    @Test
    void validate_modifiedSignature_shouldReturnFalse() {
        when(keyService.findPrivateKey(KeyType.UNIVERSE, TEST_KEY_ID)).thenReturn(Optional.of(testPrivateKey));
        when(keyService.findPublicKey(KeyType.UNIVERSE, TEST_KEY_ID)).thenReturn(Optional.of(testPublicKey));

        String signature = signService.sign(TEST_TEXT, TEST_KEY_ID);
        String modifiedSignature = signature + "extra";
        boolean isValid = signService.validate(TEST_TEXT, modifiedSignature);

        assertThat(isValid).isFalse();
    }

    @Test
    void validate_wrongPublicKey_shouldReturnFalse() throws Exception {
        when(keyService.findPrivateKey(KeyType.UNIVERSE, TEST_KEY_ID)).thenReturn(Optional.of(testPrivateKey));
        String signature = signService.sign(TEST_TEXT, TEST_KEY_ID);
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("EC");
        kpg.initialize(new java.security.spec.ECGenParameterSpec("secp256r1"));
        PublicKey otherPublic = kpg.generateKeyPair().getPublic();
        when(keyService.findPublicKey(KeyType.UNIVERSE, TEST_KEY_ID)).thenReturn(Optional.of(otherPublic));
        boolean valid = signService.validate(TEST_TEXT, signature);
        assertThat(valid).isFalse();
    }

    @Test
    void validate_keyNotFound_shouldReturnFalse() {
        boolean valid = signService.validate(TEST_TEXT, "invalid:format");
        assertThat(valid).isFalse();
    }

    @Test
    void validate_invalidSignatureFormat_tooFewParts_shouldReturnFalse() {
        boolean isValid = signService.validate(TEST_TEXT, "invalid:format");

        assertThat(isValid).isFalse();
    }

    @Test
    void validate_invalidSignatureFormat_emptyString_shouldReturnFalse() {
        boolean isValid = signService.validate(TEST_TEXT, "");

        assertThat(isValid).isFalse();
    }

    // ================================================================
    // Round-trip tests
    // ================================================================

    @Test
    void roundTrip_withPrivateKey_shouldWorkCorrectly() {
        when(keyService.findPrivateKey(KeyType.UNIVERSE, TEST_KEY_ID)).thenReturn(Optional.of(testPrivateKey));
        when(keyService.findPublicKey(KeyType.UNIVERSE, TEST_KEY_ID)).thenReturn(Optional.of(testPublicKey));
        String signature = signService.sign(TEST_TEXT, TEST_KEY_ID);
        boolean isValid = signService.validate(TEST_TEXT, signature);
        assertThat(isValid).isTrue();
    }

    @Test
    void roundTrip_emptyText_shouldWorkCorrectly() {
        when(keyService.findPrivateKey(KeyType.UNIVERSE, TEST_KEY_ID)).thenReturn(Optional.of(testPrivateKey));
        when(keyService.findPublicKey(KeyType.UNIVERSE, TEST_KEY_ID)).thenReturn(Optional.of(testPublicKey));

        String signature = signService.sign("", TEST_KEY_ID);
        boolean isValid = signService.validate("", signature);

        assertThat(isValid).isTrue();
    }

    @Test
    void roundTrip_textWithSpecialCharacters_shouldWorkCorrectly() {
        when(keyService.findPrivateKey(KeyType.UNIVERSE, TEST_KEY_ID)).thenReturn(Optional.of(testPrivateKey));
        when(keyService.findPublicKey(KeyType.UNIVERSE, TEST_KEY_ID)).thenReturn(Optional.of(testPublicKey));

        String specialText = "Text with special chars: äöü 日本語 emoji 🎉 \n\t";
        String signature = signService.sign(specialText, TEST_KEY_ID);
        boolean isValid = signService.validate(specialText, signature);

        assertThat(isValid).isTrue();
    }

    @Test
    void roundTrip_longText_shouldWorkCorrectly() {
        when(keyService.findPrivateKey(KeyType.UNIVERSE, TEST_KEY_ID)).thenReturn(Optional.of(testPrivateKey));
        when(keyService.findPublicKey(KeyType.UNIVERSE, TEST_KEY_ID)).thenReturn(Optional.of(testPublicKey));

        String longText = "a".repeat(10000);
        String signature = signService.sign(longText, TEST_KEY_ID);
        boolean isValid = signService.validate(longText, signature);

        assertThat(isValid).isTrue();
    }

    // ================================================================
    // Signature format tests
    // ================================================================

    @Test
    void signatureFormat_shouldContainKeyId() {
        when(keyService.findPrivateKey(KeyType.UNIVERSE, TEST_KEY_ID)).thenReturn(Optional.of(testPrivateKey));

        String signature = signService.sign(TEST_TEXT, TEST_KEY_ID);
        String[] parts = signature.split(":");
        String decodedKeyId = new String(java.util.Base64.getDecoder().decode(parts[0]), java.nio.charset.StandardCharsets.UTF_8);
        assertThat(decodedKeyId).isEqualTo(TEST_KEY_ID);
    }

    @Test
    void signatureFormat_shouldContainAlgorithm() {
        when(keyService.findPrivateKey(KeyType.UNIVERSE, TEST_KEY_ID)).thenReturn(Optional.of(testPrivateKey));
        String signature = signService.sign(TEST_TEXT, TEST_KEY_ID);
        String[] parts = signature.split(":");
        assertThat(parts[1]).contains("ECDSA");
    }

    @Test
    void signatureFormat_shouldContainBase64Signature() {
        when(keyService.findPrivateKey(KeyType.UNIVERSE, TEST_KEY_ID)).thenReturn(Optional.of(testPrivateKey));
        String signature = signService.sign(TEST_TEXT, TEST_KEY_ID);
        String[] parts = signature.split(":");
        assertThat(parts).hasSize(3);
        assertThat(parts[2]).isNotEmpty();
        assertThat(java.util.Base64.getDecoder().decode(parts[2])).isNotEmpty();
    }
}
