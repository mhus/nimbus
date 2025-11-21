package de.mhus.nimbus.shared.security;

import javax.crypto.SecretKey;
import java.security.PrivateKey;
import java.util.Optional;

/**
 * Provider interface to load symmetric secret keys by their {@link KeyId}.
 *
 * <p>Intended usage: register one or more Spring beans implementing this
 * interface. Inject them as a list (e.g. {@code List<SecretKeyProvider>}) and
 * iterate until one returns a value.
 *
 * <p>No Spring dependency here to keep the shared module lightweight.
 */
public interface PrivateKeyProvider {

    /**
     * Loads a secret key for the given id if available.
     *
     * @param id the key id (owner + id), never null
     * @return optional containing the key if this provider can resolve it; empty if not found
     */
    Optional<PrivateKey> loadPrivateKey(KeyType type, KeyId id);

    /**
     * Convenience overload to avoid manual KeyId construction.
     */
    default Optional<PrivateKey> loadPrivateKey(KeyType type, String owner, String uuid) {
        return loadPrivateKey(type, KeyId.of(owner, uuid));
    }
}
