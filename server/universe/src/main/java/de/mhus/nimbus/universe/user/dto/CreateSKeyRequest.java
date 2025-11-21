package de.mhus.nimbus.universe.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(name = "CreateSKeyRequest", description = "Anfragedaten zum Erstellen eines Schlüssels")
public class CreateSKeyRequest {

    @NotBlank
    @Size(max = 64)
    @Schema(description = "Kontexttyp (z. B. UNIVERSE)", example = "UNIVERSE", requiredMode = Schema.RequiredMode.REQUIRED)
    public String type;

    @NotBlank
    @Size(max = 16)
    @Schema(description = "Art des Schlüssels", example = "public", allowableValues = {"public","private","symmetric"}, requiredMode = Schema.RequiredMode.REQUIRED)
    public String kind;

    @NotBlank
    @Size(max = 64)
    @Schema(description = "Algorithmus", example = "RSA", requiredMode = Schema.RequiredMode.REQUIRED)
    public String algorithm;

    @NotBlank
    @Size(max = 128)
    @Schema(description = "Name/UUID des Schlüssels", example = "b2c7e2f0-1234-5678-9abc-ffeeddccbbaa", requiredMode = Schema.RequiredMode.REQUIRED)
    public String name;

    @NotBlank
    @Schema(description = "Key-Material Base64-kodiert (wird nie zurückgegeben)", requiredMode = Schema.RequiredMode.REQUIRED)
    public String key;
}
