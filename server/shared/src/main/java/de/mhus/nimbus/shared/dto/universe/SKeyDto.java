package de.mhus.nimbus.shared.dto.universe;

import java.time.Instant;

/**
 * Metadaten eines Schlüssels (ohne Key-Material).
 * Hinweis: Keine OpenAPI-Annotationen hier, damit das shared-Modul leichtgewichtig bleibt.
 */
public class SKeyDto {

    public String id;
    public String type;
    public String kind;
    public String algorithm;
    public String keyId;
    public Instant createdAt;
    public String owner;
    public String intent;
}
