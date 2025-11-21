package de.mhus.nimbus.shared.security;

import javax.crypto.SecretKey;
import java.util.Optional;

/**
 * Provider interface for synchronous (symmetric) encryption keys.
 *
 * <p>This provider exposes access to a symmetric key identified by {@link KeyId}.
 * Implementations can look up keys from any backing store. Register one or more
 * Spring beans implementing this interface in your application modules; they will
 * be discovered lazily by {@code SecurityService} and queried in order until one
 * returns a value.</p>
 *
 * <p>No Spring dependency here to keep the shared module lightweight.</p>
 */
public interface SymmetricKeyProvider {

    /**
     * Loads a symmetric key for the given id if available.
     *
     * @param id the key id (owner + uuid), never null
     * @return optional containing the key if this provider can resolve it; empty if not found
     */
    Optional<SecretKey> loadSymmetricKey(KeyType type, KeyId id);

    /**
     * Convenience overload to avoid manual KeyId construction.
     */
    default Optional<SecretKey> loadSymmetricKey(KeyType type, String owner, String uuid) {
        return loadSymmetricKey(type, KeyId.of(owner, uuid));
    }
}
