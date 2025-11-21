package de.mhus.nimbus.shared.dto.universe;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request-DTO zum Aktualisieren des Schlüsselnamens.
 */
public class UpdateSKeyNameRequest {

    @NotBlank
    @Size(max = 128)
    public String name;
}
