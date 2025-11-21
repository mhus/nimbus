package de.mhus.nimbus.universe.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(name = "UpdateSKeyNameRequest", description = "Anfragedaten zum Ändern des Schlüsselnamens")
public class UpdateSKeyNameRequest {

    @NotBlank
    @Size(max = 128)
    @Schema(description = "Neuer Name/UUID des Schlüssels", example = "3d5b0c8d-aaaa-bbbb-cccc-ddddeeeeffff", requiredMode = Schema.RequiredMode.REQUIRED)
    public String name;
}
