package de.mhus.nimbus.shared.dto.universe;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request-DTO zum Erstellen eines Schlüssels.
 * Hinweis: Keine OpenAPI-Annotationen hier, damit das shared-Modul leicht bleibt.
 */
public class CreateSKeyRequest {

    @NotBlank
    @Size(max = 64)
    public String type;

    @NotBlank
    @Size(max = 16)
    public String kind; // "public" | "private" | "symmetric"

    @NotBlank
    @Size(max = 64)
    public String algorithm;

    @NotBlank
    @Size(max = 128)
    public String name; // UUID / frei definierter Name

    @NotBlank
    public String key; // Base64-kodiertes Key-Material (wird nie zurückgegeben)
}
