package de.mhus.nimbus.shared.security;

import javax.crypto.SecretKey;
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
public interface SecretKeyProvider {

    /**
     * Loads a secret key for the given id if available.
     *
     * @param id the key id (owner + uuid), never null
     * @return optional containing the key if this provider can resolve it; empty if not found
     */
    Optional<SecretKey> loadSecretKey(KeyId id);

    /**
     * Convenience overload to avoid manual KeyId construction.
     */
    default Optional<SecretKey> loadSecretKey(String owner, String uuid) {
        return loadSecretKey(KeyId.of(owner, uuid));
    }
}
