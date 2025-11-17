package de.mhus.nimbus.shared.cipher;

import de.mhus.nimbus.shared.keys.IKeyService;
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
class CipherServiceTest {

    @Mock
    private IKeyService keyService;

    private CipherService cipherService;

    private SecretKey testSyncKey;
    private SecretKey testSecretKey;

    private static final String TEST_KEY_ID = "owner:uuid-123";
    private static final String TEST_TEXT = "Sensitive data to encrypt";

    @BeforeEach
    void setUp() throws Exception {
        cipherService = new CipherService(keyService);

        // Generate test keys for AES
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(256);
        testSyncKey = keyGen.generateKey();
        testSecretKey = keyGen.generateKey();
    }

    // ================================================================
    // encrypt() tests
    // ================================================================

    @Test
    void encrypt_withSyncKey_shouldReturnCipher() {
        when(keyService.findSyncKey(TEST_KEY_ID)).thenReturn(Optional.of(testSyncKey));

        String cipher = cipherService.encrypt(TEST_TEXT, TEST_KEY_ID);

        assertThat(cipher).isNotNull();
        assertThat(cipher).isNotEmpty();
        // Cipher format: keyIdBase64:algorithm:encryptedDataBase64:ivBase64
        assertThat(cipher.split(":")).hasSize(4);
        String expectedKeyIdBase64 = java.util.Base64.getEncoder().encodeToString(TEST_KEY_ID.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        assertThat(cipher).startsWith(expectedKeyIdBase64 + ":AES/GCM/NoPadding:");
    }

    @Test
    void encrypt_withSecretKey_shouldReturnCipher() {
        when(keyService.findSyncKey(TEST_KEY_ID)).thenReturn(Optional.empty());
        when(keyService.findSecretKey(TEST_KEY_ID)).thenReturn(Optional.of(testSecretKey));

        String cipher = cipherService.encrypt(TEST_TEXT, TEST_KEY_ID);

        assertThat(cipher).isNotNull();
        assertThat(cipher).isNotEmpty();
        assertThat(cipher.split(":")).hasSize(4);
        String expectedKeyIdBase64 = java.util.Base64.getEncoder().encodeToString(TEST_KEY_ID.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        assertThat(cipher).startsWith(expectedKeyIdBase64 + ":AES/GCM/NoPadding:");
    }

    @Test
    void encrypt_prefersSyncKeyOverSecretKey() {
        when(keyService.findSyncKey(TEST_KEY_ID)).thenReturn(Optional.of(testSyncKey));

        String cipher = cipherService.encrypt(TEST_TEXT, TEST_KEY_ID);

        assertThat(cipher).isNotNull();
        // The cipher should be created with syncKey (we can verify by decrypting with syncKey)
        String decrypted = cipherService.decrypt(cipher);
        assertThat(decrypted).isEqualTo(TEST_TEXT);
    }

    @Test
    void encrypt_noKeyFound_shouldThrowException() {
        when(keyService.findSyncKey(TEST_KEY_ID)).thenReturn(Optional.empty());
        when(keyService.findSecretKey(TEST_KEY_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cipherService.encrypt(TEST_TEXT, TEST_KEY_ID))
                .isInstanceOf(CipherService.CipherException.class)
                .hasMessageContaining("No symmetric key found for keyId");
    }

    @Test
    void encrypt_sameTextMultipleTimes_shouldProduceDifferentCiphers() {
        when(keyService.findSyncKey(TEST_KEY_ID)).thenReturn(Optional.of(testSyncKey));

        String cipher1 = cipherService.encrypt(TEST_TEXT, TEST_KEY_ID);
        String cipher2 = cipherService.encrypt(TEST_TEXT, TEST_KEY_ID);

        // Should be different because of random IV
        assertThat(cipher1).isNotEqualTo(cipher2);
    }

    @Test
    void encrypt_differentTextSameKey_shouldProduceDifferentCiphers() {
        when(keyService.findSyncKey(TEST_KEY_ID)).thenReturn(Optional.of(testSyncKey));

        String cipher1 = cipherService.encrypt("Text 1", TEST_KEY_ID);
        String cipher2 = cipherService.encrypt("Text 2", TEST_KEY_ID);

        assertThat(cipher1).isNotEqualTo(cipher2);
    }

    @Test
    void encrypt_emptyText_shouldReturnCipher() {
        when(keyService.findSyncKey(TEST_KEY_ID)).thenReturn(Optional.of(testSyncKey));

        String cipher = cipherService.encrypt("", TEST_KEY_ID);

        assertThat(cipher).isNotNull();
        assertThat(cipher).isNotEmpty();
    }

    // ================================================================
    // decrypt() tests
    // ================================================================

    @Test
    void decrypt_validCipher_shouldReturnOriginalText() {
        when(keyService.findSyncKey(TEST_KEY_ID)).thenReturn(Optional.of(testSyncKey));

        String cipher = cipherService.encrypt(TEST_TEXT, TEST_KEY_ID);
        String decrypted = cipherService.decrypt(cipher);

        assertThat(decrypted).isEqualTo(TEST_TEXT);
    }

    @Test
    void decrypt_validCipherWithSecretKey_shouldReturnOriginalText() {
        when(keyService.findSyncKey(TEST_KEY_ID)).thenReturn(Optional.empty());
        when(keyService.findSecretKey(TEST_KEY_ID)).thenReturn(Optional.of(testSecretKey));

        String cipher = cipherService.encrypt(TEST_TEXT, TEST_KEY_ID);
        String decrypted = cipherService.decrypt(cipher);

        assertThat(decrypted).isEqualTo(TEST_TEXT);
    }

    @Test
    void decrypt_modifiedCipher_shouldThrowException() {
        when(keyService.findSyncKey(TEST_KEY_ID)).thenReturn(Optional.of(testSyncKey));

        String cipher = cipherService.encrypt(TEST_TEXT, TEST_KEY_ID);
        String modifiedCipher = cipher + "extra";

        assertThatThrownBy(() -> cipherService.decrypt(modifiedCipher))
                .isInstanceOf(CipherService.CipherException.class);
    }

    @Test
    void decrypt_wrongKey_shouldThrowException() throws Exception {
        when(keyService.findSyncKey(TEST_KEY_ID)).thenReturn(Optional.of(testSyncKey));
        String cipher = cipherService.encrypt(TEST_TEXT, TEST_KEY_ID);

        // Try to decrypt with different key
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(256);
        SecretKey differentKey = keyGen.generateKey();
        when(keyService.findSyncKey(TEST_KEY_ID)).thenReturn(Optional.of(differentKey));

        assertThatThrownBy(() -> cipherService.decrypt(cipher))
                .isInstanceOf(CipherService.CipherException.class);
    }

    @Test
    void decrypt_keyNotFound_shouldThrowException() {
        when(keyService.findSyncKey(TEST_KEY_ID)).thenReturn(Optional.of(testSyncKey));
        String cipher = cipherService.encrypt(TEST_TEXT, TEST_KEY_ID);

        // Remove keys
        when(keyService.findSyncKey(TEST_KEY_ID)).thenReturn(Optional.empty());
        when(keyService.findSecretKey(TEST_KEY_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cipherService.decrypt(cipher))
                .isInstanceOf(CipherService.CipherException.class)
                .hasMessageContaining("No key found for keyId");
    }

    @Test
    void decrypt_invalidCipherFormat_tooFewParts_shouldThrowException() {
        assertThatThrownBy(() -> cipherService.decrypt("invalid:format:three"))
                .isInstanceOf(CipherService.CipherException.class)
                .hasMessageContaining("Invalid cipher format");
    }

    @Test
    void decrypt_invalidCipherFormat_emptyString_shouldThrowException() {
        assertThatThrownBy(() -> cipherService.decrypt(""))
                .isInstanceOf(CipherService.CipherException.class);
    }

    @Test
    void decrypt_invalidBase64InCipher_shouldThrowException() {
        when(keyService.findSyncKey(TEST_KEY_ID)).thenReturn(Optional.of(testSyncKey));

        String keyIdBase64 = java.util.Base64.getEncoder().encodeToString(TEST_KEY_ID.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        String invalidCipher = keyIdBase64 + ":AES/GCM/NoPadding:invalid!!!base64:invalid!!!base64";
        
        assertThatThrownBy(() -> cipherService.decrypt(invalidCipher))
                .isInstanceOf(CipherService.CipherException.class);
    }

    @Test
    void decrypt_tamperedEncryptedData_shouldThrowException() {
        when(keyService.findSyncKey(TEST_KEY_ID)).thenReturn(Optional.of(testSyncKey));

        String cipher = cipherService.encrypt(TEST_TEXT, TEST_KEY_ID);
        String[] parts = cipher.split(":");
        // Tamper with encrypted data part
        String tamperedCipher = parts[0] + ":" + parts[1] + ":" + parts[2] + ":AAAA" + parts[3];

        assertThatThrownBy(() -> cipherService.decrypt(tamperedCipher))
                .isInstanceOf(CipherService.CipherException.class);
    }

    @Test
    void decrypt_tamperedIV_shouldThrowException() {
        when(keyService.findSyncKey(TEST_KEY_ID)).thenReturn(Optional.of(testSyncKey));

        String cipher = cipherService.encrypt(TEST_TEXT, TEST_KEY_ID);
        String[] parts = cipher.split(":");
        // Tamper with IV part
        String tamperedCipher = parts[0] + ":" + parts[1] + ":" + parts[2] + ":" + parts[3] + "AAAA";

        assertThatThrownBy(() -> cipherService.decrypt(tamperedCipher))
                .isInstanceOf(CipherService.CipherException.class);
    }

    // ================================================================
    // Round-trip tests
    // ================================================================

    @Test
    void roundTrip_withSyncKey_shouldWorkCorrectly() {
        when(keyService.findSyncKey(TEST_KEY_ID)).thenReturn(Optional.of(testSyncKey));

        String cipher = cipherService.encrypt(TEST_TEXT, TEST_KEY_ID);
        String decrypted = cipherService.decrypt(cipher);

        assertThat(decrypted).isEqualTo(TEST_TEXT);
    }

    @Test
    void roundTrip_withSecretKey_shouldWorkCorrectly() {
        when(keyService.findSyncKey(TEST_KEY_ID)).thenReturn(Optional.empty());
        when(keyService.findSecretKey(TEST_KEY_ID)).thenReturn(Optional.of(testSecretKey));

        String cipher = cipherService.encrypt(TEST_TEXT, TEST_KEY_ID);
        String decrypted = cipherService.decrypt(cipher);

        assertThat(decrypted).isEqualTo(TEST_TEXT);
    }

    @Test
    void roundTrip_emptyText_shouldWorkCorrectly() {
        when(keyService.findSyncKey(TEST_KEY_ID)).thenReturn(Optional.of(testSyncKey));

        String cipher = cipherService.encrypt("", TEST_KEY_ID);
        String decrypted = cipherService.decrypt(cipher);

        assertThat(decrypted).isEqualTo("");
    }

    @Test
    void roundTrip_textWithSpecialCharacters_shouldWorkCorrectly() {
        when(keyService.findSyncKey(TEST_KEY_ID)).thenReturn(Optional.of(testSyncKey));

        String specialText = "Text with special chars: äöü 日本語 emoji 🎉 \n\t";
        String cipher = cipherService.encrypt(specialText, TEST_KEY_ID);
        String decrypted = cipherService.decrypt(cipher);

        assertThat(decrypted).isEqualTo(specialText);
    }

    @Test
    void roundTrip_longText_shouldWorkCorrectly() {
        when(keyService.findSyncKey(TEST_KEY_ID)).thenReturn(Optional.of(testSyncKey));

        String longText = "a".repeat(10000);
        String cipher = cipherService.encrypt(longText, TEST_KEY_ID);
        String decrypted = cipherService.decrypt(cipher);

        assertThat(decrypted).isEqualTo(longText);
    }

    @Test
    void roundTrip_multipleCiphersOfSameText_shouldAllDecryptCorrectly() {
        when(keyService.findSyncKey(TEST_KEY_ID)).thenReturn(Optional.of(testSyncKey));

        String cipher1 = cipherService.encrypt(TEST_TEXT, TEST_KEY_ID);
        String cipher2 = cipherService.encrypt(TEST_TEXT, TEST_KEY_ID);
        String cipher3 = cipherService.encrypt(TEST_TEXT, TEST_KEY_ID);

        assertThat(cipherService.decrypt(cipher1)).isEqualTo(TEST_TEXT);
        assertThat(cipherService.decrypt(cipher2)).isEqualTo(TEST_TEXT);
        assertThat(cipherService.decrypt(cipher3)).isEqualTo(TEST_TEXT);
    }

    // ================================================================
    // Cipher format tests
    // ================================================================

    @Test
    void cipherFormat_shouldContainKeyId() {
        when(keyService.findSyncKey(TEST_KEY_ID)).thenReturn(Optional.of(testSyncKey));

        String cipher = cipherService.encrypt(TEST_TEXT, TEST_KEY_ID);
        String[] parts = cipher.split(":");

        // Decode the Base64-encoded keyId and compare
        String decodedKeyId = new String(java.util.Base64.getDecoder().decode(parts[0]), java.nio.charset.StandardCharsets.UTF_8);
        assertThat(decodedKeyId).isEqualTo(TEST_KEY_ID);
    }

    @Test
    void cipherFormat_shouldContainAlgorithm() {
        when(keyService.findSyncKey(TEST_KEY_ID)).thenReturn(Optional.of(testSyncKey));

        String cipher = cipherService.encrypt(TEST_TEXT, TEST_KEY_ID);
        String[] parts = cipher.split(":");

        assertThat(parts[1]).isEqualTo("AES/GCM/NoPadding");
    }

    @Test
    void cipherFormat_shouldContainBase64EncryptedData() {
        when(keyService.findSyncKey(TEST_KEY_ID)).thenReturn(Optional.of(testSyncKey));

        String cipher = cipherService.encrypt(TEST_TEXT, TEST_KEY_ID);
        String[] parts = cipher.split(":");

        assertThat(parts).hasSize(4); // keyIdBase64:algorithm:encryptedDataBase64:ivBase64
        assertThat(parts[2]).isNotEmpty();
        // Verify it's valid base64 by trying to decode
        assertThat(java.util.Base64.getDecoder().decode(parts[2])).isNotEmpty();
    }

    @Test
    void cipherFormat_shouldContainBase64IV() {
        when(keyService.findSyncKey(TEST_KEY_ID)).thenReturn(Optional.of(testSyncKey));

        String cipher = cipherService.encrypt(TEST_TEXT, TEST_KEY_ID);
        String[] parts = cipher.split(":");

        assertThat(parts).hasSize(4);
        // Split remaining part to get encryptedData and IV
        int lastColon = cipher.lastIndexOf(':');
        String ivBase64 = cipher.substring(lastColon + 1);
        
        assertThat(ivBase64).isNotEmpty();
        // Verify it's valid base64 by trying to decode
        byte[] iv = java.util.Base64.getDecoder().decode(ivBase64);
        assertThat(iv).isNotEmpty();
        assertThat(iv).hasSize(12); // GCM IV should be 12 bytes
    }

    @Test
    void cipherFormat_ivShouldBeDifferentForEachEncryption() {
        when(keyService.findSyncKey(TEST_KEY_ID)).thenReturn(Optional.of(testSyncKey));

        String cipher1 = cipherService.encrypt(TEST_TEXT, TEST_KEY_ID);
        String cipher2 = cipherService.encrypt(TEST_TEXT, TEST_KEY_ID);

        int lastColon1 = cipher1.lastIndexOf(':');
        String ivBase64_1 = cipher1.substring(lastColon1 + 1);
        
        int lastColon2 = cipher2.lastIndexOf(':');
        String ivBase64_2 = cipher2.substring(lastColon2 + 1);

        assertThat(ivBase64_1).isNotEqualTo(ivBase64_2);
    }

    // ================================================================
    // Cross-validation tests
    // ================================================================

    @Test
    void decrypt_cipherCreatedBySyncKey_canBeDecryptedBySecretKeyIfSame() throws Exception {
        // Use the same key for both providers
        when(keyService.findSyncKey(TEST_KEY_ID)).thenReturn(Optional.of(testSyncKey));
        when(keyService.findSecretKey(TEST_KEY_ID)).thenReturn(Optional.of(testSyncKey));

        String cipher = cipherService.encrypt(TEST_TEXT, TEST_KEY_ID);

        // Remove sync key, leave only secret key
        when(keyService.findSyncKey(TEST_KEY_ID)).thenReturn(Optional.empty());
        when(keyService.findSecretKey(TEST_KEY_ID)).thenReturn(Optional.of(testSyncKey));

        String decrypted = cipherService.decrypt(cipher);

        assertThat(decrypted).isEqualTo(TEST_TEXT);
    }

    // ================================================================
    // GCM authentication tests
    // ================================================================

    @Test
    void gcmAuthentication_tamperedCiphertext_shouldFailDecryption() {
        when(keyService.findSyncKey(TEST_KEY_ID)).thenReturn(Optional.of(testSyncKey));

        String cipher = cipherService.encrypt(TEST_TEXT, TEST_KEY_ID);
        String[] parts = cipher.split(":");
        
        // Decode, modify, and re-encode the encrypted data
        byte[] encryptedData = java.util.Base64.getDecoder().decode(parts[2]);
        encryptedData[0] ^= 1; // Flip one bit
        String tamperedEncryptedData = java.util.Base64.getEncoder().encodeToString(encryptedData);
        
        // Reconstruct the cipher with tampered data (keeping IV at the end)
        int lastColon = cipher.lastIndexOf(':');
        String baseWithoutIV = cipher.substring(0, lastColon);
        String iv = cipher.substring(lastColon + 1);
        
        // Find the second-to-last colon (before encrypted data)
        int secondLastColon = baseWithoutIV.lastIndexOf(':');
        String prefix = baseWithoutIV.substring(0, secondLastColon + 1);
        String tamperedCipher = prefix + tamperedEncryptedData + ":" + iv;

        // GCM should detect the tampering and throw an exception
        assertThatThrownBy(() -> cipherService.decrypt(tamperedCipher))
                .isInstanceOf(CipherService.CipherException.class);
    }
}
