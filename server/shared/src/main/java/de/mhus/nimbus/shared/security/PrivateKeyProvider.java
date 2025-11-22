package de.mhus.nimbus.shared.security;

import java.security.PrivateKey;
import java.util.List;
import java.util.Optional;

public interface PrivateKeyProvider {
    Optional<PrivateKey> getPrivateKey(KeyType type, KeyId id);
    List<PrivateKey> getPrivateKeysForOwner(KeyType type, String owner);
}

