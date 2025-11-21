package de.mhus.nimbus.shared.security;

import java.security.PublicKey;
import java.util.Optional;

/**
 * Provider interface to load asymmetric public keys by their {@link KeyId}.
 *
 * <p>Intended usage: register one or more Spring beans implementing this
 * interface. Inject them as a list (e.g. {@code List<PublicKeyProvider>}) and
 * iterate until one returns a value.
 *
 * <p>No Spring dependency here to keep the shared module lightweight.
 */
public interface PublicKeyProvider {

    /**
     * Loads a public key for the given id if available.
     *
     * @param id the key id (owner + uuid), never null
     * @return optional containing the key if this provider can resolve it; empty if not found
     */
    Optional<PublicKey> loadPublicKey(KeyType type, KeyId id);

    /**
     * Convenience overload to avoid manual KeyId construction.
     */
    default Optional<PublicKey> loadPublicKey(KeyType type, String owner, String uuid) {
        return loadPublicKey(type, KeyId.of(owner, uuid));
        
    }
}
