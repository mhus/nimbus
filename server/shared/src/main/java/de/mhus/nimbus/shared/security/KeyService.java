package de.mhus.nimbus.shared.security;

import lombok.NonNull;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.ECGenParameterSpec;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Spring-managed service that aggregates registered key providers and resolves keys on demand.
 *
 * <p>Key ids are expressed as a string using the format "owner:id". Both parts are trimmed.
 * If the input cannot be parsed, an empty Optional will be returned by the finder methods.
 */
@Service
public class KeyService {

    // Inject provider lists lazily; there may be multiple providers of each type
    private final ObjectProvider<List<PublicKeyProvider>> publicKeyProviders;
    private final ObjectProvider<List<PrivateKeyProvider>> secretKeyProviders;
    private final ObjectProvider<List<SymmetricKeyProvider>> syncKeyProviders;

    public KeyService(@NonNull ObjectProvider<List<PublicKeyProvider>> publicKeyProviders,
                      @NonNull ObjectProvider<List<PrivateKeyProvider>> secretKeyProviders,
                      @NonNull ObjectProvider<List<SymmetricKeyProvider>> syncKeyProviders) {
        this.publicKeyProviders = publicKeyProviders;
        this.secretKeyProviders = secretKeyProviders;
        this.syncKeyProviders = syncKeyProviders;
    }

    // ------------------------------------------------------------
    // Public keys

    public Optional<PublicKey> findPublicKey(KeyType type, String keyId) {
        return parseKeyId(keyId).flatMap(id -> findPublicKey(type, id));
    }

    public Optional<PublicKey> findPublicKey(KeyType type, @NonNull KeyId id) {
        for (PublicKeyProvider provider : publicKeyProviders.getIfAvailable(Collections::emptyList)) {
            Optional<PublicKey> key = provider.loadPublicKey(type, id);
            if (key.isPresent()) return key;
        }
        return Optional.empty();
    }

    // ------------------------------------------------------------
    // Secret keys

    public Optional<PrivateKey> findPrivateKey(KeyType type, String keyId) {
        return parseKeyId(keyId).flatMap(id -> findPrivateKey(type, id));
    }

    public Optional<PrivateKey> findPrivateKey(KeyType type, @NonNull KeyId id) {
        for (PrivateKeyProvider provider : secretKeyProviders.getIfAvailable(Collections::emptyList)) {
            Optional<PrivateKey> key = provider.loadPrivateKey(type, id);
            if (key.isPresent()) return key;
        }
        return Optional.empty();
    }

    // ------------------------------------------------------------
    // Sync keys (synchronous/symmetric)

    public Optional<SecretKey> findSymetricKey(KeyType type, String keyId) {
        return parseKeyId(keyId).flatMap(id -> findSymetricKey(type, id));
    }

    public Optional<SecretKey> findSymetricKey(KeyType type, @NonNull KeyId id) {
        for (SymmetricKeyProvider provider : syncKeyProviders.getIfAvailable(Collections::emptyList)) {
            Optional<SecretKey> key = provider.loadSymmetricKey(type, id);
            if (key.isPresent()) return key;
        }
        return Optional.empty();
    }

    // ------------------------------------------------------------

    /**
     * Parses a string in the form "owner:id" into a KeyId.
     * Returns Optional.empty() if the input is null, blank, or malformed.
     */
    public Optional<KeyId> parseKeyId(String keyId) {
        if (keyId == null) return Optional.empty();
        String trimmed = keyId.trim();
        if (trimmed.isEmpty()) return Optional.empty();
        int sep = trimmed.indexOf(':');
        if (sep <= 0 || sep >= trimmed.length() - 1) {
            return Optional.empty();
        }
        String owner = trimmed.substring(0, sep).trim();
        String uuid = trimmed.substring(sep + 1).trim();
        if (owner.isEmpty() || uuid.isEmpty()) return Optional.empty();
        try {
            return Optional.of(KeyId.of(owner, uuid));
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
    }

    public SecretKey createAESKey() throws NoSuchAlgorithmException {
        // Generate test keys for AES
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(256);
        return keyGen.generateKey();
    }

    public KeyPair createECCKeys() throws InvalidAlgorithmParameterException, NoSuchAlgorithmException {
        var keyGen = KeyPairGenerator.getInstance("EC");
        ECGenParameterSpec ecSpec = new ECGenParameterSpec("secp256r1");
        keyGen.initialize(ecSpec);
        return keyGen.generateKeyPair();
    }

}
