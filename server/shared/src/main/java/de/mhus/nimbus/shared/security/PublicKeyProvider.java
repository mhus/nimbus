package de.mhus.nimbus.shared.security;

import java.security.PublicKey;
import java.util.List;
import java.util.Optional;

public interface PublicKeyProvider {
    Optional<PublicKey> getPublicKey(KeyType type, KeyId id);
    List<PublicKey> getPublicKeysForOwner(KeyType type, String owner);
}

