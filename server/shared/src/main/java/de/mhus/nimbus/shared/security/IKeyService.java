package de.mhus.nimbus.shared.security;

import lombok.NonNull;

import javax.crypto.SecretKey;
import java.security.PublicKey;
import java.util.Optional;

/**
 * Interface for key resolution services.
 * 
 * <p>Key ids are expressed as a string using the format "owner:uuid". Both parts are trimmed.
 * If the input cannot be parsed, an empty Optional will be returned by the finder methods.
 */
public interface IKeyService {

    Optional<PublicKey> findPublicKey(KeyType type, String keyId);

    Optional<PublicKey> findPublicKey(KeyType type, @NonNull KeyId id);

    Optional<SecretKey> findSecretKey(KeyType type, String keyId);

    Optional<SecretKey> findSecretKey(KeyType type, @NonNull KeyId id);

    Optional<SecretKey> findSyncKey(KeyType type, String keyId);

    Optional<SecretKey> findSyncKey(KeyType type, @NonNull KeyId id);

    Optional<KeyId> parseKeyId(String keyId);
}
