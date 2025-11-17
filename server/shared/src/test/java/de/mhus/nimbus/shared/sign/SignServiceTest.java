package de.mhus.nimbus.shared.sign;

import de.mhus.nimbus.shared.keys.KeyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SignServiceTest {

    @Mock
    private KeyService keyService;

    private SignService signService;

    private SecretKey testSyncKey;
    private SecretKey testSecretKey;

    private static final String TEST_KEY_ID = "owner:uuid-123";
    private static final String TEST_TEXT = "Important message to sign";

    @BeforeEach
    void setUp() throws Exception {
        signService = new SignService(keyService);

        // Generate test keys
        KeyGenerator keyGen = KeyGenerator.getInstance("HmacSHA256");
        keyGen.init(256);
        testSyncKey = keyGen.generateKey();
        testSecretKey = keyGen.generateKey();
    }

    // ================================================================
    // sign() tests
    // ================================================================

    @Test
    void sign_withSyncKey_shouldReturnSignature() {
        when(keyService.findSyncKey(TEST_KEY_ID)).thenReturn(Optional.of(testSyncKey));

        String signature = signService.sign(TEST_TEXT, TEST_KEY_ID);

        assertThat(signature).isNotNull();
        assertThat(signature).isNotEmpty();
        // Signature format: keyIdBase64:algorithm:signatureBase64
        assertThat(signature.split(":")).hasSize(3);
        String expectedKeyIdBase64 = java.util.Base64.getEncoder().encodeToString(TEST_KEY_ID.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        assertThat(signature).startsWith(expectedKeyIdBase64 + ":HmacSHA256:");
    }

    @Test
    void sign_withSecretKey_shouldReturnSignature() {
        when(keyService.findSyncKey(TEST_KEY_ID)).thenReturn(Optional.empty());
        when(keyService.findSecretKey(TEST_KEY_ID)).thenReturn(Optional.of(testSecretKey));

        String signature = signService.sign(TEST_TEXT, TEST_KEY_ID);

        assertThat(signature).isNotNull();
        assertThat(signature).isNotEmpty();
        assertThat(signature.split(":")).hasSize(3);
        String expectedKeyIdBase64 = java.util.Base64.getEncoder().encodeToString(TEST_KEY_ID.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        assertThat(signature).startsWith(expectedKeyIdBase64 + ":HmacSHA256:");
    }

    @Test
    void sign_prefersSyncKeyOverSecretKey() {
        when(keyService.findSyncKey(TEST_KEY_ID)).thenReturn(Optional.of(testSyncKey));

        String signature = signService.sign(TEST_TEXT, TEST_KEY_ID);

        assertThat(signature).isNotNull();
        // The signature should be created with syncKey (we can verify by checking it validates with syncKey)
        boolean isValid = signService.validate(TEST_TEXT, signature);
        assertThat(isValid).isTrue();
    }

    @Test
    void sign_noKeyFound_shouldThrowException() {
        when(keyService.findSyncKey(TEST_KEY_ID)).thenReturn(Optional.empty());
        when(keyService.findSecretKey(TEST_KEY_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> signService.sign(TEST_TEXT, TEST_KEY_ID))
                .isInstanceOf(SignService.SignatureException.class)
                .hasMessageContaining("No symmetric key found for keyId");
    }

    @Test
    void sign_sameTextDifferentKeys_shouldProduceDifferentSignatures() throws Exception {
        KeyGenerator keyGen = KeyGenerator.getInstance("HmacSHA256");
        keyGen.init(256);
        SecretKey anotherKey = keyGen.generateKey();

        when(keyService.findSyncKey(TEST_KEY_ID)).thenReturn(Optional.of(testSyncKey));
        String signature1 = signService.sign(TEST_TEXT, TEST_KEY_ID);

        when(keyService.findSyncKey(TEST_KEY_ID)).thenReturn(Optional.of(anotherKey));
        String signature2 = signService.sign(TEST_TEXT, TEST_KEY_ID);

        assertThat(signature1).isNotEqualTo(signature2);
    }

    @Test
    void sign_differentTextSameKey_shouldProduceDifferentSignatures() {
        when(keyService.findSyncKey(TEST_KEY_ID)).thenReturn(Optional.of(testSyncKey));

        String signature1 = signService.sign("Text 1", TEST_KEY_ID);
        String signature2 = signService.sign("Text 2", TEST_KEY_ID);

        assertThat(signature1).isNotEqualTo(signature2);
    }

    // ================================================================
    // validate() tests
    // ================================================================

    @Test
    void validate_validSignature_shouldReturnTrue() {
        when(keyService.findSyncKey(TEST_KEY_ID)).thenReturn(Optional.of(testSyncKey));

        String signature = signService.sign(TEST_TEXT, TEST_KEY_ID);
        boolean isValid = signService.validate(TEST_TEXT, signature);

        assertThat(isValid).isTrue();
    }

    @Test
    void validate_validSignatureWithSecretKey_shouldReturnTrue() {
        when(keyService.findSyncKey(TEST_KEY_ID)).thenReturn(Optional.empty());
        when(keyService.findSecretKey(TEST_KEY_ID)).thenReturn(Optional.of(testSecretKey));

        String signature = signService.sign(TEST_TEXT, TEST_KEY_ID);
        boolean isValid = signService.validate(TEST_TEXT, signature);

        assertThat(isValid).isTrue();
    }

    @Test
    void validate_modifiedText_shouldReturnFalse() {
        when(keyService.findSyncKey(TEST_KEY_ID)).thenReturn(Optional.of(testSyncKey));

        String signature = signService.sign(TEST_TEXT, TEST_KEY_ID);
        boolean isValid = signService.validate("Modified text", signature);

        assertThat(isValid).isFalse();
    }

    @Test
    void validate_modifiedSignature_shouldReturnFalse() {
        when(keyService.findSyncKey(TEST_KEY_ID)).thenReturn(Optional.of(testSyncKey));

        String signature = signService.sign(TEST_TEXT, TEST_KEY_ID);
        String modifiedSignature = signature + "extra";
        boolean isValid = signService.validate(TEST_TEXT, modifiedSignature);

        assertThat(isValid).isFalse();
    }

    @Test
    void validate_wrongKey_shouldReturnFalse() throws Exception {
        when(keyService.findSyncKey(TEST_KEY_ID)).thenReturn(Optional.of(testSyncKey));
        String signature = signService.sign(TEST_TEXT, TEST_KEY_ID);

        // Try to validate with different key
        KeyGenerator keyGen = KeyGenerator.getInstance("HmacSHA256");
        keyGen.init(256);
        SecretKey differentKey = keyGen.generateKey();
        when(keyService.findSyncKey(TEST_KEY_ID)).thenReturn(Optional.of(differentKey));

        boolean isValid = signService.validate(TEST_TEXT, signature);

        assertThat(isValid).isFalse();
    }

    @Test
    void validate_keyNotFound_shouldReturnFalse() {
        when(keyService.findSyncKey(TEST_KEY_ID)).thenReturn(Optional.of(testSyncKey));
        String signature = signService.sign(TEST_TEXT, TEST_KEY_ID);

        // Remove keys
        when(keyService.findSyncKey(TEST_KEY_ID)).thenReturn(Optional.empty());
        when(keyService.findSecretKey(TEST_KEY_ID)).thenReturn(Optional.empty());

        boolean isValid = signService.validate(TEST_TEXT, signature);

        assertThat(isValid).isFalse();
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

    @Test
    void validate_invalidBase64InSignature_shouldReturnFalse() {
        when(keyService.findSyncKey(TEST_KEY_ID)).thenReturn(Optional.of(testSyncKey));

        String keyIdBase64 = java.util.Base64.getEncoder().encodeToString(TEST_KEY_ID.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        String invalidSignature = keyIdBase64 + ":HmacSHA256:invalid!!!base64";
        boolean isValid = signService.validate(TEST_TEXT, invalidSignature);

        assertThat(isValid).isFalse();
    }

    // ================================================================
    // Round-trip tests
    // ================================================================

    @Test
    void roundTrip_withSyncKey_shouldWorkCorrectly() {
        when(keyService.findSyncKey(TEST_KEY_ID)).thenReturn(Optional.of(testSyncKey));

        String signature = signService.sign(TEST_TEXT, TEST_KEY_ID);
        boolean isValid = signService.validate(TEST_TEXT, signature);

        assertThat(isValid).isTrue();
    }

    @Test
    void roundTrip_withSecretKey_shouldWorkCorrectly() {
        when(keyService.findSyncKey(TEST_KEY_ID)).thenReturn(Optional.empty());
        when(keyService.findSecretKey(TEST_KEY_ID)).thenReturn(Optional.of(testSecretKey));

        String signature = signService.sign(TEST_TEXT, TEST_KEY_ID);
        boolean isValid = signService.validate(TEST_TEXT, signature);

        assertThat(isValid).isTrue();
    }

    @Test
    void roundTrip_emptyText_shouldWorkCorrectly() {
        when(keyService.findSyncKey(TEST_KEY_ID)).thenReturn(Optional.of(testSyncKey));

        String signature = signService.sign("", TEST_KEY_ID);
        boolean isValid = signService.validate("", signature);

        assertThat(isValid).isTrue();
    }

    @Test
    void roundTrip_textWithSpecialCharacters_shouldWorkCorrectly() {
        when(keyService.findSyncKey(TEST_KEY_ID)).thenReturn(Optional.of(testSyncKey));

        String specialText = "Text with special chars: äöü 日本語 emoji 🎉 \n\t";
        String signature = signService.sign(specialText, TEST_KEY_ID);
        boolean isValid = signService.validate(specialText, signature);

        assertThat(isValid).isTrue();
    }

    @Test
    void roundTrip_longText_shouldWorkCorrectly() {
        when(keyService.findSyncKey(TEST_KEY_ID)).thenReturn(Optional.of(testSyncKey));

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
        when(keyService.findSyncKey(TEST_KEY_ID)).thenReturn(Optional.of(testSyncKey));

        String signature = signService.sign(TEST_TEXT, TEST_KEY_ID);
        String[] parts = signature.split(":");

        // Decode the Base64-encoded keyId and compare
        String decodedKeyId = new String(java.util.Base64.getDecoder().decode(parts[0]), java.nio.charset.StandardCharsets.UTF_8);
        assertThat(decodedKeyId).isEqualTo(TEST_KEY_ID);
    }

    @Test
    void signatureFormat_shouldContainAlgorithm() {
        when(keyService.findSyncKey(TEST_KEY_ID)).thenReturn(Optional.of(testSyncKey));

        String signature = signService.sign(TEST_TEXT, TEST_KEY_ID);
        String[] parts = signature.split(":");

        assertThat(parts[1]).isEqualTo("HmacSHA256");
    }

    @Test
    void signatureFormat_shouldContainBase64Signature() {
        when(keyService.findSyncKey(TEST_KEY_ID)).thenReturn(Optional.of(testSyncKey));

        String signature = signService.sign(TEST_TEXT, TEST_KEY_ID);
        String[] parts = signature.split(":");

        assertThat(parts).hasSize(3); // keyIdBase64:algorithm:signatureBase64
        assertThat(parts[2]).isNotEmpty();
        // Verify it's valid base64 by trying to decode
        assertThat(java.util.Base64.getDecoder().decode(parts[2])).isNotEmpty();
    }

    // ================================================================
    // Cross-validation tests
    // ================================================================

    @Test
    void validate_signatureCreatedBySyncKey_canBeValidatedBySecretKeyIfSame() throws Exception {
        // Use the same key for both providers
        when(keyService.findSyncKey(TEST_KEY_ID)).thenReturn(Optional.of(testSyncKey));
        when(keyService.findSecretKey(TEST_KEY_ID)).thenReturn(Optional.of(testSyncKey));

        String signature = signService.sign(TEST_TEXT, TEST_KEY_ID);

        // Remove sync key, leave only secret key
        when(keyService.findSyncKey(TEST_KEY_ID)).thenReturn(Optional.empty());
        when(keyService.findSecretKey(TEST_KEY_ID)).thenReturn(Optional.of(testSyncKey));

        boolean isValid = signService.validate(TEST_TEXT, signature);

        assertThat(isValid).isTrue();
    }
}
