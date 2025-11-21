package de.mhus.nimbus.shared.security;

import lombok.NonNull;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.security.PublicKey;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Spring-managed service that aggregates registered key providers and resolves keys on demand.
 *
 * <p>Key ids are expressed as a string using the format "owner:uuid". Both parts are trimmed.
 * If the input cannot be parsed, an empty Optional will be returned by the finder methods.
 */
@Service
public class KeyService implements IKeyService {

    // Inject provider lists lazily; there may be multiple providers of each type
    private final ObjectProvider<List<PublicKeyProvider>> publicKeyProviders;
    private final ObjectProvider<List<SecretKeyProvider>> secretKeyProviders;
    private final ObjectProvider<List<SyncKeyProvider>> syncKeyProviders;

    public KeyService(@NonNull ObjectProvider<List<PublicKeyProvider>> publicKeyProviders,
                      @NonNull ObjectProvider<List<SecretKeyProvider>> secretKeyProviders,
                      @NonNull ObjectProvider<List<SyncKeyProvider>> syncKeyProviders) {
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

    public Optional<SecretKey> findSecretKey(KeyType type, String keyId) {
        return parseKeyId(keyId).flatMap(id -> findSecretKey(type, id));
    }

    public Optional<SecretKey> findSecretKey(KeyType type, @NonNull KeyId id) {
        for (SecretKeyProvider provider : secretKeyProviders.getIfAvailable(Collections::emptyList)) {
            Optional<SecretKey> key = provider.loadSecretKey(type, id);
            if (key.isPresent()) return key;
        }
        return Optional.empty();
    }

    // ------------------------------------------------------------
    // Sync keys (synchronous/symmetric)

    public Optional<SecretKey> findSyncKey(KeyType type, String keyId) {
        return parseKeyId(keyId).flatMap(id -> findSyncKey(type, id));
    }

    public Optional<SecretKey> findSyncKey(KeyType type, @NonNull KeyId id) {
        for (SyncKeyProvider provider : syncKeyProviders.getIfAvailable(Collections::emptyList)) {
            Optional<SecretKey> key = provider.loadSyncKey(type, id);
            if (key.isPresent()) return key;
        }
        return Optional.empty();
    }

    // ------------------------------------------------------------

    /**
     * Parses a string in the form "owner:uuid" into a KeyId.
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
}
