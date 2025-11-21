package de.mhus.nimbus.shared.security;

import java.util.Objects;

/**
 * Identifier for a cryptographic key.
 * <p>
 * A key id is composed of an owner (e.g. tenant, system, application) and a UUID
 * to uniquely identify a particular key version. The concrete format of both
 * fields is up to the implementation, but both must be non-null and non-blank.
 */
public record KeyId(String owner, String id) {

    public KeyId {
        if (owner == null || owner.isBlank()) {
            throw new IllegalArgumentException("owner must not be null or blank");
        }
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("id must not be null or blank");
        }
    }

    /**
     * Creates a KeyId ensuring both components are trimmed.
     */
    public static KeyId of(String owner, String uuid) {
        Objects.requireNonNull(owner, "owner");
        Objects.requireNonNull(uuid, "id");
        return new KeyId(owner.trim(), uuid.trim());
    }
}
