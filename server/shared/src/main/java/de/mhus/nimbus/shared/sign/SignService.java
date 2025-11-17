package de.mhus.nimbus.shared.sign;

import de.mhus.nimbus.shared.keys.IKeyService;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.Signature;
import java.util.Base64;
import java.util.Optional;

/**
 * Service for signing and validating text using cryptographic keys from KeyService.
 *
 * <p>The signature format is: "keyIdBase64:algorithm:signatureBase64"
 * where keyIdBase64 is the Base64-encoded keyId (to avoid delimiter conflicts with "owner:uuid" format),
 * algorithm is the cryptographic algorithm used,
 * and signatureBase64 is the Base64-encoded signature bytes.
 *
 * <p>Signing uses a constant default algorithm (HmacSHA256 for symmetric keys).
 * Validation extracts the algorithm from the signature string.
 */
@Service
@Slf4j
public class SignService {

    private final IKeyService keyService;

    public SignService(@NonNull IKeyService keyService) {
        this.keyService = keyService;
    }

    /**
     * Default algorithm used for signing with symmetric keys (HMAC).
     */
    private static final String DEFAULT_HMAC_ALGORITHM = "HmacSHA256";

    /**
     * Signs the given text using a symmetric key identified by keyId.
     * The signature format is "keyIdBase64:algorithm:signatureBase64".
     *
     * @param text the text to sign, never null
     * @param keyId the key identifier in format "owner:uuid", never null
     * @return the complete signature string containing Base64-encoded keyId, algorithm, and signature
     * @throws SignatureException if the key cannot be found or signing fails
     */
    public String sign(@NonNull String text, @NonNull String keyId) {
        // Try to find a sync key first (preferred for signing)
        Optional<SecretKey> syncKey = keyService.findSyncKey(keyId);
        if (syncKey.isPresent()) {
            return signWithHmac(text, keyId, syncKey.get(), DEFAULT_HMAC_ALGORITHM);
        }

        // Fallback to secret key
        Optional<SecretKey> secretKey = keyService.findSecretKey(keyId);
        if (secretKey.isPresent()) {
            return signWithHmac(text, keyId, secretKey.get(), DEFAULT_HMAC_ALGORITHM);
        }

        throw new SignatureException("No symmetric key found for keyId: " + keyId);
    }

    /**
     * Validates the given text against a signature string.
     * The signature string must be in the format "keyIdBase64:algorithm:signatureBase64".
     *
     * @param text the original text to validate, never null
     * @param signatureString the complete signature string, never null
     * @return true if the signature is valid, false otherwise
     */
    public boolean validate(@NonNull String text, @NonNull String signatureString) {
        try {
            // Parse the signature string
            SignatureParts parts = parseSignature(signatureString);

            // Retrieve the key based on the extracted keyId
            Optional<SecretKey> syncKey = keyService.findSyncKey(parts.keyId());
            if (syncKey.isPresent()) {
                return validateWithHmac(text, parts, syncKey.get());
            }

            Optional<SecretKey> secretKey = keyService.findSecretKey(parts.keyId());
            if (secretKey.isPresent()) {
                return validateWithHmac(text, parts, secretKey.get());
            }

            log.warn("No key found for keyId: {}", parts.keyId());
            return false;

        } catch (Exception e) {
            log.error("Validation failed: {}", e.getMessage(), e);
            return false;
        }
    }

    // ----------------------------------------------------------------
    // Private helper methods

    private String signWithHmac(String text, String keyId, SecretKey key, String algorithm) {
        try {
            Mac mac = Mac.getInstance(algorithm);
            mac.init(key);
            byte[] signatureBytes = mac.doFinal(text.getBytes(StandardCharsets.UTF_8));
            String signatureBase64 = Base64.getEncoder().encodeToString(signatureBytes);
            String keyIdBase64 = Base64.getEncoder().encodeToString(keyId.getBytes(StandardCharsets.UTF_8));
            return keyIdBase64 + ":" + algorithm + ":" + signatureBase64;
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new SignatureException("Failed to sign text with HMAC: " + e.getMessage(), e);
        }
    }

    private boolean validateWithHmac(String text, SignatureParts parts, SecretKey key) {
        try {
            Mac mac = Mac.getInstance(parts.algorithm());
            mac.init(key);
            byte[] computedSignature = mac.doFinal(text.getBytes(StandardCharsets.UTF_8));
            byte[] providedSignature = Base64.getDecoder().decode(parts.signatureBase64());
            return java.security.MessageDigest.isEqual(computedSignature, providedSignature);
        } catch (Exception e) {
            log.error("HMAC validation error: {}", e.getMessage(), e);
            return false;
        }
    }

    private SignatureParts parseSignature(String signatureString) {
        String[] parts = signatureString.split(":", 3);
        if (parts.length != 3) {
            throw new SignatureException("Invalid signature format. Expected 'keyIdBase64:algorithm:signatureBase64'");
        }
        // Decode the Base64-encoded keyId
        String keyIdBase64 = parts[0];
        String keyId = new String(Base64.getDecoder().decode(keyIdBase64), StandardCharsets.UTF_8);
        return new SignatureParts(keyId, parts[1], parts[2]);
    }

    // ----------------------------------------------------------------
    // Internal record to hold parsed signature components

    private record SignatureParts(String keyId, String algorithm, String signatureBase64) {}

    // ----------------------------------------------------------------
    // Custom exception

    public static class SignatureException extends RuntimeException {
        public SignatureException(String message) {
            super(message);
        }

        public SignatureException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
