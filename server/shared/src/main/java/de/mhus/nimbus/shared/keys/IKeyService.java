package de.mhus.nimbus.shared.keys;

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

    Optional<PublicKey> findPublicKey(String keyId);

    Optional<PublicKey> findPublicKey(@NonNull KeyId id);

    Optional<SecretKey> findSecretKey(String keyId);

    Optional<SecretKey> findSecretKey(@NonNull KeyId id);

    Optional<SecretKey> findSyncKey(String keyId);

    Optional<SecretKey> findSyncKey(@NonNull KeyId id);

    Optional<KeyId> parseKeyId(String keyId);
}
